package co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ComponenteFormacionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.CategoriaJustificacionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.EstadoSistemaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroAuditoria;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsolidado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.Pagina;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU061 (consolidado), HU172 (auditoria: rango UTC por defecto y maximo) y HU147 (componentes). */
class ConsultasConsolidadoAuditoriaUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();
    private static final Instant AHORA = Instant.parse("2026-11-04T15:00:00Z");
    private static final OffsetDateTime AHORA_UTC = AHORA.atOffset(ZoneOffset.UTC);

    private final ConsultasTransversalesQueryPort port = mock(ConsultasTransversalesQueryPort.class);
    private final ConsultasTransversalesUseCaseImpl useCase =
            new ConsultasTransversalesUseCaseImpl(port, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void constructor_rechaza_clock_nulo() {
        assertThrows(NullPointerException.class, () -> new ConsultasTransversalesUseCaseImpl(port, null));
    }

    @Test
    void T_061_UC_01_consolidado_sin_filtro_usa_filtro_vacio_y_paginacion_por_defecto() {
        when(port.consultarConsolidadoAsistencia(any(), any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0));

        useCase.consultarConsolidadoAsistencia(USUARIO, null, null, null);

        verify(port).consultarConsolidadoAsistencia(USUARIO, new FiltroConsolidado(null, null, null, null), 1, 10);
    }

    @Test
    void T_061_UC_02_consolidado_conserva_filtros_y_rechaza_tamano_invalido() {
        final FiltroConsolidado filtro = new FiltroConsolidado(ID, ID, null, ID);
        when(port.consultarConsolidadoAsistencia(any(), any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 3));

        assertEquals(3, useCase.consultarConsolidadoAsistencia(USUARIO, filtro, 2, 50).total());
        verify(port).consultarConsolidadoAsistencia(USUARIO, filtro, 2, 50);
        assertThrows(ValidationException.class, () -> useCase.consultarConsolidadoAsistencia(USUARIO, filtro, 1, 101));
    }

    @Test
    void T_172_UC_01_auditoria_sin_rango_consulta_los_ultimos_7_dias_solo_asistencia() {
        when(port.consultarAuditoria(any(), any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0));

        useCase.consultarAuditoria(USUARIO, null, null, null);

        verify(port).consultarAuditoria(USUARIO, new FiltroAuditoria(AHORA_UTC.minusDays(7), AHORA_UTC, null, null,
                null, null, null, true), 1, 10);
    }

    @Test
    void T_172_UC_02_auditoria_normaliza_a_utc_y_filtros_de_texto() {
        final OffsetDateTime desde = OffsetDateTime.parse("2026-11-01T00:00:00-05:00");
        final OffsetDateTime hasta = OffsetDateTime.parse("2026-11-03T00:00:00-05:00");
        when(port.consultarAuditoria(any(), any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0));

        useCase.consultarAuditoria(USUARIO, new FiltroAuditoria(desde, hasta, " crear_sesion ", "  ", " S1 ", ID,
                "failure", false), 3, 20);

        verify(port).consultarAuditoria(USUARIO, new FiltroAuditoria(
                OffsetDateTime.parse("2026-11-01T05:00:00Z"), OffsetDateTime.parse("2026-11-03T05:00:00Z"),
                "CREAR_SESION", null, "S1", ID, "FAILURE", false), 3, 20);
    }

    @Test
    void T_172_UC_03_auditoria_solo_hasta_toma_7_dias_antes() {
        final OffsetDateTime hasta = OffsetDateTime.parse("2026-10-10T00:00:00Z");
        when(port.consultarAuditoria(any(), any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0));

        useCase.consultarAuditoria(USUARIO, new FiltroAuditoria(null, hasta, null, null, null, null, null, true), 1, 10);

        verify(port).consultarAuditoria(USUARIO, new FiltroAuditoria(hasta.minusDays(7), hasta, null, null, null,
                null, null, true), 1, 10);
    }

    @Test
    void T_172_UC_04_auditoria_rechaza_rangos_invalidos_y_resultado_fuera_de_contrato() {
        final OffsetDateTime hasta = AHORA_UTC;
        assertThrows(ValidationException.class, () -> useCase.consultarAuditoria(USUARIO,
                new FiltroAuditoria(hasta, hasta, null, null, null, null, null, true), 1, 10));
        assertThrows(ValidationException.class, () -> useCase.consultarAuditoria(USUARIO,
                new FiltroAuditoria(hasta.minusDays(91), hasta, null, null, null, null, null, true), 1, 10));
        assertThrows(ValidationException.class, () -> useCase.consultarAuditoria(USUARIO,
                new FiltroAuditoria(null, null, null, null, null, null, "OK", true), 1, 10));
        assertThrows(ValidationException.class, () -> useCase.consultarAuditoria(USUARIO,
                new FiltroAuditoria(null, null, "A".repeat(121), null, null, null, null, true), 1, 10));
        verifyNoInteractions(port);
    }

    @Test
    void T_172_UC_05_auditoria_acepta_exactamente_90_dias() {
        when(port.consultarAuditoria(any(), any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0));

        useCase.consultarAuditoria(USUARIO, new FiltroAuditoria(AHORA_UTC.minusDays(90), AHORA_UTC, null, null, null,
                null, "SUCCESS", true), 1, 10);

        verify(port).consultarAuditoria(USUARIO, new FiltroAuditoria(AHORA_UTC.minusDays(90), AHORA_UTC, null, null,
                null, null, "SUCCESS", true), 1, 10);
    }

    @Test
    void T_147_UC_01_componentes_delega_y_exige_usuario() {
        final List<ComponenteFormacionProjection> componentes = List.of(
                new ComponenteFormacionProjection(ID, "ESP", "Especificas", 4));
        when(port.consultarComponentes(USUARIO)).thenReturn(componentes);

        assertEquals(componentes, useCase.consultarComponentes(USUARIO));
        assertThrows(ForbiddenException.class, () -> useCase.consultarComponentes(null));
        assertThrows(ForbiddenException.class, () -> useCase.consultarAuditoria(null, null, null, null));
    }

    @Test
    void T_155_UC_01_estados_del_sistema_delega_en_el_puerto() {
        final List<EstadoSistemaProjection> estados = List.of(new EstadoSistemaProjection("SOLICITUD", ID, "P", "pendiente"));
        when(port.consultarEstadosSistema()).thenReturn(estados);

        assertEquals(estados, useCase.consultarEstadosSistema());
    }

    @Test
    void T_005_UC_01_categorias_de_justificacion_delega_en_el_puerto() {
        final List<CategoriaJustificacionProjection> categorias = List.of(
                new CategoriaJustificacionProjection(ID, "LAB", "Laboral", "Obligacion laboral"));
        when(port.consultarCategoriasJustificacion()).thenReturn(categorias);

        assertEquals(categorias, useCase.consultarCategoriasJustificacion());
    }
}
