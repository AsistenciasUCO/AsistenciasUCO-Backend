package co.edu.uco.asistenciasuco.application.features.admin.consultarcambiosparametro.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcambiosparametro.usecase.ConsultarCambiosParametroUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort.PaginaCambios;

import java.time.ZoneOffset;
import java.util.Objects;

/** CULM-001 D.3: {@code usp_consultar_cambios_parametro_paginado}; pagina, perfil y existencia los valida la DB. */
public final class ConsultarCambiosParametroUseCaseImpl implements ConsultarCambiosParametroUseCase {

    private final ParametroAdminRepositoryPort repositorio;

    public ConsultarCambiosParametroUseCaseImpl(final ParametroAdminRepositoryPort repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "ParametroAdminRepositoryPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para consultar los cambios es obligatorio.");
        if (comando.idParametro() == null) {
            throw new ValidationException("El parametro es obligatorio.");
        }
        final int pagina = comando.pagina() == null ? PAGINA_POR_DEFECTO : comando.pagina();
        final int tamano = comando.tamano() == null ? TAMANO_POR_DEFECTO : comando.tamano();
        final PaginaCambios resultado = repositorio.consultarCambios(comando.usuarioEjecutor(), comando.idParametro(), pagina, tamano);
        return new Resultado(resultado.elementos().stream()
                .map(c -> new Cambio(c.id(), c.valorAnterior(), c.valorNuevo(), c.motivo(), c.idUsuarioEjecutor(),
                        c.nombreEjecutor(), c.idTraza(), c.fechaCambioUtc().toInstant(ZoneOffset.UTC)))
                .toList(), pagina, tamano, resultado.totalRegistros());
    }
}
