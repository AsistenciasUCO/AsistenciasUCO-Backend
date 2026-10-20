package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolicitarRevisionAsistenciaDomainTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USUARIO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOPORTE = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void construye_domain_valido_y_normaliza_justificacion() {
        final SolicitarRevisionAsistenciaDomain domain =
                new SolicitarRevisionAsistenciaDomain(
                        SESION,
                        "  ERROR_REGISTRO  ",
                        "  Llegue despues del registro  ",
                        SOPORTE,
                        USUARIO
                );

        assertEquals(SESION, domain.getSesion());
        assertEquals("ERROR_REGISTRO", domain.getCategoria());
        assertEquals("Llegue despues del registro", domain.getJustificacion());
        assertEquals(USUARIO, domain.getUsuario());
        assertEquals(SOPORTE, domain.getSoporteArchivoId());
    }

    @Test
    void el_soporte_es_opcional() {
        final SolicitarRevisionAsistenciaDomain domain =
                new SolicitarRevisionAsistenciaDomain(SESION, "CAT", "motivo valido", null, USUARIO);

        assertEquals(null, domain.getSoporteArchivoId());
    }

    @Test
    void rechaza_sesion_usuario_y_textos_invalidos() {
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(null, "CAT", "motivo valido", null, USUARIO));
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(SESION, "CAT", "motivo valido", null, null));
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(SESION, null, "motivo valido", null, USUARIO));
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(SESION, "CAT", null, null, USUARIO));
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(SESION, "CAT", "a".repeat(301), null, USUARIO));
    }
}
