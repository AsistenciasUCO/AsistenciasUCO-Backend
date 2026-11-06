package co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.ConsultarFacultadDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.EstudianteFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.FacultadNodoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.FichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.MatriculaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.PlanEstudioNodoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.ProgramaNodoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.SesionFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.ConsultarFacultadDecanoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.EstructuraFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.MatriculaEstudianteProjection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarFacultadDecanoInteractor implements ConsultarFacultadDecanoInputPort {

    private final ConsultarFacultadDecanoUseCase useCase;

    public ConsultarFacultadDecanoInteractor(final ConsultarFacultadDecanoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ConsultarFacultadDecanoUseCase es obligatorio.");
    }

    @Override
    public PaginaDTO<EstudianteFacultadDTO> consultarEstudiantes(final UUID usuario, final String busqueda,
                                                                 final UUID idPrograma, final Boolean estaActivo,
                                                                 final Integer pagina, final Integer tamano) {
        final var resultado = useCase.consultarEstudiantes(usuario, busqueda, idPrograma, estaActivo, pagina, tamano);
        return new PaginaDTO<>(resultado.elementos().stream()
                .map(e -> new EstudianteFacultadDTO(e.idEstudiante(), e.numeroIdentificacion(), e.nombreCompleto(),
                        e.correo(), e.activo(), e.programas(), e.totalGrupos()))
                .toList(), resultado.total());
    }

    @Override
    public FichaEstudianteDTO consultarFichaEstudiante(final UUID usuario, final UUID idEstudiante) {
        final List<MatriculaEstudianteProjection> matriculas = useCase.consultarDetalleEstudiante(usuario, idEstudiante);
        final MatriculaEstudianteProjection primera = matriculas.getFirst();
        return new FichaEstudianteDTO(primera.idEstudiante(), primera.numeroIdentificacion(), primera.nombreCompleto(),
                matriculas.stream().anyMatch(MatriculaEstudianteProjection::activo),
                matriculas.stream().map(m -> new MatriculaEstudianteDTO(m.idPrograma(), m.nombrePrograma(),
                        m.inpPlanEstudio(), m.idAsignatura(), m.nombreAsignatura(), m.idGrupo(), m.nombreGrupo()))
                        .toList());
    }

    /** Arma el arbol Facultad -> Programa -> Plan a partir de las filas planas (orden del provider). */
    @Override
    public List<FacultadNodoDTO> consultarEstructura(final UUID usuario) {
        final Map<UUID, EstructuraFacultadProjection> facultades = new LinkedHashMap<>();
        final Map<UUID, Map<UUID, EstructuraFacultadProjection>> programasPorFacultad = new LinkedHashMap<>();
        final Map<UUID, List<PlanEstudioNodoDTO>> planesPorPrograma = new LinkedHashMap<>();
        for (final EstructuraFacultadProjection fila : useCase.consultarEstructura(usuario)) {
            facultades.putIfAbsent(fila.idFacultad(), fila);
            final var programas = programasPorFacultad.computeIfAbsent(fila.idFacultad(), k -> new LinkedHashMap<>());
            if (fila.idPrograma() == null) {
                continue;
            }
            programas.putIfAbsent(fila.idPrograma(), fila);
            final var planes = planesPorPrograma.computeIfAbsent(fila.idPrograma(), k -> new ArrayList<>());
            if (fila.idPlanEstudio() != null) {
                planes.add(new PlanEstudioNodoDTO(fila.idPlanEstudio(), fila.inpPlanEstudio(), fila.planActivo(),
                        fila.totalSemestres()));
            }
        }
        return facultades.values().stream()
                .map(f -> new FacultadNodoDTO(f.idFacultad(), f.nombreFacultad(), f.nombreDecano(), f.facultadActiva(),
                        programasPorFacultad.get(f.idFacultad()).values().stream()
                                .map(p -> new ProgramaNodoDTO(p.idPrograma(), p.nombrePrograma(), p.nombreCoordinador(),
                                        p.programaActivo(), p.nombreTipoPrograma(), p.nombreModalidad(),
                                        p.nombreJornada(), List.copyOf(planesPorPrograma.get(p.idPrograma()))))
                                .toList()))
                .toList();
    }

    @Override
    public PaginaDTO<SesionFacultadDTO> consultarSesiones(final UUID usuario, final LocalDate desde,
                                                          final LocalDate hasta, final UUID idPrograma,
                                                          final Integer pagina, final Integer tamano) {
        final var resultado = useCase.consultarSesiones(usuario, desde, hasta, idPrograma, pagina, tamano);
        return new PaginaDTO<>(resultado.elementos().stream()
                .map(s -> new SesionFacultadDTO(s.id(), s.nombre(), s.codigo(), s.numeroSemana(),
                        s.fechaHoraInicio(), s.fechaHoraFin(), s.idGrupo(), s.codigoGrupo(), s.nombreGrupo(),
                        s.idAsignatura(), s.nombreAsignatura(), s.idPrograma(), s.nombrePrograma(),
                        s.fechaHoraInicioUtc(), s.fechaHoraFinUtc(), s.estadoTemporal().name()))
                .toList(), resultado.total());
    }
}
