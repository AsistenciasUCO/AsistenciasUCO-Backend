package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.domain.SesionGrupoHorarioDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.AsistenciaSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.GrupoProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.Pagina;

import java.time.LocalDate;
import java.util.UUID;

public interface ConsultarAsistenciaProgramaUseCase {

    Pagina<GrupoProgramaProjection> consultarGrupos(UUID usuario, UUID idPeriodoAcademico, String busqueda,
                                                     Integer pagina, Integer tamano);

    Pagina<SesionGrupoHorarioDomain> consultarSesionesGrupo(UUID usuario, UUID idGrupo, LocalDate desde,
                                                             LocalDate hasta, Integer pagina, Integer tamano);

    Pagina<AsistenciaSesionProjection> consultarAsistenciasSesion(UUID usuario, UUID idSesion, Boolean asistio,
                                                                   Integer pagina, Integer tamano);
}
