package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CancelacionSesionProjection;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.interactor.ConsultarAsistenciaProgramaInteractor;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.AsistenciaSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.GrupoProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.SesionGrupoProjection;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.domain.SesionGrupoHorarioDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU064/HU060/HU061: reglas de la consulta de solo lectura del coordinador. */
class ConsultarAsistenciaProgramaUseCaseImplTest {

    private static final UUID COORDINADOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();

    private final AsistenciaProgramaQueryPort queryPort = mock(AsistenciaProgramaQueryPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SesionProcedenciaQueryPort procedenciaPort = mock(SesionProcedenciaQueryPort.class);
    private ConsultarAsistenciaProgramaUseCaseImpl useCase;

    @BeforeEach
    void preparar() {
        useCase = new ConsultarAsistenciaProgramaUseCaseImpl(queryPort, scopePort, procedenciaPort);
        when(scopePort.findProgramaIdByCoordinadorUsuario(COORDINADOR)).thenReturn(Optional.of(PROGRAMA));
    }

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class,
                () -> new ConsultarAsistenciaProgramaUseCaseImpl(null, scopePort, procedenciaPort));
        assertThrows(NullPointerException.class,
                () -> new ConsultarAsistenciaProgramaUseCaseImpl(queryPort, null, procedenciaPort));
        assertThrows(NullPointerException.class,
                () -> new ConsultarAsistenciaProgramaUseCaseImpl(queryPort, scopePort, null));
    }

    @Test
    void T_064_01_grupos_aplica_valores_por_defecto_y_normaliza_busqueda() {
        final Pagina<GrupoProgramaProjection> pagina = new Pagina<>(List.of(grupo()), 1);
        when(queryPort.consultarGrupos(COORDINADOR, PERIODO, "sis", 1, 10)).thenReturn(pagina);

        assertEquals(pagina, useCase.consultarGrupos(COORDINADOR, PERIODO, "  sis ", null, null));
    }

    @Test
    void T_064_02_grupos_busqueda_en_blanco_se_envia_nula() {
        when(queryPort.consultarGrupos(COORDINADOR, null, null, 2, 5)).thenReturn(new Pagina<>(List.of(), 0));

        useCase.consultarGrupos(COORDINADOR, null, "   ", 2, 5);

        verify(queryPort).consultarGrupos(COORDINADOR, null, null, 2, 5);
    }

    @Test
    void T_064_03_usuario_sin_programa_de_coordinador_es_403() {
        final UUID otro = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(otro)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.consultarGrupos(otro, null, null, 1, 10));
        assertThrows(ForbiddenException.class, () -> useCase.consultarGrupos(null, null, null, 1, 10));
        verifyNoInteractions(queryPort);
    }

    @Test
    void T_064_04_paginacion_fuera_de_rango_es_400() {
        assertThrows(ValidationException.class, () -> useCase.consultarGrupos(COORDINADOR, null, null, 0, 10));
        assertThrows(ValidationException.class, () -> useCase.consultarGrupos(COORDINADOR, null, null, 1, 0));
        assertThrows(ValidationException.class, () -> useCase.consultarGrupos(COORDINADOR, null, null, 1, 101));
        verifyNoInteractions(queryPort);
    }

    @Test
    void T_060_01_sesiones_envia_rango_de_fechas() {
        final LocalDate desde = LocalDate.of(2026, 2, 1);
        final LocalDate hasta = LocalDate.of(2026, 3, 1);
        when(queryPort.consultarSesionesGrupo(COORDINADOR, GRUPO, desde, hasta, 1, 100))
                .thenReturn(new Pagina<>(List.of(sesion()), 1));

        assertEquals(1, useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, desde, hasta, 1, 100).total());
    }

    @Test
    void T_060_02_sesiones_exige_grupo_y_rango_coherente() {
        assertThrows(ValidationException.class,
                () -> useCase.consultarSesionesGrupo(COORDINADOR, null, null, null, 1, 10));
        assertThrows(ValidationException.class, () -> useCase.consultarSesionesGrupo(COORDINADOR, GRUPO,
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1), 1, 10));
        verifyNoInteractions(queryPort);
    }

    @Test
    void T_060_03_sesiones_con_una_sola_fecha_es_valido() {
        when(queryPort.consultarSesionesGrupo(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0));

        useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, LocalDate.of(2026, 3, 2), null, null, null);
        useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, null, LocalDate.of(2026, 3, 2), null, null);

        verify(queryPort).consultarSesionesGrupo(COORDINADOR, GRUPO, LocalDate.of(2026, 3, 2), null, 1, 10);
    }

    @Test
    void T_APP04_01_sesion_confirmada_expone_instante_utc_y_conserva_el_valor_v1() {
        final LocalDateTime inicioUtc = LocalDateTime.of(2026, 11, 3, 15, 0, 0, 123_456_700);
        final LocalDateTime finUtc = LocalDateTime.of(2026, 11, 3, 17, 0);
        when(queryPort.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, 1, 10))
                .thenReturn(new Pagina<>(List.of(sesion()), 1));
        when(procedenciaPort.consultarSesionesPorGrupo(GRUPO))
                .thenReturn(List.of(procedencia(SESION, GRUPO, inicioUtc, finUtc, "UTC_V2")));

        final var sesion = useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, null, null)
                .elementos().getFirst();

        assertEquals(EstadoHorarioSesion.CONFIRMADA, sesion.estadoTemporal());
        assertEquals(inicioUtc, sesion.fechaHoraInicioUtc());
        assertEquals(finUtc, sesion.fechaHoraFinUtc());
        assertEquals(LocalDateTime.of(2026, 2, 3, 7, 0), sesion.fechaHoraInicio());
    }

    @Test
    void T_APP04_02_sin_procedencia_confirmada_no_se_ofrece_instante() {
        final UUID legado = UUID.randomUUID();
        final UUID sinFila = UUID.randomUUID();
        when(queryPort.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, 1, 10))
                .thenReturn(new Pagina<>(List.of(sesion(legado), sesion(sinFila)), 2));
        when(procedenciaPort.consultarSesionesPorGrupo(GRUPO)).thenReturn(List.of(
                procedencia(legado, GRUPO, LocalDateTime.of(2026, 2, 3, 12, 0), LocalDateTime.of(2026, 2, 3, 14, 0),
                        null)));

        final var sesiones = useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, null, null).elementos();

        assertEquals(2, sesiones.size());
        for (final var sesion : sesiones) {
            assertEquals(EstadoHorarioSesion.INDETERMINADA, sesion.estadoTemporal());
            assertNull(sesion.fechaHoraInicioUtc());
            assertNull(sesion.fechaHoraFinUtc());
        }
    }

    @Test
    void T_APP04_03_procedencia_de_otro_grupo_no_se_usa() {
        when(queryPort.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, 1, 10))
                .thenReturn(new Pagina<>(List.of(sesion()), 1));
        when(procedenciaPort.consultarSesionesPorGrupo(GRUPO)).thenReturn(List.of(procedencia(SESION,
                UUID.randomUUID(), LocalDateTime.of(2026, 2, 3, 12, 0), LocalDateTime.of(2026, 2, 3, 14, 0), "UTC_V2")));

        final var sesion = useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, null, null)
                .elementos().getFirst();

        assertEquals(EstadoHorarioSesion.INDETERMINADA, sesion.estadoTemporal());
        assertNull(sesion.fechaHoraInicioUtc());
    }

    @Test
    void T_APP04_04_pagina_vacia_no_consulta_procedencia() {
        when(queryPort.consultarSesionesGrupo(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0));

        useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, null, null);

        verifyNoInteractions(procedenciaPort);
    }

    @Test
    void T_APP04_05_dominio_descarta_instantes_si_la_procedencia_no_es_confirmada() {
        final var dominio = horario(SESION, LocalDateTime.of(2026, 2, 3, 12, 0), EstadoHorarioSesion.INDETERMINADA);

        assertNull(dominio.fechaHoraInicioUtc());
        assertNull(dominio.fechaHoraFinUtc());
        assertEquals(LocalDateTime.of(2026, 2, 3, 7, 0), dominio.fechaHoraInicio());
        assertThrows(NullPointerException.class, () -> horario(null, null, EstadoHorarioSesion.INDETERMINADA));
        assertThrows(NullPointerException.class, () -> horario(SESION, null, null));
    }

    private static SesionGrupoHorarioDomain horario(final UUID id, final LocalDateTime utc,
                                                    final EstadoHorarioSesion estado) {
        return new SesionGrupoHorarioDomain(id, GRUPO, 1, "Grupo 1", "Sesion 1", 1, "S-01", 2,
                LocalDateTime.of(2026, 2, 3, 7, 0), LocalDateTime.of(2026, 2, 3, 9, 0), utc, utc, estado);
    }

    @Test
    void T_061_01_asistencias_envia_filtro_asistio() {
        when(queryPort.consultarAsistenciasSesion(COORDINADOR, SESION, false, 1, 10))
                .thenReturn(new Pagina<>(List.of(asistencia()), 1));

        assertFalse(useCase.consultarAsistenciasSesion(COORDINADOR, SESION, false, null, null).elementos().getFirst()
                .asistio());
    }

    @Test
    void T_061_02_asistencias_exige_sesion() {
        assertThrows(ValidationException.class,
                () -> useCase.consultarAsistenciasSesion(COORDINADOR, null, null, 1, 10));
        verifyNoInteractions(queryPort);
    }

    @Test
    void interactor_mapea_proyecciones_a_dto_con_pagina_y_tamano() {
        final ConsultarAsistenciaProgramaInteractor interactor = new ConsultarAsistenciaProgramaInteractor(useCase);
        when(procedenciaPort.consultarSesionesPorGrupo(GRUPO)).thenReturn(List.of(procedencia(SESION, GRUPO,
                LocalDateTime.of(2026, 2, 3, 12, 0), LocalDateTime.of(2026, 2, 3, 14, 0), "UTC_GENERADOR")));
        when(queryPort.consultarGrupos(COORDINADOR, null, null, 1, 10)).thenReturn(new Pagina<>(List.of(grupo()), 7));
        when(queryPort.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, 2, 5))
                .thenReturn(new Pagina<>(List.of(sesion()), 6));
        when(queryPort.consultarAsistenciasSesion(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(asistencia()), 1));

        final var grupos = interactor.consultarGrupos(COORDINADOR, null, null, null, null);
        assertEquals(7, grupos.total());
        assertEquals(1, grupos.pagina());
        assertEquals(10, grupos.tamano());
        assertEquals("Grupo 1", grupos.elementos().getFirst().nombre());
        assertTrue(grupos.elementos().getFirst().habilitado());

        final var sesiones = interactor.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, 2, 5);
        assertEquals(2, sesiones.pagina());
        assertEquals("S-01", sesiones.elementos().getFirst().codigo());
        assertEquals(LocalDateTime.of(2026, 2, 3, 7, 0), sesiones.elementos().getFirst().fechaHoraInicio());
        assertEquals(LocalDateTime.of(2026, 2, 3, 12, 0), sesiones.elementos().getFirst().fechaHoraInicioUtc());
        assertEquals(LocalDateTime.of(2026, 2, 3, 14, 0), sesiones.elementos().getFirst().fechaHoraFinUtc());
        assertEquals("CONFIRMADA", sesiones.elementos().getFirst().estadoTemporal());

        final var asistencias = interactor.consultarAsistenciasSesion(COORDINADOR, SESION, null, null, null);
        assertEquals("Ana Ruiz", asistencias.elementos().getFirst().nombreCompleto());
        assertEquals("INJ", asistencias.elementos().getFirst().codigoRazonCausa());
    }

    // CULM-001 B.1 (3.2.4): el cronograma del coordinador muestra la sesion cancelada con su cancelacion.
    @Test
    void T_B1_01_sesion_cancelada_expone_estado_y_cancelacion_hasta_el_dto() {
        final ConsultarAsistenciaProgramaInteractor interactor = new ConsultarAsistenciaProgramaInteractor(useCase);
        when(queryPort.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, 1, 10))
                .thenReturn(new Pagina<>(List.of(sesion()), 1));
        when(procedenciaPort.consultarSesionesPorGrupo(GRUPO)).thenReturn(List.of(new SesionProcedenciaRepositoryProjection(
                SESION, GRUPO, "Sesion 1", 1, "S-01", 2, "1", "Grupo 1", LocalDateTime.of(2026, 2, 3, 12, 0),
                LocalDateTime.of(2026, 2, 3, 14, 0), "UTC_V2",
                new CancelacionSesionProjection("FESTIVO", "Dia festivo: San Jose", "San Jose", LocalDateTime.of(2026, 1, 2, 5, 0)))));

        final var sesion = interactor.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, null, null).elementos().getFirst();

        assertEquals("CANCELADA", sesion.estado());
        assertEquals("FESTIVO", sesion.cancelacion().tipo());
        assertEquals("San Jose", sesion.cancelacion().nombreFestivo());
        assertEquals(java.time.Instant.parse("2026-01-02T05:00:00Z"), sesion.cancelacion().fechaCancelacionUtc());
    }

    @Test
    void T_B1_02_sesion_sin_procedencia_queda_programada() {
        final ConsultarAsistenciaProgramaInteractor interactor = new ConsultarAsistenciaProgramaInteractor(useCase);
        when(queryPort.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, 1, 10))
                .thenReturn(new Pagina<>(List.of(sesion()), 1));
        when(procedenciaPort.consultarSesionesPorGrupo(GRUPO)).thenReturn(List.of());

        final var sesion = interactor.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, null, null).elementos().getFirst();

        assertEquals("PROGRAMADA", sesion.estado());
        assertNull(sesion.cancelacion());
    }

    @Test
    void interactor_rechaza_use_case_nulo() {
        assertThrows(NullPointerException.class, () -> new ConsultarAsistenciaProgramaInteractor(null));
    }

    private static GrupoProgramaProjection grupo() {
        return new GrupoProgramaProjection(GRUPO, 1, "Grupo 1", UUID.randomUUID(), "Calculo", UUID.randomUUID(), 30, 20,
                10, true, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 5));
    }

    private static SesionGrupoProjection sesion() {
        return sesion(SESION);
    }

    private static SesionProcedenciaRepositoryProjection procedencia(final UUID sesion, final UUID grupo,
                                                                      final LocalDateTime inicio,
                                                                      final LocalDateTime fin,
                                                                      final String procedenciaTemporal) {
        return new SesionProcedenciaRepositoryProjection(sesion, grupo, "Sesion 1", 1, "S-01", 2, "1", "Grupo 1",
                inicio, fin, procedenciaTemporal);
    }

    private static SesionGrupoProjection sesion(final UUID id) {
        return new SesionGrupoProjection(id, GRUPO, 1, "Grupo 1", "Sesion 1", 1, "S-01", 2,
                LocalDateTime.of(2026, 2, 3, 7, 0), LocalDateTime.of(2026, 2, 3, 9, 0));
    }

    private static AsistenciaSesionProjection asistencia() {
        return new AsistenciaSesionProjection(UUID.randomUUID(), SESION, "Sesion 1", UUID.randomUUID(), "1017000111",
                "Ana Ruiz", false, "INJ", "Inasistencia injustificada", LocalDateTime.of(2026, 2, 3, 7, 0),
                LocalDateTime.of(2026, 2, 3, 9, 0));
    }
}
