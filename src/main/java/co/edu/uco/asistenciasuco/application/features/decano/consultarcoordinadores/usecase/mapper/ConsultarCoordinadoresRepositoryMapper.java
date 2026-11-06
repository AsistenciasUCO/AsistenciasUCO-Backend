package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain.CoordinadorDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenCoordinacionProjection;

public final class ConsultarCoordinadoresRepositoryMapper {

    private ConsultarCoordinadoresRepositoryMapper() {
    }

    public static CoordinadorDomain toDomain(final CoordinadorProjection projection) {
        return new CoordinadorDomain(projection.id(), projection.idUsuario(), projection.numeroIdentificacion(),
                projection.nombreCompleto(), projection.idPrograma(), projection.nombrePrograma(),
                projection.estaActivoCoordinador(), projection.primerNombre(), projection.segundoNombre(),
                projection.primerApellido(), projection.segundoApellido(), projection.correo(),
                projection.estaActivoRolCoordinador());
    }

    /** Certificacion F3: agrega la facultad y los conteos del programa que coordina (0 si no coordina ninguno). */
    public static CoordinadorDomain toDomain(final CoordinadorProjection projection, final String nombreFacultad,
                                             final ResumenCoordinacionProjection resumen) {
        return new CoordinadorDomain(projection.id(), projection.idUsuario(), projection.numeroIdentificacion(),
                projection.nombreCompleto(), projection.idPrograma(), projection.nombrePrograma(),
                projection.estaActivoCoordinador(), projection.primerNombre(), projection.segundoNombre(),
                projection.primerApellido(), projection.segundoApellido(), projection.correo(),
                projection.estaActivoRolCoordinador(), nombreFacultad,
                resumen == null ? 0 : resumen.totalDocentes(), resumen == null ? 0 : resumen.totalGrupos());
    }
}
