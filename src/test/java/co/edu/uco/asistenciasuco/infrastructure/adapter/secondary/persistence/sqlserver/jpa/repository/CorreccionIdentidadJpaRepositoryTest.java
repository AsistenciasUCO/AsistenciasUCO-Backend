package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.CorreccionIdentidadCommandPort.NombresCorregidos;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.CorreccionIdentidadCommandPort.ResultadoCorreccionIdentidad;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.CorreccionIdentidadCommandPort.TipoUsuarioCorregible;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU159: invocacion de usp_corregir_identidad_usuario y lectura de sus columnas extra. */
class CorreccionIdentidadJpaRepositoryTest {

    private static final UUID REGISTRO = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID TIPO = UUID.randomUUID();

    private final JpaProcedureExecutor executor = mock(JpaProcedureExecutor.class);
    private final CorreccionIdentidadJpaRepository repository = new CorreccionIdentidadJpaRepository(executor);

    @BeforeEach
    void correlacion() {
        CorrelationIdContext.set(UUID.randomUUID());
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void constructor_rechaza_executor_nulo() {
        assertThrows(NullPointerException.class, () -> new CorreccionIdentidadJpaRepository(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void T_159_REPO_01_envia_tipo_registro_nombres_documento_y_lee_usuario_correo_y_documento() {
        when(executor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(3)))
                .thenReturn(new ProcedureRow(mock(ProcedureResult.class), Arrays.asList(USUARIO.toString(), "ana@uco.edu.co", 915900011)));

        final ResultadoCorreccionIdentidad resultado = repository.corregir(TipoUsuarioCorregible.ESTUDIANTE, REGISTRO,
                new NombresCorregidos("Ana", null, "Gomez", null), new CorreccionDocumentoDTO(TIPO, 915900011, "Digitacion"), USUARIO);

        assertEquals(USUARIO, resultado.idUsuario());
        assertEquals("ana@uco.edu.co", resultado.correo());
        assertEquals(915900011, resultado.numeroIdentificacion());
        final ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);
        verify(executor).executeWithExtras(eq(CorreccionIdentidadJpaRepository.OP_CORREGIR_IDENTIDAD),
                eq(CorreccionIdentidadJpaRepository.SQL_CORREGIR_IDENTIDAD), captor.capture(), any(), eq(3));
        assertEquals("ESTUDIANTE", captor.getValue().get("tipoUsuario"));
        assertEquals(TIPO, captor.getValue().get("idTipoIdIdentificacion"));
        assertEquals("Digitacion", captor.getValue().get("motivo"));
    }

    @Test
    void T_159_REPO_02_sin_documento_envia_nulos_y_tolera_correo_y_numero_nulos() {
        when(executor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(3)))
                .thenReturn(new ProcedureRow(mock(ProcedureResult.class), Arrays.asList(USUARIO, null, null)));

        final ResultadoCorreccionIdentidad resultado = repository.corregir(TipoUsuarioCorregible.DECANO, REGISTRO,
                new NombresCorregidos("Luis", null, "Ruiz", null), null, USUARIO);

        assertNull(resultado.correo());
        assertNull(resultado.numeroIdentificacion());
    }

    @Test
    void T_159_REPO_03_sin_usuario_en_la_respuesta_es_violacion_del_contrato() {
        when(executor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(3)))
                .thenReturn(new ProcedureRow(mock(ProcedureResult.class), Arrays.asList(null, null, null)));

        assertThrows(DatabaseOperationException.class, () -> repository.corregir(TipoUsuarioCorregible.DECANO, REGISTRO,
                new NombresCorregidos("Luis", null, "Ruiz", null), CorreccionDocumentoDTO.ninguna(), USUARIO));
    }
}
