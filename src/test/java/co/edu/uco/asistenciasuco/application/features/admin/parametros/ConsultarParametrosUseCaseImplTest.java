package co.edu.uco.asistenciasuco.application.features.admin.parametros;

import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.ConsultarParametrosInputPort.Comando;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.ConsultarParametrosInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.impl.ConsultarParametrosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort.PaginaParametros;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort.ParametroAdmin;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 D.3 (CONTRACT v2 §4.3, F-ADM-012): consulta paginada de parametros legibles; 100 % de ramas. */
class ConsultarParametrosUseCaseImplTest {

    private static final UUID ADMIN = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private final ParametroAdminRepositoryPort repositorio = mock(ParametroAdminRepositoryPort.class);
    private final ConsultarParametrosUseCaseImpl useCase = new ConsultarParametrosUseCaseImpl(repositorio);

    private static ParametroAdmin parametro(final List<String> valoresPermitidos) {
        return new ParametroAdmin(UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001"), "ASISTENCIA", "TOLERANCIA_MINUTOS", "15", "15",
                "INT", true, new BigDecimal("0.0000"), new BigDecimal("60.0000"), valoresPermitidos,
                "Tolerancia de apertura", "Minutos antes del inicio.", "minutos", "Asistencia", 10,
                LocalDateTime.of(2026, 11, 7, 15, 4, 5, 7_000_000), LocalDateTime.of(2026, 11, 7, 16, 0, 0), "Ana Admin");
    }

    @Test
    void sin_parametros_de_pagina_ni_filtros_usa_pagina_1_tamano_20_y_solo_editables() {
        when(repositorio.consultar(ADMIN, 1, 20, null, null, true))
                .thenReturn(new PaginaParametros(List.of(parametro(List.of("true", "false"))), 41));

        final Resultado resultado = useCase.execute(new Comando(null, null, null, null, null, ADMIN));

        assertEquals(1, resultado.numeroPagina());
        assertEquals(20, resultado.tamanoPagina());
        assertEquals(41, resultado.totalElementos());
        assertEquals(1, resultado.elementos().size());
        final var elemento = resultado.elementos().getFirst();
        assertEquals(List.of("true", "false"), elemento.valoresPermitidos());
        assertEquals(Instant.parse("2026-11-07T15:04:05.007Z"), elemento.fechaModificacion());
        assertEquals(Instant.parse("2026-11-07T16:00:00Z"), elemento.fechaUltimoCambioUtc());
        assertEquals(new BigDecimal("0.0000"), elemento.valorMinimo());
    }

    @Test
    void los_valores_recibidos_se_pasan_tal_cual_y_sin_ultimo_cambio_queda_nulo() {
        final ParametroAdmin sinCambio = new ParametroAdmin(UUID.randomUUID(), "RED", "CIDRS_CAMPUS", "10.0.0.0/8", "10.0.0.0/8", "STRING",
                false, null, null, List.of(), "CIDRS_CAMPUS", "", null, "RED", 0, LocalDateTime.of(2026, 1, 1, 0, 0), null, null);
        when(repositorio.consultar(ADMIN, 3, 5, "RED", "cidr", false)).thenReturn(new PaginaParametros(List.of(sinCambio), 1));

        final Resultado resultado = useCase.execute(new Comando(3, 5, "RED", "cidr", false, ADMIN));

        assertEquals(3, resultado.numeroPagina());
        assertEquals(5, resultado.tamanoPagina());
        assertNull(resultado.elementos().getFirst().fechaUltimoCambioUtc());
        verify(repositorio).consultar(ADMIN, 3, 5, "RED", "cidr", false);
    }

    @Test
    void el_comando_y_el_repositorio_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(NullPointerException.class, () -> new ConsultarParametrosUseCaseImpl(null));
    }
}
