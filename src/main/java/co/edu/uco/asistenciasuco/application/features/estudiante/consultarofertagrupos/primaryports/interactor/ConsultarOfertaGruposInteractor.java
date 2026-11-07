package co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.ConsultarOfertaGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto.ConsultarOfertaGruposDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto.GrupoOfertaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto.OfertaGruposPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.ConsultarOfertaGruposUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.domain.ConsultarOfertaGruposDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.domain.OfertaGruposPaginaDomain;

import java.util.Objects;

public class ConsultarOfertaGruposInteractor implements ConsultarOfertaGruposInputPort {

    private final ConsultarOfertaGruposUseCase useCase;

    public ConsultarOfertaGruposInteractor(final ConsultarOfertaGruposUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public OfertaGruposPaginaDTO execute(final ConsultarOfertaGruposDTO dto) {
        final OfertaGruposPaginaDomain pagina = useCase.execute(dto == null ? null
                : new ConsultarOfertaGruposDomain(dto.actorUsuarioId(), dto.busqueda(), dto.pagina(), dto.tamano()));
        return new OfertaGruposPaginaDTO(pagina.grupos().stream()
                .map(g -> new GrupoOfertaDTO(g.idGrupo(), g.codigoGrupo(), g.nombreGrupo(), g.idAsignatura(), g.codigoAsignatura(),
                        g.nombreAsignatura(), g.nombreDocente(), g.idPeriodoAcademico(), g.nombrePeriodoAcademico(),
                        g.fechaInicioPeriodo(), g.fechaFinPeriodo(), g.estadoPeriodo(), g.horario(), g.capacidadMaxima(),
                        g.estudiantesActivos(), g.cuposDisponibles(), g.matriculado(),
                        g.tipoAsignatura(), g.transversal()))
                .toList(), pagina.total());
    }
}
