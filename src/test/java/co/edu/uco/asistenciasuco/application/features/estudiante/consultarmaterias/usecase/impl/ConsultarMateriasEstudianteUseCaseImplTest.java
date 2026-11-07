package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.mapper.ConsultarMateriasEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.domain.MateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.MateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarMateriasEstudianteUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final MateriaEstudianteQueryPort queryPort = mock(MateriaEstudianteQueryPort.class);
    private final ResumenAsistenciaQueryPort resumenPort = mock(ResumenAsistenciaQueryPort.class);
    private final ConsultarMateriasEstudianteUseCaseImpl useCase =
            new ConsultarMateriasEstudianteUseCaseImpl(scopePort, queryPort, resumenPort);

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_estudiante() {
        final UUID actor = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(actor));
    }

    @Test
    void execute_consulta_materias_del_estudiante_resuelto() {
        final UUID actor = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(estudiante));
        // CULM-001 A.2-11: el SP recibe el USUARIO ejecutor y resuelve el estudiante (solo matricula A).
        when(queryPort.consultarMateriasEstudiante(actor)).thenReturn(List.of(
                new MateriaEstudianteProjection(asignatura, "Calculo", UUID.randomUUID(), "Grupo 1")));

        final List<MateriaEstudianteDomain> result = useCase.execute(actor);

        assertEquals(1, result.size());
        assertEquals(asignatura, result.getFirst().idAsignatura());
    }

    @Test
    void B2_cada_materia_lleva_sus_indicadores_por_horas_y_sin_fila_recibe_ceros_con_umbrales() {
        final UUID actor = UUID.randomUUID();
        final UUID grupoConDatos = UUID.randomUUID();
        final UUID grupoSinSesiones = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(UUID.randomUUID()));
        when(queryPort.consultarMateriasEstudiante(actor)).thenReturn(List.of(
                new MateriaEstudianteProjection(UUID.randomUUID(), "Calculo", grupoConDatos, "Grupo 1"),
                new MateriaEstudianteProjection(UUID.randomUUID(), "Fisica", grupoSinSesiones, "Grupo 2")));
        final IndicadoresAsistenciaDTO indicadores = new IndicadoresAsistenciaDTO(10, new BigDecimal("20.00"),
                new BigDecimal("16.00"), new BigDecimal("3.00"), new BigDecimal("1.00"), BigDecimal.ZERO,
                new BigDecimal("15.00"), new BigDecimal("80.00"), "RIESGO", new BigDecimal("15.00"), new BigDecimal("20.00"));
        when(resumenPort.consultarResumenEstudiante(actor)).thenReturn(List.of(
                new ResumenAsistenciaQueryPort.FilaResumen(grupoConDatos, UUID.randomUUID(), "Ana", "A", indicadores)));

        final List<MateriaEstudianteDomain> result = useCase.execute(actor);

        assertEquals(indicadores, result.get(0).indicadores());
        assertEquals(0, result.get(1).indicadores().sesionesEjecutadas());
        assertNull(result.get(1).indicadores().codigoPermanencia());
        assertEquals(new BigDecimal("20.00"), result.get(1).indicadores().umbralPerdida());
        final MateriaEstudianteDTO dto = ConsultarMateriasEstudianteMapper.toDTO(result.get(0));
        assertEquals(indicadores, dto.indicadores());
    }

    @Test
    void B2_sin_resumen_las_materias_tienen_ceros_sin_umbrales_y_sin_materias_no_consulta_resumen() {
        final UUID actor = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(UUID.randomUUID()));
        when(queryPort.consultarMateriasEstudiante(actor)).thenReturn(List.of());
        assertEquals(List.of(), useCase.execute(actor));
        verify(resumenPort, never()).consultarResumenEstudiante(any());

        when(queryPort.consultarMateriasEstudiante(actor)).thenReturn(List.of(
                new MateriaEstudianteProjection(UUID.randomUUID(), "Calculo", UUID.randomUUID(), "Grupo 1")));
        when(resumenPort.consultarResumenEstudiante(actor)).thenReturn(List.of());
        final MateriaEstudianteDomain materia = useCase.execute(actor).getFirst();
        assertEquals(0, materia.indicadores().sesionesEjecutadas());
        assertNull(materia.indicadores().umbralAdvertencia());
        assertEquals(0, new MateriaEstudianteDTO(UUID.randomUUID(), "X", UUID.randomUUID(), "G").indicadores().sesionesEjecutadas());
    }
}
