package co.edu.uco.asistenciasuco.application.features.admin.parametros;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcambiosparametro.primaryports.ConsultarCambiosParametroInputPort.Comando;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcambiosparametro.primaryports.ConsultarCambiosParametroInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcambiosparametro.usecase.impl.ConsultarCambiosParametroUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort.CambioParametro;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort.PaginaCambios;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** CULM-001 D.3 (CONTRACT v2 §4.3, parte diferible): historial paginado de un parametro; 100 % de ramas. */
class ConsultarCambiosParametroUseCaseImplTest {

    private static final UUID ADMIN = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID PARAMETRO = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private final ParametroAdminRepositoryPort repositorio = mock(ParametroAdminRepositoryPort.class);
    private final ConsultarCambiosParametroUseCaseImpl useCase = new ConsultarCambiosParametroUseCaseImpl(repositorio);

    @Test
    void usa_pagina_1_tamano_20_por_defecto_y_mapea_las_filas() {
        final UUID correlacion = UUID.randomUUID();
        when(repositorio.consultarCambios(ADMIN, PARAMETRO, 1, 20)).thenReturn(new PaginaCambios(List.of(new CambioParametro(
                UUID.randomUUID(), "15", "20", "Ajuste de prueba", ADMIN, "Ana Admin", correlacion,
                LocalDateTime.of(2026, 11, 7, 16, 0, 0, 123_000_000))), 1));

        final Resultado resultado = useCase.execute(new Comando(PARAMETRO, null, null, ADMIN));

        assertEquals(1, resultado.numeroPagina());
        assertEquals(20, resultado.tamanoPagina());
        assertEquals(1, resultado.totalElementos());
        assertEquals("Ana Admin", resultado.elementos().getFirst().nombreEjecutor());
        assertEquals(correlacion, resultado.elementos().getFirst().idTraza());
        assertEquals(Instant.parse("2026-11-07T16:00:00.123Z"), resultado.elementos().getFirst().fechaCambioUtc());
    }

    @Test
    void pasa_la_pagina_y_el_tamano_recibidos() {
        when(repositorio.consultarCambios(ADMIN, PARAMETRO, 2, 5)).thenReturn(new PaginaCambios(List.of(), 0));

        final Resultado resultado = useCase.execute(new Comando(PARAMETRO, 2, 5, ADMIN));

        assertEquals(2, resultado.numeroPagina());
        assertEquals(5, resultado.tamanoPagina());
        assertEquals(List.of(), resultado.elementos());
    }

    @Test
    void el_parametro_es_obligatorio_y_las_dependencias_tambien() {
        assertThrows(ValidationException.class, () -> useCase.execute(new Comando(null, 1, 20, ADMIN)));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(NullPointerException.class, () -> new ConsultarCambiosParametroUseCaseImpl(null));
    }
}
