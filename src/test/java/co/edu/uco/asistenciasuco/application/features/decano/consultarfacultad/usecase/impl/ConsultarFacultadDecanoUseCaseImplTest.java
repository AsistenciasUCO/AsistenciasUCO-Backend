package co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.impl;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CancelacionSesionProjection;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.interactor.ConsultarFacultadDecanoInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.domain.SesionFacultadHorarioDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.EstructuraFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.EstudianteFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.MatriculaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.SesionFacultadProjection;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU104/HU105/HU109/HU119/HU145: reglas de las consultas de solo lectura del decano. */
class ConsultarFacultadDecanoUseCaseImplTest {

    private static final UUID DECANO = UUID.randomUUID();
    private static final UUID FACULTAD = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    private final FacultadDecanoQueryPort queryPort = mock(FacultadDecanoQueryPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SesionProcedenciaQueryPort procedenciaPort = mock(SesionProcedenciaQueryPort.class);
    private ConsultarFacultadDecanoUseCaseImpl useCase;

    @BeforeEach
    void preparar() {
        useCase = new ConsultarFacultadDecanoUseCaseImpl(queryPort, scopePort, procedenciaPort);
        when(scopePort.findFacultadIdByDecanoUsuario(DECANO)).thenReturn(Optional.of(FACULTAD));
    }

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class,
                () -> new ConsultarFacultadDecanoUseCaseImpl(null, scopePort, procedenciaPort));
        assertThrows(NullPointerException.class,
                () -> new ConsultarFacultadDecanoUseCaseImpl(queryPort, null, procedenciaPort));
        assertThrows(NullPointerException.class, () -> new ConsultarFacultadDecanoUseCaseImpl(queryPort, scopePort, null));
    }

    @Test
    void T_104_01_estudiantes_aplica_valores_por_defecto_y_normaliza_busqueda() {
        final Pagina<EstudianteFacultadProjection> pagina = new Pagina<>(List.of(estudiante()), 1);
        when(queryPort.consultarEstudiantes(DECANO, "ana", PROGRAMA, true, 1, 10)).thenReturn(pagina);

        assertEquals(pagina, useCase.consultarEstudiantes(DECANO, "  ana ", PROGRAMA, true, null, null));
    }

    @Test
    void T_104_02_busqueda_en_blanco_se_envia_nula_y_busqueda_larga_es_400() {
        when(queryPort.consultarEstudiantes(DECANO, null, null, null, 2, 5)).thenReturn(new Pagina<>(List.of(), 0));

        useCase.consultarEstudiantes(DECANO, "  ", null, null, 2, 5);

        verify(queryPort).consultarEstudiantes(DECANO, null, null, null, 2, 5);
        assertThrows(ValidationException.class,
                () -> useCase.consultarEstudiantes(DECANO, "x".repeat(101), null, null, 1, 10));
    }

    @Test
    void T_104_03_usuario_sin_facultad_de_decano_es_403() {
        final UUID otro = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(otro)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.consultarEstudiantes(otro, null, null, null, 1, 10));
        assertThrows(ForbiddenException.class, () -> useCase.consultarEstructura(null));
        assertThrows(ForbiddenException.class, () -> useCase.consultarSesiones(otro, null, null, null, 1, 10));
        assertThrows(ForbiddenException.class, () -> useCase.consultarDetalleEstudiante(otro, ESTUDIANTE));
        verifyNoInteractions(queryPort);
    }

    @Test
    void T_104_04_paginacion_fuera_de_rango_es_400() {
        assertThrows(ValidationException.class, () -> useCase.consultarEstudiantes(DECANO, null, null, null, 0, 10));
        assertThrows(ValidationException.class, () -> useCase.consultarEstudiantes(DECANO, null, null, null, 1, 0));
        assertThrows(ValidationException.class, () -> useCase.consultarSesiones(DECANO, null, null, null, 1, 101));
        verifyNoInteractions(queryPort);
    }

    @Test
    void T_105_01_ficha_exige_estudiante_y_404_si_no_es_de_la_facultad() {
        assertThrows(ValidationException.class, () -> useCase.consultarDetalleEstudiante(DECANO, null));
        when(queryPort.consultarDetalleEstudiante(DECANO, ESTUDIANTE)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> useCase.consultarDetalleEstudiante(DECANO, ESTUDIANTE));
    }

    @Test
    void T_145_01_sesiones_valida_rango_y_envia_filtros() {
        final LocalDate desde = LocalDate.of(2026, 2, 1);
        final LocalDate hasta = LocalDate.of(2026, 2, 28);
        when(queryPort.consultarSesiones(DECANO, desde, hasta, PROGRAMA, 1, 100))
                .thenReturn(new Pagina<>(List.of(sesion()), 1));
        when(queryPort.consultarSesiones(DECANO, desde, null, null, 1, 10)).thenReturn(new Pagina<>(List.of(), 0));

        assertEquals(1, useCase.consultarSesiones(DECANO, desde, hasta, PROGRAMA, 1, 100).total());
        assertThrows(ValidationException.class, () -> useCase.consultarSesiones(DECANO, hasta, desde, null, 1, 10));
        useCase.consultarSesiones(DECANO, desde, null, null, null, null);
        verify(queryPort).consultarSesiones(DECANO, desde, null, null, 1, 10);
    }

    @Test
    void T_145_02_sesion_confirmada_expone_instantes_utc_y_la_indeterminada_no() {
        final SesionFacultadProjection confirmada = sesion();
        final SesionFacultadProjection antigua = sesion(confirmada.idGrupo());
        final SesionFacultadProjection otroGrupo = sesion();
        when(queryPort.consultarSesiones(DECANO, null, null, null, 1, 10))
                .thenReturn(new Pagina<>(List.of(confirmada, antigua, otroGrupo), 3));
        when(procedenciaPort.consultarSesionesPorGrupo(confirmada.idGrupo())).thenReturn(List.of(
                procedencia(confirmada, confirmada.idGrupo(), "UTC_V2"),
                procedencia(antigua, confirmada.idGrupo(), null)));
        // Fila de otro grupo con el mismo id: nunca se aplica (defensa en profundidad).
        when(procedenciaPort.consultarSesionesPorGrupo(otroGrupo.idGrupo()))
                .thenReturn(List.of(procedencia(otroGrupo, UUID.randomUUID(), "UTC_V2")));

        final var resultado = useCase.consultarSesiones(DECANO, null, null, null, null, null);

        assertEquals(3, resultado.total());
        final var primera = resultado.elementos().getFirst();
        assertEquals(EstadoHorarioSesion.CONFIRMADA, primera.estadoTemporal());
        assertEquals(LocalDateTime.of(2026, 2, 3, 12, 0), primera.fechaHoraInicioUtc());
        assertEquals(LocalDateTime.of(2026, 2, 3, 14, 0), primera.fechaHoraFinUtc());
        assertEquals(EstadoHorarioSesion.INDETERMINADA, resultado.elementos().get(1).estadoTemporal());
        assertNull(resultado.elementos().get(1).fechaHoraInicioUtc());
        assertEquals(EstadoHorarioSesion.INDETERMINADA, resultado.elementos().get(2).estadoTemporal());
        assertNull(resultado.elementos().get(2).fechaHoraFinUtc());
        verify(procedenciaPort, times(1)).consultarSesionesPorGrupo(confirmada.idGrupo());
    }

    @Test
    void T_145_03_pagina_vacia_no_consulta_procedencia() {
        when(queryPort.consultarSesiones(DECANO, null, null, null, 1, 10)).thenReturn(new Pagina<>(List.of(), 0));

        assertTrue(useCase.consultarSesiones(DECANO, null, null, null, null, null).elementos().isEmpty());
        verifyNoInteractions(procedenciaPort);
    }

    @Test
    void T_145_04_dominio_descarta_instantes_si_no_esta_confirmada() {
        final var dominio = dominio(UUID.randomUUID(), LocalDateTime.now(), EstadoHorarioSesion.INDETERMINADA);

        assertNull(dominio.fechaHoraInicioUtc());
        assertNull(dominio.fechaHoraFinUtc());
        assertThrows(NullPointerException.class,
                () -> dominio(null, null, EstadoHorarioSesion.INDETERMINADA));
        assertThrows(NullPointerException.class, () -> dominio(UUID.randomUUID(), null, null));
    }

    private static SesionFacultadHorarioDomain dominio(final UUID id, final LocalDateTime utc,
                                                       final EstadoHorarioSesion estado) {
        return new SesionFacultadHorarioDomain(id, "Sesion 1", "S-01", 2, LocalDateTime.of(2026, 2, 3, 12, 0),
                LocalDateTime.of(2026, 2, 3, 14, 0), UUID.randomUUID(), 1, "Grupo 1", UUID.randomUUID(), "Calculo",
                PROGRAMA, "Sistemas", utc, utc, estado);
    }

    @Test
    void interactor_mapea_estudiantes_ficha_y_sesiones() {
        final ConsultarFacultadDecanoInteractor interactor = new ConsultarFacultadDecanoInteractor(useCase);
        when(queryPort.consultarEstudiantes(DECANO, null, null, null, 1, 10))
                .thenReturn(new Pagina<>(List.of(estudiante()), 7));
        when(queryPort.consultarDetalleEstudiante(DECANO, ESTUDIANTE)).thenReturn(List.of(
                matricula(UUID.randomUUID(), false), matricula(UUID.randomUUID(), true)));
        when(queryPort.consultarSesiones(DECANO, null, null, null, 1, 10)).thenReturn(new Pagina<>(List.of(sesion()), 1));

        final var estudiantes = interactor.consultarEstudiantes(DECANO, null, null, null, null, null);
        assertEquals(7, estudiantes.total());
        assertEquals("Ana Ruiz", estudiantes.elementos().getFirst().nombreCompleto());

        final var ficha = interactor.consultarFichaEstudiante(DECANO, ESTUDIANTE);
        assertEquals(ESTUDIANTE, ficha.idEstudiante());
        assertTrue(ficha.activo());
        assertEquals(2, ficha.matriculas().size());

        final var sesiones = interactor.consultarSesiones(DECANO, null, null, null, null, null);
        assertEquals("S-01", sesiones.elementos().getFirst().codigo());
        assertEquals("Sistemas", sesiones.elementos().getFirst().nombrePrograma());
        assertEquals("INDETERMINADA", sesiones.elementos().getFirst().estadoTemporal());
        assertNull(sesiones.elementos().getFirst().fechaHoraInicioUtc());
    }

    @Test
    void interactor_arma_el_arbol_facultad_programa_plan() {
        final ConsultarFacultadDecanoInteractor interactor = new ConsultarFacultadDecanoInteractor(useCase);
        final UUID otroPrograma = UUID.randomUUID();
        final UUID plan1 = UUID.randomUUID();
        final UUID plan2 = UUID.randomUUID();
        when(queryPort.consultarEstructura(DECANO)).thenReturn(List.of(
                fila(PROGRAMA, "Sistemas", plan1, 20241),
                fila(PROGRAMA, "Sistemas", plan2, 20251),
                fila(otroPrograma, "Industrial", null, null)));

        final var arbol = interactor.consultarEstructura(DECANO);

        assertEquals(1, arbol.size());
        final var facultad = arbol.getFirst();
        assertEquals("Ingenieria", facultad.nombre());
        assertEquals(2, facultad.programas().size());
        assertEquals(2, facultad.programas().getFirst().planes().size());
        assertEquals(20251, facultad.programas().getFirst().planes().get(1).inp());
        assertTrue(facultad.programas().get(1).planes().isEmpty());
    }

    @Test
    void interactor_facultad_sin_programas_queda_con_lista_vacia() {
        final ConsultarFacultadDecanoInteractor interactor = new ConsultarFacultadDecanoInteractor(useCase);
        when(queryPort.consultarEstructura(DECANO)).thenReturn(List.of(fila(null, null, null, null)));

        final var arbol = interactor.consultarEstructura(DECANO);

        assertEquals(1, arbol.size());
        assertTrue(arbol.getFirst().programas().isEmpty());
        assertFalse(arbol.getFirst().nombreDecano().isBlank());
    }

    // CULM-001 B.1 (3.2.4): el cronograma de la facultad muestra la sesion cancelada con su cancelacion.
    @Test
    void T_B1_01_sesion_cancelada_expone_estado_y_cancelacion_hasta_el_dto() {
        final ConsultarFacultadDecanoInteractor interactor = new ConsultarFacultadDecanoInteractor(useCase);
        final SesionFacultadProjection cancelada = sesion();
        final SesionFacultadProjection programada = sesion();
        when(queryPort.consultarSesiones(DECANO, null, null, null, 1, 10))
                .thenReturn(new Pagina<>(List.of(cancelada, programada), 2));
        when(procedenciaPort.consultarSesionesPorGrupo(cancelada.idGrupo())).thenReturn(List.of(
                new SesionProcedenciaRepositoryProjection(cancelada.id(), cancelada.idGrupo(), cancelada.nombre(), 1,
                        cancelada.codigo(), cancelada.numeroSemana(), "1", cancelada.nombreGrupo(), cancelada.fechaHoraInicio(),
                        cancelada.fechaHoraFin(), "UTC_V2",
                        new CancelacionSesionProjection("DOCENTE", "Calamidad domestica", null, LocalDateTime.of(2026, 2, 1, 9, 0)))));

        final var sesiones = interactor.consultarSesiones(DECANO, null, null, null, null, null).elementos();

        assertEquals("CANCELADA", sesiones.getFirst().estado());
        assertEquals("DOCENTE", sesiones.getFirst().cancelacion().tipo());
        assertEquals("Calamidad domestica", sesiones.getFirst().cancelacion().motivo());
        assertEquals("PROGRAMADA", sesiones.get(1).estado());
        assertNull(sesiones.get(1).cancelacion());
    }

    @Test
    void interactor_rechaza_use_case_nulo() {
        assertThrows(NullPointerException.class, () -> new ConsultarFacultadDecanoInteractor(null));
    }

    private static EstudianteFacultadProjection estudiante() {
        return new EstudianteFacultadProjection(ESTUDIANTE, "1017000111", "Ana Ruiz", "ana@uco.edu.co", true,
                "Sistemas", 3);
    }

    private static MatriculaEstudianteProjection matricula(final UUID grupo, final boolean activo) {
        return new MatriculaEstudianteProjection(ESTUDIANTE, "1017000111", "Ana Ruiz", PROGRAMA, "Sistemas", 20241,
                UUID.randomUUID(), "Calculo", grupo, "Grupo 1", activo);
    }

    private static SesionFacultadProjection sesion() {
        return sesion(UUID.randomUUID());
    }

    private static SesionFacultadProjection sesion(final UUID grupo) {
        return new SesionFacultadProjection(UUID.randomUUID(), "Sesion 1", "S-01", 2,
                LocalDateTime.of(2026, 2, 3, 12, 0), LocalDateTime.of(2026, 2, 3, 14, 0), grupo, 1,
                "Grupo 1", UUID.randomUUID(), "Calculo", PROGRAMA, "Sistemas");
    }

    private static SesionProcedenciaRepositoryProjection procedencia(final SesionFacultadProjection s, final UUID grupo,
                                                                     final String procedenciaTemporal) {
        return new SesionProcedenciaRepositoryProjection(s.id(), grupo, s.nombre(), 1, s.codigo(), s.numeroSemana(),
                "1", s.nombreGrupo(), s.fechaHoraInicio(), s.fechaHoraFin(), procedenciaTemporal);
    }

    private static EstructuraFacultadProjection fila(final UUID programa, final String nombre, final UUID plan,
                                                     final Integer inp) {
        return new EstructuraFacultadProjection(FACULTAD, "Ingenieria", "Decana Uno", true, programa, nombre,
                "Coordinador", true, "Pregrado", "Presencial", "Diurna", plan, inp, true, 10);
    }
}
