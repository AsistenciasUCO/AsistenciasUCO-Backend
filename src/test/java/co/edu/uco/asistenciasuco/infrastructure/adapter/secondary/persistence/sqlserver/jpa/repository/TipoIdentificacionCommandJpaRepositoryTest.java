package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionCommandPort.GuardarTipoIdentificacionCommand;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** HU153: EXEC canonico de usp_guardar_tipo_identificacion. */
class TipoIdentificacionCommandJpaRepositoryTest {

    private static final UUID CORRELATION_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ACTOR = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private final JpaProcedureExecutor executor = mock(JpaProcedureExecutor.class);
    private final TipoIdentificacionCommandJpaRepository repository = new TipoIdentificacionCommandJpaRepository(executor);

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void constructor_rechaza_ejecutor_nulo() {
        assertThrows(NullPointerException.class, () -> new TipoIdentificacionCommandJpaRepository(null));
    }

    @Test
    void T_153_PAT_01_alta_y_edicion_delegan_con_correlacion_y_ejecutor() {
        CorrelationIdContext.set(CORRELATION_ID);

        repository.guardarTipoIdentificacion(new GuardarTipoIdentificacionCommand(null, "PEP", "Permiso especial", ACTOR));
        repository.guardarTipoIdentificacion(new GuardarTipoIdentificacionCommand(ID, null, "Pasaporte", ACTOR));

        verify(executor).execute(eq(TipoIdentificacionCommandJpaRepository.OP_GUARDAR),
                eq(TipoIdentificacionCommandJpaRepository.SQL_GUARDAR),
                eq(parametros(null, "PEP", "Permiso especial")), eq(CORRELATION_ID));
        verify(executor).execute(eq(TipoIdentificacionCommandJpaRepository.OP_GUARDAR),
                eq(TipoIdentificacionCommandJpaRepository.SQL_GUARDAR),
                eq(parametros(ID, null, "Pasaporte")), eq(CORRELATION_ID));
    }

    private static Map<String, Object> parametros(final UUID id, final String codigo, final String nombre) {
        final Map<String, Object> parametros = new HashMap<>();
        parametros.put("idTipoIdentificacion", id);
        parametros.put("codigo", codigo);
        parametros.put("nombre", nombre);
        parametros.put("idCorrelacion", CORRELATION_ID);
        parametros.put("idUsuarioEjecutor", ACTOR);
        return parametros;
    }
}
