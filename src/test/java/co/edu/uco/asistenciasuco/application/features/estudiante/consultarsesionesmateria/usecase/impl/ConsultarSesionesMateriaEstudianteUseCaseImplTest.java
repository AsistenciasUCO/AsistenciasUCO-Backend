package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.EstadoAsistenciaSesion;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaSesionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarSesionesMateriaEstudianteUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SesionMateriaEstudianteQueryPort queryPort = mock(SesionMateriaEstudianteQueryPort.class);
    private final AsistenciaSesionEstudianteQueryPort asistenciaPort = mock(AsistenciaSesionEstudianteQueryPort.class);
    private final SesionProcedenciaQueryPort procedenciaPort = mock(SesionProcedenciaQueryPort.class);
    private final ParameterCatalogPort parametros = mock(ParameterCatalogPort.class);
    private final ConsultarSesionesMateriaEstudianteUseCaseImpl useCase =
            new ConsultarSesionesMateriaEstudianteUseCaseImpl(scopePort, queryPort, asistenciaPort, procedenciaPort, parametros);

    private final UUID actor = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();
    private final UUID asignatura = UUID.randomUUID();
    private final UUID sesion1 = UUID.randomUUID();
    private final UUID sesion2 = UUID.randomUUID();

    private SesionMateriaEstudianteProjection sesion(final UUID id, final int numero, final UUID grupo) {
        return new SesionMateriaEstudianteProjection(id, "Sesion " + numero, numero, "SES-0" + numero, numero, grupo,
                "1", "Grupo 001", LocalDateTime.of(2026, 8, 17, 13, 0), LocalDateTime.of(2026, 8, 17, 15, 0));
    }

    private static AsistenciaSesionEstudianteProjection hecho(final UUID sesion, final Integer codigo,
                                                              final Boolean asistio, final String razon) {
        return new AsistenciaSesionEstudianteProjection(sesion, codigo, asistio, razon, null, null, null);
    }

    private static AsistenciaSesionEstudianteProjection hechoConSolicitud(final UUID sesion, final String estado,
                                                                          final LocalDate fecha) {
        return new AsistenciaSesionEstudianteProjection(sesion, 1, Boolean.FALSE, "SJC", UUID.randomUUID(), estado, fecha);
    }

    private void dado(final List<SesionMateriaEstudianteProjection> sesiones,
                      final List<AsistenciaSesionEstudianteProjection> hechos) {
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(estudiante));
        when(queryPort.consultarSesionesMateria(actor, asignatura, 1, 100))
                .thenReturn(new PaginaResultado<>(sesiones, 1, 100, sesiones.size()));
        when(asistenciaPort.consultarAsistenciaSesiones(estudiante, asignatura)).thenReturn(hechos);
    }

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_estudiante_y_no_consulta_nada() {
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(actor, asignatura));
        verify(queryPort, never()).consultarSesionesMateria(any(), any(), anyInt(), anyInt());
        verify(asistenciaPort, never()).consultarAsistenciaSesiones(any(), any());
    }

    @Test
    void la_asistencia_se_consulta_solo_con_el_estudiante_resuelto_del_usuario_autenticado() {
        dado(List.of(sesion(sesion1, 1, UUID.randomUUID())), List.of());

        useCase.execute(actor, asignatura);

        verify(scopePort).findEstudianteIdByUsuario(actor);
        verify(asistenciaPort).consultarAsistenciaSesiones(estudiante, asignatura);
    }

    @Test
    void sesion_sin_registro_de_asistencia_es_no_disponible_y_no_reclamable() {
        dado(List.of(sesion(sesion1, 1, UUID.randomUUID())), List.of());

        final SesionMateriaEstudianteDomain result = useCase.execute(actor, asignatura).getFirst();

        assertEquals(EstadoAsistenciaSesion.NO_DISPONIBLE, result.estadoAsistencia());
        assertFalse(result.reclamable());
        assertNull(result.estadoSolicitudRevision());
    }

    @Test
    void cabecera_sin_detalle_es_no_disponible_y_no_ausencia() {
        dado(List.of(sesion(sesion1, 1, UUID.randomUUID())), List.of(hecho(sesion1, null, null, null)));

        final SesionMateriaEstudianteDomain result = useCase.execute(actor, asignatura).getFirst();

        assertEquals(EstadoAsistenciaSesion.NO_DISPONIBLE, result.estadoAsistencia());
        assertFalse(result.reclamable());
    }

    @Test
    void asistio_verdadero_con_asistencia_normal_es_presente_y_no_reclamable() {
        dado(List.of(sesion(sesion1, 1, UUID.randomUUID())), List.of(hecho(sesion1, 1, Boolean.TRUE, "AN")));

        final SesionMateriaEstudianteDomain result = useCase.execute(actor, asignatura).getFirst();

        assertEquals(EstadoAsistenciaSesion.PRESENTE, result.estadoAsistencia());
        assertFalse(result.reclamable());
    }

    @Test
    void asistio_falso_sin_justa_causa_es_ausente_y_reclamable() {
        dado(List.of(sesion(sesion2, 2, UUID.randomUUID())), List.of(hecho(sesion2, 2, Boolean.FALSE, "SJC")));

        final SesionMateriaEstudianteDomain result = useCase.execute(actor, asignatura).getFirst();

        assertEquals(EstadoAsistenciaSesion.AUSENTE, result.estadoAsistencia());
        assertTrue(result.reclamable());
        assertNull(result.estadoSolicitudRevision());
    }

    @Test
    void asistio_falso_con_excusa_es_justificada_y_no_reclamable() {
        dado(List.of(sesion(sesion2, 2, UUID.randomUUID())), List.of(hecho(sesion2, 2, Boolean.FALSE, "ex")));

        final SesionMateriaEstudianteDomain result = useCase.execute(actor, asignatura).getFirst();

        assertEquals(EstadoAsistenciaSesion.JUSTIFICADA, result.estadoAsistencia());
        assertFalse(result.reclamable());
    }

    @Test
    void combinaciones_fuera_del_contrato_no_se_interpretan_ni_se_inventa_retardo() {
        final List<SesionMateriaEstudianteProjection> sesiones = List.of(
                sesion(UUID.randomUUID(), 1, UUID.randomUUID()), sesion(UUID.randomUUID(), 2, UUID.randomUUID()),
                sesion(UUID.randomUUID(), 3, UUID.randomUUID()), sesion(UUID.randomUUID(), 4, UUID.randomUUID()));
        dado(sesiones, List.of(
                hecho(sesiones.get(0).id(), 1, Boolean.TRUE, "SJC"),
                hecho(sesiones.get(1).id(), 1, Boolean.FALSE, "AN"),
                hecho(sesiones.get(2).id(), 1, Boolean.FALSE, "RETARDO"),
                hecho(sesiones.get(3).id(), 1, Boolean.FALSE, null)));

        useCase.execute(actor, asignatura).forEach(result -> {
            assertEquals(EstadoAsistenciaSesion.NO_DISPONIBLE, result.estadoAsistencia());
            assertFalse(result.reclamable());
        });
    }

    @Test
    void con_varios_detalles_prevalece_el_de_mayor_codigo() {
        dado(List.of(sesion(sesion1, 1, UUID.randomUUID())), List.of(
                hecho(sesion1, 1, Boolean.FALSE, "SJC"),
                hecho(sesion1, 2, Boolean.TRUE, "AN")));

        assertEquals(EstadoAsistenciaSesion.PRESENTE, useCase.execute(actor, asignatura).getFirst().estadoAsistencia());
    }

    @Test
    void ausencia_con_solicitud_vigente_no_es_reclamable_y_expone_el_estado_de_la_solicitud() {
        dado(List.of(sesion(sesion2, 2, UUID.randomUUID())),
                List.of(hechoConSolicitud(sesion2, "pendiente", LocalDate.of(2026, 8, 28))));

        final SesionMateriaEstudianteDomain result = useCase.execute(actor, asignatura).getFirst();

        assertEquals(EstadoAsistenciaSesion.AUSENTE, result.estadoAsistencia());
        assertEquals("pendiente", result.estadoSolicitudRevision());
        assertFalse(result.reclamable());
    }

    @Test
    void con_varias_solicitudes_se_reporta_la_mas_reciente_y_ninguna_permite_duplicar() {
        dado(List.of(sesion(sesion2, 2, UUID.randomUUID())), List.of(
                hechoConSolicitud(sesion2, "rechazada", LocalDate.of(2026, 8, 20)),
                hechoConSolicitud(sesion2, "pendiente", LocalDate.of(2026, 8, 28))));

        final SesionMateriaEstudianteDomain result = useCase.execute(actor, asignatura).getFirst();

        assertEquals("pendiente", result.estadoSolicitudRevision());
        assertFalse(result.reclamable());
    }

    @Test
    void solicitud_sin_nombre_de_estado_se_reporta_como_desconocida_sin_permitir_duplicados() {
        dado(List.of(sesion(sesion2, 2, UUID.randomUUID())), List.of(hechoConSolicitud(sesion2, " ", null)));

        final SesionMateriaEstudianteDomain result = useCase.execute(actor, asignatura).getFirst();

        assertEquals("DESCONOCIDO", result.estadoSolicitudRevision());
        assertFalse(result.reclamable());
    }

    @Test
    void asignatura_con_varios_grupos_conserva_identidad_de_grupo_y_estado_por_sesion() {
        final UUID grupoA = UUID.randomUUID();
        final UUID grupoB = UUID.randomUUID();
        dado(List.of(sesion(sesion1, 1, grupoA), sesion(sesion2, 1, grupoB)), List.of(
                hecho(sesion1, 1, Boolean.TRUE, "AN"), hecho(sesion2, 1, Boolean.FALSE, "SJC")));

        final List<SesionMateriaEstudianteDomain> result = useCase.execute(actor, asignatura);

        assertEquals(grupoA, result.get(0).idGrupo());
        assertEquals(EstadoAsistenciaSesion.PRESENTE, result.get(0).estadoAsistencia());
        assertEquals(grupoB, result.get(1).idGrupo());
        assertEquals(EstadoAsistenciaSesion.AUSENTE, result.get(1).estadoAsistencia());
    }

    @Test
    void hechos_de_sesiones_ajenas_a_la_lista_no_se_filtran_a_otras_sesiones() {
        dado(List.of(sesion(sesion1, 1, UUID.randomUUID())),
                List.of(hecho(UUID.randomUUID(), 1, Boolean.FALSE, "SJC")));

        assertEquals(EstadoAsistenciaSesion.NO_DISPONIBLE,
                useCase.execute(actor, asignatura).getFirst().estadoAsistencia());
    }

    @Test
    void preserva_los_instantes_de_la_sesion_sin_transformarlos() {
        dado(List.of(sesion(sesion1, 1, UUID.randomUUID())), List.of());

        final SesionMateriaEstudianteDomain result = useCase.execute(actor, asignatura).getFirst();

        assertEquals(LocalDateTime.of(2026, 8, 17, 13, 0), result.fechaHoraInicio());
        assertEquals(LocalDateTime.of(2026, 8, 17, 15, 0), result.fechaHoraFin());
    }

    // APP-04 (estudiante): los instantes UTC solo se exponen con procedencia CONFIRMADA, igual que HU060 coordinador.
    private static SesionProcedenciaRepositoryProjection procedencia(final UUID sesion, final UUID grupo,
                                                                    final String procedenciaTemporal) {
        return new SesionProcedenciaRepositoryProjection(sesion, grupo, "Sesion", 1, "SES", 1, "1", "Grupo 001",
                LocalDateTime.of(2026, 8, 17, 13, 0), LocalDateTime.of(2026, 8, 17, 15, 0), procedenciaTemporal);
    }

    // CULM-001 B.1 (3.2.4): una sesion cancelada no muestra asistencia ni es reclamable aunque tenga una falta registrada.
    @Test
    void sesion_cancelada_es_no_disponible_no_reclamable_y_trae_su_cancelacion() {
        final UUID grupo = UUID.randomUUID();
        dado(List.of(sesion(sesion1, 1, grupo)), List.of(hecho(sesion1, 1, Boolean.FALSE, "SJC")));
        when(procedenciaPort.consultarSesionesPorGrupo(grupo)).thenReturn(List.of(new SesionProcedenciaRepositoryProjection(
                sesion1, grupo, "Sesion", 1, "SES", 1, "1", "Grupo 001", LocalDateTime.of(2026, 8, 17, 13, 0),
                LocalDateTime.of(2026, 8, 17, 15, 0), "UTC_V2",
                new co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CancelacionSesionProjection(
                        "DOCENTE", "Calamidad domestica", null, LocalDateTime.of(2026, 8, 16, 20, 0)))));

        final SesionMateriaEstudianteDomain result = useCase.execute(actor, asignatura).getFirst();

        assertEquals(EstadoAsistenciaSesion.NO_DISPONIBLE, result.estadoAsistencia());
        assertFalse(result.reclamable());
        assertEquals("DOCENTE", result.cancelacion().tipo());
        final var dto = co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.mapper
                .ConsultarSesionesMateriaEstudianteMapper.toDTO(result);
        assertEquals("CANCELADA", dto.estado());
        assertEquals("Calamidad domestica", dto.cancelacion().motivo());
    }

    @Test
    void app04_sesion_con_procedencia_confirmada_expone_instantes_utc() {
        final UUID grupo = UUID.randomUUID();
        dado(List.of(sesion(sesion1, 1, grupo)), List.of());
        when(procedenciaPort.consultarSesionesPorGrupo(grupo)).thenReturn(List.of(procedencia(sesion1, grupo, "UTC_V2")));

        final SesionMateriaEstudianteDomain result = useCase.execute(actor, asignatura).getFirst();

        assertEquals(EstadoHorarioSesion.CONFIRMADA, result.estadoTemporal());
        assertEquals(LocalDateTime.of(2026, 8, 17, 13, 0), result.fechaHoraInicioUtc());
        assertEquals(LocalDateTime.of(2026, 8, 17, 15, 0), result.fechaHoraFinUtc());
        assertEquals(LocalDateTime.of(2026, 8, 17, 13, 0), result.fechaHoraInicio());
    }

    @Test
    void app04_procedencia_indeterminada_o_ausente_no_ofrece_instantes() {
        final UUID grupo = UUID.randomUUID();
        dado(List.of(sesion(sesion1, 1, grupo), sesion(sesion2, 2, grupo)), List.of());
        when(procedenciaPort.consultarSesionesPorGrupo(grupo)).thenReturn(List.of(procedencia(sesion1, grupo, null)));

        final List<SesionMateriaEstudianteDomain> result = useCase.execute(actor, asignatura);

        result.forEach(r -> {
            assertEquals(EstadoHorarioSesion.INDETERMINADA, r.estadoTemporal());
            assertNull(r.fechaHoraInicioUtc());
            assertNull(r.fechaHoraFinUtc());
        });
    }

    @Test
    void app04_procedencia_de_otro_grupo_no_se_aplica_y_se_consulta_una_vez_por_grupo() {
        final UUID grupoA = UUID.randomUUID();
        final UUID grupoB = UUID.randomUUID();
        dado(List.of(sesion(sesion1, 1, grupoA), sesion(sesion2, 2, grupoA)), List.of());
        when(procedenciaPort.consultarSesionesPorGrupo(grupoA))
                .thenReturn(List.of(procedencia(sesion1, grupoB, "UTC_V2"), procedencia(sesion2, grupoA, "UTC_V2")));

        final List<SesionMateriaEstudianteDomain> result = useCase.execute(actor, asignatura);

        assertEquals(EstadoHorarioSesion.INDETERMINADA, result.get(0).estadoTemporal());
        assertEquals(EstadoHorarioSesion.CONFIRMADA, result.get(1).estadoTemporal());
        verify(procedenciaPort, times(1)).consultarSesionesPorGrupo(grupoA);
    }

    @Test
    void app04_sin_sesiones_no_consulta_procedencia() {
        dado(List.of(), List.of());

        assertTrue(useCase.execute(actor, asignatura).isEmpty());
        verify(procedenciaPort, never()).consultarSesionesPorGrupo(any());
    }

    // ---- CULM-001 D.1 (HU026B): pagina, tamano por defecto, usuario ejecutor y fecha limite ----

    @Test
    void el_puerto_recibe_el_usuario_autenticado_y_la_pagina_por_defecto() {
        dado(List.of(sesion(sesion1, 1, UUID.randomUUID())), List.of());

        useCase.execute(actor, asignatura);

        verify(queryPort).consultarSesionesMateria(actor, asignatura, 1, 100);
    }

    @Test
    void el_tamano_por_defecto_sale_del_parametro_paginacion_tamano_maximo() {
        when(parametros.getParameter("PAGINACION", "TAMANO_MAXIMO")).thenReturn(Optional.of(" 50 "));
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(estudiante));
        when(queryPort.consultarSesionesMateria(actor, asignatura, 1, 50)).thenReturn(new PaginaResultado<>(List.of(), 1, 50, 0));

        assertTrue(useCase.execute(actor, asignatura, null, null).isEmpty());

        verify(queryPort).consultarSesionesMateria(actor, asignatura, 1, 50);
    }

    @Test
    void un_parametro_de_tamano_ausente_o_mal_formado_usa_100_como_la_semilla() {
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(estudiante));
        when(queryPort.consultarSesionesMateria(actor, asignatura, 1, 100)).thenReturn(new PaginaResultado<>(List.of(), 1, 100, 0));
        when(parametros.getParameter("PAGINACION", "TAMANO_MAXIMO")).thenReturn(Optional.of("abc"), Optional.of(""), Optional.empty());

        useCase.execute(actor, asignatura);
        useCase.execute(actor, asignatura);
        useCase.execute(actor, asignatura);

        verify(queryPort, times(3)).consultarSesionesMateria(actor, asignatura, 1, 100);
    }

    @Test
    void la_pagina_y_el_tamano_explicitos_llegan_al_puerto() {
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(estudiante));
        when(queryPort.consultarSesionesMateria(actor, asignatura, 2, 10)).thenReturn(new PaginaResultado<>(List.of(), 2, 10, 0));

        useCase.execute(actor, asignatura, 2, 10);

        verify(queryPort).consultarSesionesMateria(actor, asignatura, 2, 10);
    }

    @Test
    void la_fecha_limite_y_el_plazo_vigente_del_sp_llegan_al_dominio_sin_recalcularse() {
        final LocalDateTime limite = LocalDateTime.of(2026, 11, 10, 5, 0);
        final var vigente = new SesionMateriaEstudianteProjection(sesion1, "Sesion 1", 1, "SES-01", 1, UUID.randomUUID(), "1",
                "Grupo 001", LocalDateTime.of(2026, 11, 4, 12, 0), LocalDateTime.of(2026, 11, 4, 13, 0), limite, true);
        final var vencida = new SesionMateriaEstudianteProjection(sesion2, "Sesion 2", 2, "SES-02", 2, UUID.randomUUID(), "1",
                "Grupo 001", LocalDateTime.of(2026, 8, 17, 13, 0), LocalDateTime.of(2026, 8, 17, 15, 0),
                LocalDateTime.of(2026, 8, 23, 5, 0), false);
        final var sinParametro = new SesionMateriaEstudianteProjection(UUID.randomUUID(), "Sesion 3", 3, "SES-03", 3,
                UUID.randomUUID(), "1", "Grupo 001", LocalDateTime.of(2026, 8, 18, 13, 0), LocalDateTime.of(2026, 8, 18, 15, 0), null, false);
        dado(List.of(vigente, vencida, sinParametro), List.of());

        final List<SesionMateriaEstudianteDomain> result = useCase.execute(actor, asignatura);

        assertEquals(limite, result.get(0).fechaLimiteJustificacionUtc());
        assertTrue(result.get(0).plazoJustificacionVigente());
        assertEquals(LocalDateTime.of(2026, 8, 23, 5, 0), result.get(1).fechaLimiteJustificacionUtc());
        assertFalse(result.get(1).plazoJustificacionVigente());
        assertNull(result.get(2).fechaLimiteJustificacionUtc());
        assertFalse(result.get(2).plazoJustificacionVigente());
    }

    @Test
    void si_el_total_supera_la_pagina_se_devuelve_la_primera_pagina_sin_fallar() {
        final var primera = sesion(sesion1, 1, UUID.randomUUID());
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(estudiante));
        when(queryPort.consultarSesionesMateria(actor, asignatura, 1, 100))
                .thenReturn(new PaginaResultado<>(List.of(primera), 1, 100, 250));
        when(asistenciaPort.consultarAsistenciaSesiones(estudiante, asignatura)).thenReturn(List.of());

        final List<SesionMateriaEstudianteDomain> result = useCase.execute(actor, asignatura);

        assertEquals(1, result.size());
        assertEquals(sesion1, result.getFirst().id());
    }

    @Test
    void un_tamano_no_positivo_o_un_fallo_del_catalogo_usan_100() {
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(estudiante));
        when(queryPort.consultarSesionesMateria(actor, asignatura, 1, 100)).thenReturn(new PaginaResultado<>(List.of(), 1, 100, 0));
        when(parametros.getParameter("PAGINACION", "TAMANO_MAXIMO")).thenReturn(Optional.of("0"))
                .thenThrow(new ParameterCatalogPort.ParameterCatalogException("catalogo caido"));

        useCase.execute(actor, asignatura);
        useCase.execute(actor, asignatura);

        verify(queryPort, times(2)).consultarSesionesMateria(actor, asignatura, 1, 100);
    }

    @Test
    void con_pagina_explicita_un_total_mayor_es_normal_y_no_cambia_el_resultado() {
        final var segunda = sesion(sesion2, 2, UUID.randomUUID());
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(estudiante));
        when(queryPort.consultarSesionesMateria(actor, asignatura, 2, 1)).thenReturn(new PaginaResultado<>(List.of(segunda), 2, 1, 5));
        when(asistenciaPort.consultarAsistenciaSesiones(estudiante, asignatura)).thenReturn(List.of());

        final List<SesionMateriaEstudianteDomain> result = useCase.execute(actor, asignatura, 2, 1);

        assertEquals(1, result.size());
        assertEquals(sesion2, result.getFirst().id());
    }
}
