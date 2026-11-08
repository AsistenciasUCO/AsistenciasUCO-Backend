package co.edu.uco.asistenciasuco.application.features.admin.actualizarparametro.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarparametro.usecase.ActualizarParametroUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.exception.ParametroErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParametroEdicionCapacidadPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort.ParametroActualizado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * CULM-001 D.3 (HU168): forma del comando -> capacidad del proveedor de parametros ({@code PRM_010}, [SUPUESTO-D-06]) ->
 * {@code usp_actualizar_parametro} (lista blanca, tipo, rango, reglas cruzadas, motivo y version los valida la DB) -> si
 * el valor cambio, invalida el parametro en la cache. Un fallo al invalidar se registra y no revierte el cambio.
 * El evento {@code PARAMETER_UPDATED} se publica cuando C este mezclado (diferible, CONTRACT v2 §1.2).
 */
public final class ActualizarParametroUseCaseImpl implements ActualizarParametroUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActualizarParametroUseCaseImpl.class);

    private final ParametroAdminRepositoryPort repositorio;
    private final ParametroEdicionCapacidadPort capacidad;
    private final CatalogInvalidationPort invalidacion;

    public ActualizarParametroUseCaseImpl(final ParametroAdminRepositoryPort repositorio,
                                          final ParametroEdicionCapacidadPort capacidad,
                                          final CatalogInvalidationPort invalidacion) {
        this.repositorio = Objects.requireNonNull(repositorio, "ParametroAdminRepositoryPort es obligatorio.");
        this.capacidad = Objects.requireNonNull(capacidad, "ParametroEdicionCapacidadPort es obligatorio.");
        this.invalidacion = Objects.requireNonNull(invalidacion, "CatalogInvalidationPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para actualizar el parametro es obligatorio.");
        if (comando.idParametro() == null || comando.valor() == null || comando.fechaModificacionEsperada() == null
                || comando.motivo() == null || comando.usuarioEjecutor() == null) {
            throw new ValidationException("Los datos para actualizar el parametro no son validos.");
        }
        if (!capacidad.admiteEdicion()) {
            throw new ConflictException(ParametroErrorCode.PRM_010);
        }
        final LocalDateTime esperada = LocalDateTime.ofInstant(comando.fechaModificacionEsperada(), ZoneOffset.UTC);
        final ParametroActualizado actualizado = repositorio.actualizar(comando.idParametro(), comando.valor(), esperada,
                comando.motivo(), comando.usuarioEjecutor());
        final boolean sinCambios = Objects.equals(actualizado.valorAnterior(), actualizado.valorNuevo());
        if (!sinCambios) {
            invalidar(actualizado);
        }
        return new Resultado(actualizado.id(), actualizado.grupo(), actualizado.clave(), actualizado.valorAnterior(),
                actualizado.valorNuevo(), actualizado.fechaModificacion().toInstant(ZoneOffset.UTC), sinCambios);
    }

    private void invalidar(final ParametroActualizado actualizado) {
        try {
            invalidacion.invalidateParameter(actualizado.grupo(), actualizado.clave());
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible invalidar la cache del parametro {}/{}: {}", actualizado.grupo(), actualizado.clave(),
                    exception.getClass().getSimpleName());
        }
    }
}
