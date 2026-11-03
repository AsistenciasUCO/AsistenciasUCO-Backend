package co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsulta;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.HistorialEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.Pagina;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU112/HU128/HU143/HU157: validacion de entrada; el ambito y el perfil los decide el provider DB. */
class ConsultasTransversalesUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID PADRE = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    private final ConsultasTransversalesQueryPort port = mock(ConsultasTransversalesQueryPort.class);
    private final ConsultasTransversalesUseCaseImpl useCase = new ConsultasTransversalesUseCaseImpl(port);

    @Test
    void constructor_rechaza_port_nulo() {
        assertThrows(NullPointerException.class, () -> new ConsultasTransversalesUseCaseImpl(null));
    }

    @Test
    void T_112_UC_01_programas_creditos_normaliza_busqueda_y_paginacion_por_defecto() {
        final Pagina<ConsultasTransversalesQueryPort.ProgramaCreditosProjection> pagina = new Pagina<>(List.of(), 0);
        when(port.consultarProgramasCreditos(USUARIO, null, true, 1, 10)).thenReturn(pagina);

        assertSame(pagina, useCase.consultarProgramasCreditos(USUARIO, "   ", true, null, null));
    }

    @Test
    void T_128_UC_01_docentes_recorta_busqueda_y_conserva_programa() {
        when(port.consultarDocentes(any(), any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0));

        useCase.consultarDocentes(USUARIO, new FiltroConsulta("  ana ", PADRE, false), 2, 20);

        verify(port).consultarDocentes(USUARIO, new FiltroConsulta("ana", PADRE, false), 2, 20);
    }

    @Test
    void T_143_UC_01_asignaturas_sin_filtro_usa_filtro_vacio() {
        when(port.buscarAsignaturas(any(), any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0));

        useCase.buscarAsignaturas(USUARIO, null, 3, 5);

        verify(port).buscarAsignaturas(USUARIO, new FiltroConsulta(null, null, null), 3, 5);
    }

    @Test
    void T_157_UC_01_estudiantes_delega_con_facultad() {
        when(port.buscarEstudiantes(any(), any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 7));

        assertEquals(7, useCase.buscarEstudiantes(USUARIO, new FiltroConsulta("101", PADRE, null), 1, 100).total());
        verify(port).buscarEstudiantes(USUARIO, new FiltroConsulta("101", PADRE, null), 1, 100);
    }

    @Test
    void T_PAG_UC_01_paginacion_y_busqueda_invalidas_son_400_sin_consultar() {
        assertThrows(ValidationException.class, () -> useCase.consultarProgramasCreditos(USUARIO, null, null, 0, 10));
        assertThrows(ValidationException.class, () -> useCase.consultarDocentes(USUARIO, null, 1, 101));
        assertThrows(ValidationException.class, () -> useCase.buscarAsignaturas(USUARIO,
                new FiltroConsulta("x".repeat(101), null, null), 1, 10));
        verifyNoInteractions(port);
    }

    @Test
    void T_SEC_UC_01_sin_usuario_autenticado_es_403() {
        assertThrows(ForbiddenException.class, () -> useCase.buscarEstudiantes(null, null, null, null));
        verifyNoInteractions(port);
    }

    @Test
    void T_157_UC_02_historial_exige_estudiante_y_404_si_no_esta_en_el_ambito() {
        assertThrows(ValidationException.class, () -> useCase.consultarHistorial(USUARIO, null));
        when(port.consultarHistorial(USUARIO, ESTUDIANTE)).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> useCase.consultarHistorial(USUARIO, ESTUDIANTE));
    }

    @Test
    void T_157_UC_03_historial_devuelve_las_filas_del_provider() {
        final List<HistorialEstudianteProjection> filas = List.of(new HistorialEstudianteProjection(ESTUDIANTE, "CC",
                "101", "Ana", null, true, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null));
        when(port.consultarHistorial(USUARIO, ESTUDIANTE)).thenReturn(filas);

        assertSame(filas, useCase.consultarHistorial(USUARIO, ESTUDIANTE));
    }
}
