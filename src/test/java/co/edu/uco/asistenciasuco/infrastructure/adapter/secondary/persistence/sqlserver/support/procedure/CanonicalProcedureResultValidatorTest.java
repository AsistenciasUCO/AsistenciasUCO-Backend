package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.TechnicalException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * LB-002.2C — CMD-ADP-008/009/010: el validador JPA reproduce la semantica del executor JDBC
 * (cardinalidad, correlacion y traduccion por DBCODE) delegando la clasificacion en
 * {@code DbExceptionTranslator}. La equivalencia con el executor se comprueba contra el executor REAL.
 */
class CanonicalProcedureResultValidatorTest {

    private static final String OPERATION = "registrarAsistenciasSesion";
    private static final UUID CORRELATION = UUID.randomUUID();
    private static final String CONTRACT_MESSAGE = "El procedimiento no retorno el contrato canonico esperado.";
    private static final String CORRELATION_MESSAGE = "La correlacion retornada por la DB no coincide con la peticion.";

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    private static CanonicalProcedureResult ok(final UUID correlation) {
        return new CanonicalProcedureResult(correlation, "ok", "detalle", true);
    }

    private static CanonicalProcedureResult failed(final String technicalMessage) {
        return new CanonicalProcedureResult(CORRELATION, "mensaje usuario", technicalMessage, false);
    }

    @Test
    void resultado_unico_exitoso_con_correlacion_coincidente_se_retorna_tal_cual() {
        final CanonicalProcedureResult result = ok(CORRELATION);

        assertSame(result, CanonicalProcedureResultValidator.validate(List.of(result), CORRELATION, OPERATION));
    }

    @Test
    void cero_filas_es_violacion_del_contrato_canonico() {
        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> CanonicalProcedureResultValidator.validate(List.of(), CORRELATION, OPERATION));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
        assertEquals(CONTRACT_MESSAGE, exception.getMessage());
    }

    @Test
    void dos_filas_es_violacion_del_contrato_canonico_y_no_toma_la_primera() {
        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> CanonicalProcedureResultValidator.validate(
                        List.of(ok(CORRELATION), ok(CORRELATION)), CORRELATION, OPERATION));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
        assertEquals(CONTRACT_MESSAGE, exception.getMessage());
    }

    @Test
    void correlacion_devuelta_distinta_es_violacion_del_contrato_canonico() {
        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> CanonicalProcedureResultValidator.validate(
                        List.of(ok(UUID.randomUUID())), CORRELATION, OPERATION));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
        assertEquals(CORRELATION_MESSAGE, exception.getMessage());
    }

    @Test
    void correlacion_nula_devuelta_no_coincide() {
        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> CanonicalProcedureResultValidator.validate(List.of(ok(null)), CORRELATION, OPERATION));

        assertEquals(CORRELATION_MESSAGE, exception.getMessage());
    }

    @Test
    void la_cardinalidad_se_valida_antes_que_la_correlacion_y_esta_antes_que_el_estado() {
        final DatabaseOperationException cardinalidad = assertThrows(DatabaseOperationException.class,
                () -> CanonicalProcedureResultValidator.validate(List.of(), CORRELATION, OPERATION));
        final DatabaseOperationException correlacion = assertThrows(DatabaseOperationException.class,
                () -> CanonicalProcedureResultValidator.validate(
                        List.of(new CanonicalProcedureResult(UUID.randomUUID(), "u", "DBCODE=SEC_001|x", false)),
                        CORRELATION, OPERATION));

        assertEquals(CONTRACT_MESSAGE, cardinalidad.getMessage());
        assertEquals(CORRELATION_MESSAGE, correlacion.getMessage());
    }

    /**
     * CMD-ADP-010: para cada DBCODE (y marcadores malformados/legacy) la excepcion del validador es de la
     * misma clase y el mismo codigo que la del executor JDBC real. Una unica autoridad de clasificacion.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "DBCODE=SEC_001|d", "DBCODE=SEC_002|d", "DBCODE=EST_004|d", "DBCODE=ATT_001|d", "DBCODE=ATT_002|d",
            "DBCODE=ATT_003|d", "DBCODE=GEN_002|d", "DBCODE=RC_001|d", "DBCODE=SES_004|d", "DBCODE=SES_001|d",
            "DBCODE=SES_003|d", "DBCODE=XYZ_999|d", "DBCODE=sec-001|malformado", "texto legacy sin marcador"
    })
    void la_traduccion_de_fallos_es_identica_a_la_del_executor_jdbc(final String technicalMessage) {
        CorrelationIdContext.set(CORRELATION);
        final CanonicalProcedureResult failure = failed(technicalMessage);

        final RuntimeException viaValidator = assertThrows(RuntimeException.class,
                () -> CanonicalProcedureResultValidator.validate(List.of(failure), CORRELATION, OPERATION));
        final RuntimeException viaExecutor = assertThrows(RuntimeException.class,
                () -> jdbcExecutorReturning(failure).execute(OPERATION, "EXEC sp", new MapSqlParameterSource()));

        assertEquals(viaExecutor.getClass(), viaValidator.getClass());
        assertEquals(codeOf(viaExecutor), codeOf(viaValidator));
        assertEquals(viaExecutor.getMessage(), viaValidator.getMessage());
    }

    @SuppressWarnings("unchecked")
    private static CanonicalStoredProcedureExecutor jdbcExecutorReturning(final CanonicalProcedureResult result) {
        final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(result));
        return new CanonicalStoredProcedureExecutor(jdbc);
    }

    private static String codeOf(final RuntimeException exception) {
        if (exception instanceof TechnicalException technical) {
            return technical.getCode();
        }
        if (exception instanceof ApplicationException application) {
            return application.getCode();
        }
        return exception.getClass().getName();
    }
}
