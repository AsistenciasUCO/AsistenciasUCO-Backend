package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.ConsultarMateriasEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.domain.MateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.mapper.ConsultarMateriasEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.MateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Materias del estudiante autenticado. CULM-001 B.2 (F-EST-002, contrato 3.2.8): une por {@code idGrupo} la consulta de
 * materias con {@code usp_consultar_resumen_asistencia_estudiante} (una llamada); una materia sin fila de resumen recibe
 * indicadores en cero (nunca nulos) con los umbrales conocidos.
 */
public final class ConsultarMateriasEstudianteUseCaseImpl implements ConsultarMateriasEstudianteUseCase {

    private final InstitutionalScopePort scopePort;
    private final MateriaEstudianteQueryPort queryPort;
    private final ResumenAsistenciaQueryPort resumenPort;

    public ConsultarMateriasEstudianteUseCaseImpl(final InstitutionalScopePort scopePort, final MateriaEstudianteQueryPort queryPort,
                                                  final ResumenAsistenciaQueryPort resumenPort) {
        this.scopePort = Objects.requireNonNull(scopePort);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.resumenPort = Objects.requireNonNull(resumenPort);
    }

    @Override
    public List<MateriaEstudianteDomain> execute(final UUID actorUsuarioId) {
        // CULM-001 A.2-11: sin estudiante, 403; el SP recibe el usuario y solo devuelve matriculas inscritas (A).
        scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));
        final List<MateriaEstudianteDomain> materias = queryPort.consultarMateriasEstudiante(actorUsuarioId).stream()
                .map(ConsultarMateriasEstudianteRepositoryMapper::toDomain)
                .toList();
        if (materias.isEmpty()) {
            return materias;
        }
        final Map<UUID, IndicadoresAsistenciaDTO> porGrupo = new HashMap<>();
        IndicadoresAsistenciaDTO referencia = null;
        for (final ResumenAsistenciaQueryPort.FilaResumen fila : resumenPort.consultarResumenEstudiante(actorUsuarioId)) {
            porGrupo.put(fila.idGrupo(), fila.indicadores());
            referencia = fila.indicadores();
        }
        final IndicadoresAsistenciaDTO vacio = referencia == null
                ? IndicadoresAsistenciaDTO.sinSesiones(null, null)
                : IndicadoresAsistenciaDTO.sinSesiones(referencia.umbralAdvertencia(), referencia.umbralPerdida());
        return materias.stream()
                .map(materia -> new MateriaEstudianteDomain(materia.idAsignatura(), materia.nombreAsignatura(),
                        materia.idGrupo(), materia.nombreGrupo(), porGrupo.getOrDefault(materia.idGrupo(), vacio)))
                .toList();
    }
}
