package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResolverSolicitudRevisionAsistenciaDomainTest {

    private static final UUID SOLICITUD = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    @Test
    void constructor_con_datos_validos_normaliza_campos() {
        final ResolverSolicitudRevisionAsistenciaDomain domain =
                new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "  aprobada  ", "  Aceptada  ", USUARIO);

        assertEquals(SOLICITUD, domain.getSolicitud());
        assertEquals("APROBADA", domain.getAccion());
        assertEquals("Aceptada", domain.getRespuestaDocente());
        assertEquals(USUARIO, domain.getUsuario());
    }

    @Test
    void constructor_con_respuesta_nula_permite_valor_nulo() {
        final ResolverSolicitudRevisionAsistenciaDomain domain =
                new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "APROBADA", null, USUARIO);

        assertNull(domain.getRespuestaDocente());
    }

    @Test
    void constructor_rechaza_solicitud_nula() {
        assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(null, "APROBADA", null, USUARIO));
    }

    @Test
    void constructor_rechaza_usuario_nulo() {
        assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "APROBADA", null, null));
    }

    @Test
    void constructor_rechaza_accion_en_blanco() {
        assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "   ", null, USUARIO));
    }

    // SES-04: solo las acciones contractuales; ningun otro valor se interpreta como rechazo.
    @ParameterizedTest
    @ValueSource(strings = {"APROBAR", "ACEPTAR", "RECHAZAR", "APROBADO", "SI", "1"})
    void constructor_rechaza_acciones_fuera_del_contrato(final String accion) {
        final ValidationException exception = assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, accion, null, USUARIO));

        assertEquals(AsistenciaErrorCode.ERR_ACCION_REVISION_INVALIDA.code(), exception.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"APROBADA", "RECHAZADA", " rechazada "})
    void constructor_acepta_y_normaliza_las_acciones_contractuales(final String accion) {
        final ResolverSolicitudRevisionAsistenciaDomain domain =
                new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, accion, null, USUARIO);

        assertEquals(accion.trim().toUpperCase(java.util.Locale.ROOT), domain.getAccion());
    }
}
