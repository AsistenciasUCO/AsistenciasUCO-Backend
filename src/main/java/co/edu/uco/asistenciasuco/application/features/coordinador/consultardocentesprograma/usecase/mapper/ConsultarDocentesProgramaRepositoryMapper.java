package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;

public final class ConsultarDocentesProgramaRepositoryMapper {

    private ConsultarDocentesProgramaRepositoryMapper() {
    }

    public static DocenteProgramaDomain toDomain(final DocenteProgramaProjection projection) {
        return new DocenteProgramaDomain(
                projection.id(),
                projection.idUsuario(),
                projection.numeroIdentificacion(),
                projection.nombreCompleto(),
                projection.correo(),
                projection.idPrograma(),
                projection.nombrePrograma(),
                projection.totalGruposAsignados(),
                projection.estaActivoDocente()
        );
    }
}
