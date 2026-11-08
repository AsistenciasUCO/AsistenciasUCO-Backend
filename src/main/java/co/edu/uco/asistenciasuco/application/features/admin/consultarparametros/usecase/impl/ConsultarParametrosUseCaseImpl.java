package co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.ConsultarParametrosUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort.PaginaParametros;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort.ParametroAdmin;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * CULM-001 D.3: {@code usp_consultar_parametros_paginado}. Aqui solo se completan los valores por defecto; los limites de
 * pagina ({@code PAG_001}), el perfil y la institucion unica los valida la DB.
 */
public final class ConsultarParametrosUseCaseImpl implements ConsultarParametrosUseCase {

    private final ParametroAdminRepositoryPort repositorio;

    public ConsultarParametrosUseCaseImpl(final ParametroAdminRepositoryPort repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "ParametroAdminRepositoryPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para consultar parametros es obligatorio.");
        final int pagina = comando.pagina() == null ? PAGINA_POR_DEFECTO : comando.pagina();
        final int tamano = comando.tamano() == null ? TAMANO_POR_DEFECTO : comando.tamano();
        final boolean soloEditables = comando.soloEditables() == null || comando.soloEditables();
        final PaginaParametros resultado = repositorio.consultar(comando.usuarioEjecutor(), pagina, tamano, comando.grupo(),
                comando.texto(), soloEditables);
        return new Resultado(resultado.elementos().stream().map(ConsultarParametrosUseCaseImpl::mapear).toList(),
                pagina, tamano, resultado.totalRegistros());
    }

    private static Parametro mapear(final ParametroAdmin p) {
        return new Parametro(p.id(), p.grupo(), p.clave(), p.valor(), p.valorDefecto(), p.tipoDato(), p.esEditable(),
                p.valorMinimo(), p.valorMaximo(), p.valoresPermitidos(), p.etiqueta(), p.descripcion(), p.unidad(),
                p.categoriaVisual(), p.orden(), instante(p.fechaModificacion()), instante(p.fechaUltimoCambioUtc()),
                p.nombreUltimoActor());
    }

    private static Instant instante(final LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }
}
