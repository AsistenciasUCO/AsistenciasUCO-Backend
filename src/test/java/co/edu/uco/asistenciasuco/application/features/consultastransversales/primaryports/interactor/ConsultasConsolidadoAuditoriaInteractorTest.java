package co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FiltroAuditoriaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FiltroConsolidadoDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.ConsultasTransversalesUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ComponenteFormacionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ConsolidadoGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.CategoriaJustificacionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.EstadoSistemaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.EventoAuditoriaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroAuditoria;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsolidado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.Pagina;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** HU061/HU172/HU147: conversion de filtros DTO a filtros del puerto y mapeo de proyecciones a DTO. */
class ConsultasConsolidadoAuditoriaInteractorTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-04T15:00:00Z");

    private final ConsultasTransversalesUseCase useCase = mock(ConsultasTransversalesUseCase.class);
    private final ConsultasTransversalesInteractor interactor = new ConsultasTransversalesInteractor(useCase);

    @Test
    void T_061_INT_01_consolidado_convierte_filtro_y_mapea_grupo() {
        when(useCase.consultarConsolidadoAsistencia(USUARIO, new FiltroConsolidado(ID, null, null, null), 1, 10))
                .thenReturn(new Pagina<>(List.of(new ConsolidadoGrupoProjection(ID, 1, "G1", ID, "2026-2", ID,
                        "Sistemas", ID, "Calculo", ID, "Ana", 20, 8, 150, 10, 0, new BigDecimal("93.75"),
                        new BigDecimal("6.25"), 2, 1)), 1));
        when(useCase.consultarConsolidadoAsistencia(USUARIO, null, null, null)).thenReturn(new Pagina<>(List.of(), 0));

        final var grupo = interactor.consultarConsolidadoAsistencia(USUARIO, new FiltroConsolidadoDTO(ID, null, null,
                null), 1, 10).elementos().getFirst();

        assertEquals("Calculo", grupo.nombreAsignatura());
        assertEquals(new BigDecimal("6.25"), grupo.porcentajeInasistencia());
        assertEquals(0, interactor.consultarConsolidadoAsistencia(USUARIO, null, null, null).total());
    }

    @Test
    void T_172_INT_01_auditoria_solo_asistencia_nulo_es_true_y_mapea_evento() {
        when(useCase.consultarAuditoria(USUARIO, new FiltroAuditoria(null, AHORA, "CREAR_SESION", null, null, null,
                null, true), 1, 10)).thenReturn(new Pagina<>(List.of(new EventoAuditoriaProjection(ID, AHORA,
                USUARIO.toString(), "USER", "Ana", "CREAR_SESION", "SESION", "S1", "SUCCESS", ID, "POST",
                "/api/v1/sesiones", 201, null, null, null, true)), 1));
        when(useCase.consultarAuditoria(USUARIO, null, null, null)).thenReturn(new Pagina<>(List.of(), 0));

        final var evento = interactor.consultarAuditoria(USUARIO, new FiltroAuditoriaDTO(null, AHORA, "CREAR_SESION",
                null, null, null, null, null), 1, 10).elementos().getFirst();

        assertEquals("Ana", evento.nombreActor());
        assertTrue(evento.accionAsistencia());
        assertEquals(0, interactor.consultarAuditoria(USUARIO, null, null, null).total());
    }

    @Test
    void T_147_INT_01_componentes_mapea_codigo_y_total() {
        when(useCase.consultarComponentes(USUARIO)).thenReturn(List.of(
                new ComponenteFormacionProjection(ID, "INS", "Institucionales", 3)));

        final var componente = interactor.consultarComponentes(USUARIO).getFirst();

        assertEquals("INS", componente.codigo());
        assertEquals(3, componente.totalAsignaturas());
    }

    @Test
    void T_155_INT_01_estados_mapea_catalogo_codigo_y_nombre() {
        when(useCase.consultarEstadosSistema()).thenReturn(List.of(
                new EstadoSistemaProjection("ESTUDIANTE_GRUPO", ID, "CI", "cancelado por inasistencia")));

        final var estado = interactor.consultarEstadosSistema().getFirst();

        assertEquals("ESTUDIANTE_GRUPO", estado.catalogo());
        assertEquals("CI", estado.codigo());
        assertEquals("cancelado por inasistencia", estado.nombre());
    }

    @Test
    void T_005_INT_01_categorias_mapea_codigo_nombre_y_descripcion() {
        when(useCase.consultarCategoriasJustificacion()).thenReturn(List.of(
                new CategoriaJustificacionProjection(ID, "FMA", "Fuerza Mayor", "Hecho imprevisible")));

        final var categoria = interactor.consultarCategoriasJustificacion().getFirst();

        assertEquals(ID, categoria.id());
        assertEquals("FMA", categoria.codigo());
        assertEquals("Fuerza Mayor", categoria.nombre());
        assertEquals("Hecho imprevisible", categoria.descripcion());
    }
}
