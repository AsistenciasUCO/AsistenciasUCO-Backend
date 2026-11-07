package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.domain;






import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.exception.TipoIdentificacionErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.util.UUID;

public record ConsultarEstudiantesDomain(
        UUID tipoIdentificacionId,
        Integer numeroIdentificacion,
        String nombre,
        String correo,
        UUID institucionId,
        UUID facultadId,
        UUID programaId,
        UUID grupoId,
        Boolean activo,
        int page,
        int size,
        UUID actorUsuarioId
) {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    public static ConsultarEstudiantesDomain crear(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion,
            final String nombre,
            final String correo,
            final UUID institucionId,
            final UUID facultadId,
            final UUID programaId,
            final UUID grupoId,
            final Boolean activo,
            final Integer page,
            final Integer size,
            final UUID actorUsuarioId
    ) {
        final int pageValue = page == null ? DEFAULT_PAGE : page;
        final int sizeValue = size == null ? DEFAULT_SIZE : size;
        if (pageValue < 0) {
            throw new ValidationException(CommonErrorCode.ERR_PAGE_INVALIDA);
        }
        if (sizeValue < MIN_SIZE || sizeValue > MAX_SIZE) {
            throw new ValidationException(CommonErrorCode.ERR_SIZE_INVALIDO);
        }

        return new ConsultarEstudiantesDomain(
                validarUuidOpcional(tipoIdentificacionId, TipoIdentificacionErrorCode.ERR_TIPO_IDENTIFICACION_INVALIDA),
                numeroIdentificacion,
                TextHelper.trim(nombre),
                TextHelper.normalizeTrimLower(correo),
                validarUuidOpcional(institucionId, UsuarioErrorCode.ERR_INSTITUCION_INVALIDA),
                validarUuidOpcional(facultadId, UsuarioErrorCode.ERR_FACULTAD_INVALIDA),
                validarUuidOpcional(programaId, UsuarioErrorCode.ERR_PROGRAMA_INVALIDO),
                validarUuidOpcional(grupoId, GrupoErrorCode.ERR_GRUPO_INVALIDO),
                activo,
                pageValue,
                sizeValue,
                actorUsuarioId
        );
    }

    private static UUID validarUuidOpcional(final UUID value, final ErrorDefinition code) {
        if (EMPTY_UUID.equals(value)) {
            throw new ValidationException(code);
        }
        return value;
    }
}
