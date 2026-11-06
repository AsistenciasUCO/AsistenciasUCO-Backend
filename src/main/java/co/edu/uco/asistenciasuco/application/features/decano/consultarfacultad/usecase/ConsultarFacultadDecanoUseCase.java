package co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.domain.SesionFacultadHorarioDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.EstructuraFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.EstudianteFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.MatriculaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.Pagina;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ConsultarFacultadDecanoUseCase {

    Pagina<EstudianteFacultadProjection> consultarEstudiantes(UUID usuario, String busqueda, UUID idPrograma,
                                                              Boolean estaActivo, Integer pagina, Integer tamano);

    List<MatriculaEstudianteProjection> consultarDetalleEstudiante(UUID usuario, UUID idEstudiante);

    List<EstructuraFacultadProjection> consultarEstructura(UUID usuario);

    Pagina<SesionFacultadHorarioDomain> consultarSesiones(UUID usuario, LocalDate desde, LocalDate hasta,
                                                          UUID idPrograma, Integer pagina, Integer tamano);
}
