package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.EstadoAsistenciaSesion;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaSesionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarSesionesMateriaEstudianteUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SesionMateriaEstudianteQueryPort queryPort = mock(SesionMateriaEstudianteQueryPort.class);
    private final AsistenciaSesionEstudianteQueryPort asistenciaPort = mock(AsistenciaSesionEstudianteQueryPort.class);
    private final ConsultarSesionesMateriaEstudianteUseCaseImpl useCase =
            new ConsultarSesionesMateriaEstudianteUseCaseImpl(scopePort, queryPort, asistenciaPort);

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
        when(queryPort.consultarSesionesMateria(estudiante, asignatura)).thenReturn(sesiones);
        when(asistenciaPort.consultarAsistenciaSesiones(estudiante, asignatura)).thenReturn(hechos);
    }

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_estudiante_y_no_consulta_nada() {
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(actor, asignatura));
        verify(queryPort, never()).consultarSesionesMateria(any(), any());
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
}
