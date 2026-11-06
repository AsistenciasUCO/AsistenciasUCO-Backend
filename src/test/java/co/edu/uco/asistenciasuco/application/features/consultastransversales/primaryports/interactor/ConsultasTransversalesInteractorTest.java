package co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.ConsultasTransversalesUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.AsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.DocenteFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.EstudianteInstitucionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsulta;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.HistorialEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ProgramaCreditosProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ResumenAsistenciaProjection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** HU112/HU128/HU143/HU157: mapeo de proyecciones a DTO y armado de la ficha unificada. */
class ConsultasTransversalesInteractorTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();

    private final ConsultasTransversalesUseCase useCase = mock(ConsultasTransversalesUseCase.class);
    private final ConsultasTransversalesInteractor interactor = new ConsultasTransversalesInteractor(useCase);

    @Test
    void constructor_rechaza_use_case_nulo() {
        assertThrows(NullPointerException.class, () -> new ConsultasTransversalesInteractor(null));
    }

    @Test
    void T_112_INT_01_mapea_programas_y_creditos() {
        when(useCase.consultarProgramasCreditos(USUARIO, "sis", true, 1, 10)).thenReturn(new Pagina<>(List.of(
                new ProgramaCreditosProjection(ID, "Sistemas", "Pregrado", "Presencial", "Diurna", true, ID, 2024, true,
                        10, 52, 160)), 3));

        final var pagina = interactor.consultarProgramasCreditos(USUARIO, "sis", true, 1, 10);

        assertEquals(3, pagina.total());
        assertEquals(160, pagina.elementos().getFirst().totalCreditos());
        assertEquals(2024, pagina.elementos().getFirst().inpPlanEstudio());
    }

    @Test
    void T_128_INT_01_mapea_docentes_con_el_programa_como_filtro() {
        when(useCase.consultarDocentes(USUARIO, new FiltroConsulta("ana", ID, null), 2, 5)).thenReturn(new Pagina<>(
                List.of(new DocenteFacultadProjection(ID, "101", "Ana", "ana@uco.edu.co", true, "Sistemas", 1, 4)), 1));

        final var docente = interactor.consultarDocentes(USUARIO, "ana", ID, null, 2, 5).elementos().getFirst();

        assertEquals("Sistemas", docente.programas());
        assertEquals(4, docente.totalGrupos());
    }

    @Test
    void T_143_INT_01_mapea_asignaturas_con_la_facultad_como_filtro() {
        when(useCase.buscarAsignaturas(USUARIO, new FiltroConsulta("CAL", ID, true), null, null)).thenReturn(
                new Pagina<>(List.of(new AsignaturaProjection(ID, "CAL1", "Calculo", 3, "Ciencias", "Basico", "S1", ID,
                        2024, ID, "Sistemas", ID, "Ingenieria", true)), 1));

        final var asignatura = interactor.buscarAsignaturas(USUARIO, "CAL", ID, true, null, null).elementos()
                .getFirst();

        assertEquals("CAL1", asignatura.codigo());
        assertEquals("Ingenieria", asignatura.nombreFacultad());
        assertTrue(asignatura.activa());
    }

    @Test
    void T_157_INT_01_mapea_estudiantes_de_la_institucion() {
        when(useCase.buscarEstudiantes(USUARIO, new FiltroConsulta(null, null, null), 1, 10)).thenReturn(new Pagina<>(
                List.of(new EstudianteInstitucionProjection(ID, "101", "Ana", null, true, "Ingenieria", "Sistemas", 2)),
                1));

        final var estudiante = interactor.buscarEstudiantes(USUARIO, null, null, null, 1, 10).elementos().getFirst();

        assertEquals("Ingenieria", estudiante.facultades());
        assertEquals(2, estudiante.totalGrupos());
    }

    @Test
    void T_157_INT_02_ficha_toma_los_datos_de_la_primera_fila_y_una_matricula_por_fila() {
        final UUID grupo = UUID.randomUUID();
        when(useCase.consultarHistorial(USUARIO, ID)).thenReturn(List.of(
                fila(grupo, new ResumenAsistenciaProjection(10, 8, 2, 0, new BigDecimal("20.00"), "RIESGO")),
                fila(null, null)));

        final var ficha = interactor.consultarHistorial(USUARIO, ID);

        assertEquals("Ana Ruiz", ficha.nombreCompleto());
        assertEquals(2, ficha.matriculas().size());
        assertEquals(grupo, ficha.matriculas().getFirst().idGrupo());
        assertEquals(new BigDecimal("20.00"), ficha.matriculas().getFirst().resumen().porcentajeInasistencia());
        assertNull(ficha.matriculas().get(1).resumen());
    }

    private static HistorialEstudianteProjection fila(final UUID grupo, final ResumenAsistenciaProjection resumen) {
        return new HistorialEstudianteProjection(ID, "CC", "101", "Ana Ruiz", "ana@uco.edu.co", true, ID,
                "Ingenieria", ID, "Sistemas", 2024, ID, "2026-1", "Primer semestre 2026", grupo, 1, ID, "Calculo",
                "MAT", "Matriculado", resumen);
    }

    @Test
    void T_136_INT_01_mapea_docentes_de_la_institucion_con_facultad_y_programa() {
        when(useCase.consultarDocentesInstitucion(USUARIO, new FiltroConsulta("ana", ID, true), ID, "TODOS", 1, 20)).thenReturn(new Pagina<>(
                List.of(new DocenteFacultadProjection(ID, "101", "Ana", "ana@uco.edu.co", true, "Sistemas, Civil", 2, 5),
                        new DocenteFacultadProjection(ID, "102", "Luis", null, true, null, 0, 0, false)), 2));

        final var pagina = interactor.consultarDocentesInstitucion(USUARIO, "ana", ID, ID, true, "TODOS", 1, 20);
        org.junit.jupiter.api.Assertions.assertFalse(pagina.elementos().get(1).tieneVinculoActivo());
        org.junit.jupiter.api.Assertions.assertTrue(pagina.elementos().getFirst().tieneVinculoActivo());

        assertEquals(2, pagina.total());
        assertEquals("Sistemas, Civil", pagina.elementos().getFirst().programas());
        assertEquals(2, pagina.elementos().getFirst().totalProgramas());
    }
}
