package co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.ActualizarDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.domain.ActualizarDocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper.DocenteProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.ActualizarDocenteProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaFiltro;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.ActualizarCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;
import java.util.UUID;

/**
 * HU092 (D11): edita el tipo de vinculacion del vinculo y los nombres de la persona. El programa sale del ambito del
 * coordinador; el SP revalida RBAC, titularidad y existencia del vinculo. Los nombres no se sincronizan con Keycloak
 * (el login no depende de ellos; deuda registrada hasta IAM-001). HU159 (2026-11-05): si se corrige el documento, el
 * nombre de usuario de la cuenta si se sincroniza (es el documento); un fallo queda como desfase pendiente.
 */
public final class ActualizarDocenteProgramaUseCaseImpl implements ActualizarDocenteProgramaUseCase {

    private final InstitutionalScopePort scopePort;
    private final DocenteProgramaCommandPort commandPort;
    private final DocenteProgramaQueryPort queryPort;
    private final SincronizarCuentaInstitucionalService sincronizacion;

    public ActualizarDocenteProgramaUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final DocenteProgramaCommandPort commandPort,
            final DocenteProgramaQueryPort queryPort,
            final SincronizarCuentaInstitucionalService sincronizacion
    ) {
        this.sincronizacion = Objects.requireNonNull(sincronizacion, "SincronizarCuentaInstitucionalService es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.commandPort = Objects.requireNonNull(commandPort, "DocenteProgramaCommandPort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "DocenteProgramaQueryPort es obligatorio.");
    }

    @Override
    public DocenteProgramaDomain execute(final ActualizarDocenteProgramaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para actualizar docente es obligatorio.");
        }
        final UUID idPrograma = scopePort.findProgramaIdByCoordinadorUsuario(domain.actorUsuarioId())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa del coordinador autenticado."));

        final CorreccionDocumentoDTO documento = new CorreccionDocumentoDTO(
                domain.idTipoIdentificacion(), domain.numeroIdentificacion(), domain.motivo());
        commandPort.actualizar(new ActualizarDocenteProgramaCommand(domain.idDocente(), idPrograma,
                domain.idTipoVinculacion(), domain.primerNombre(), domain.segundoNombre(), domain.primerApellido(),
                domain.segundoApellido(), domain.actorUsuarioId(), documento));

        final DocenteProgramaDomain actualizado = queryPort.consultar(domain.actorUsuarioId(),
                        new DocentesProgramaFiltro(idPrograma, 1, 1, null, null, null, domain.idDocente(), null))
                .elementos().stream().findFirst()
                .map(DocenteProgramaMapper::toDomain)
                .orElseThrow(() -> new InternalApplicationException("No fue posible leer el vinculo del docente despues de actualizarlo."));
        if (documento.solicitaCambio()) {
            sincronizacion.actualizarDatos(actualizado.idUsuario(), new ActualizarCuentaIdentidadDTO(actualizado.correo(),
                    actualizado.primerNombre(), actualizado.primerApellido(), documento.nuevoUsername()));
        }
        return actualizado;
    }
}
