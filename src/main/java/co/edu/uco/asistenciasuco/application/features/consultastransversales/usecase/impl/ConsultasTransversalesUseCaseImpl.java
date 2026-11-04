package co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.ConsultasTransversalesUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.common.rule.ConsultaPaginadaRules;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.AsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ComponenteFormacionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ConsolidadoGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.EstadoSistemaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.EventoAuditoriaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroAuditoria;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsolidado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.DocenteFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.EstudianteInstitucionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsulta;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.HistorialEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ProgramaCreditosProjection;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * HU112/HU128/HU143/HU157. Valida la entrada (paginacion, busqueda, estudiante obligatorio) y delega en el provider
 * DB, que exige el perfil (SEC_001 -> 403) y limita las filas al ambito del ejecutor: la facultad del decano o la
 * institucion del administrador. Un historial vacio significa que el estudiante no existe en ese ambito (404).
 * HU061 (consolidado), HU172 (auditoria, solo administrador) y HU147 (componentes) siguen el mismo esquema.
 */
public final class ConsultasTransversalesUseCaseImpl implements ConsultasTransversalesUseCase {

    static final Duration RANGO_AUDITORIA_POR_DEFECTO = Duration.ofDays(7);
    static final Duration RANGO_AUDITORIA_MAXIMO = Duration.ofDays(90);
    static final int LONGITUD_MAXIMA_FILTRO_AUDITORIA = 120;
    private static final List<String> RESULTADOS_AUDITORIA = List.of("SUCCESS", "FAILURE");

    private final ConsultasTransversalesQueryPort queryPort;
    private final Clock clock;

    public ConsultasTransversalesUseCaseImpl(final ConsultasTransversalesQueryPort queryPort) {
        this(queryPort, Clock.systemUTC());
    }

    public ConsultasTransversalesUseCaseImpl(final ConsultasTransversalesQueryPort queryPort, final Clock clock) {
        this.queryPort = Objects.requireNonNull(queryPort, "ConsultasTransversalesQueryPort es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    @Override
    public Pagina<ProgramaCreditosProjection> consultarProgramasCreditos(final UUID usuario, final String busqueda,
                                                                         final Boolean estaActivo, final Integer pagina,
                                                                         final Integer tamano) {
        return queryPort.consultarProgramasCreditos(exigirUsuario(usuario), ConsultaPaginadaRules.busqueda(busqueda),
                estaActivo, ConsultaPaginadaRules.pagina(pagina), ConsultaPaginadaRules.tamano(tamano));
    }

    @Override
    public Pagina<DocenteFacultadProjection> consultarDocentes(final UUID usuario, final FiltroConsulta filtro,
                                                               final Integer pagina, final Integer tamano) {
        return queryPort.consultarDocentes(exigirUsuario(usuario), normalizar(filtro),
                ConsultaPaginadaRules.pagina(pagina), ConsultaPaginadaRules.tamano(tamano));
    }

    @Override
    public Pagina<AsignaturaProjection> buscarAsignaturas(final UUID usuario, final FiltroConsulta filtro,
                                                          final Integer pagina, final Integer tamano) {
        return queryPort.buscarAsignaturas(exigirUsuario(usuario), normalizar(filtro),
                ConsultaPaginadaRules.pagina(pagina), ConsultaPaginadaRules.tamano(tamano));
    }

    @Override
    public Pagina<EstudianteInstitucionProjection> buscarEstudiantes(final UUID usuario, final FiltroConsulta filtro,
                                                                     final Integer pagina, final Integer tamano) {
        return queryPort.buscarEstudiantes(exigirUsuario(usuario), normalizar(filtro),
                ConsultaPaginadaRules.pagina(pagina), ConsultaPaginadaRules.tamano(tamano));
    }

    @Override
    public List<HistorialEstudianteProjection> consultarHistorial(final UUID usuario, final UUID idEstudiante) {
        if (ObjectHelper.isNull(idEstudiante)) {
            throw new ValidationException("El estudiante es obligatorio.");
        }
        final List<HistorialEstudianteProjection> historial = queryPort.consultarHistorial(exigirUsuario(usuario),
                idEstudiante);
        if (historial.isEmpty()) {
            throw new ResourceNotFoundException("El estudiante no existe en su ambito.");
        }
        return historial;
    }

    @Override
    public Pagina<ConsolidadoGrupoProjection> consultarConsolidadoAsistencia(final UUID usuario,
                                                                             final FiltroConsolidado filtro,
                                                                             final Integer pagina,
                                                                             final Integer tamano) {
        return queryPort.consultarConsolidadoAsistencia(exigirUsuario(usuario),
                ObjectHelper.isNull(filtro) ? new FiltroConsolidado(null, null, null, null) : filtro,
                ConsultaPaginadaRules.pagina(pagina), ConsultaPaginadaRules.tamano(tamano));
    }

    @Override
    public Pagina<EventoAuditoriaProjection> consultarAuditoria(final UUID usuario, final FiltroAuditoria filtro,
                                                                final Integer pagina, final Integer tamano) {
        return queryPort.consultarAuditoria(exigirUsuario(usuario), normalizar(filtro),
                ConsultaPaginadaRules.pagina(pagina), ConsultaPaginadaRules.tamano(tamano));
    }

    @Override
    public List<ComponenteFormacionProjection> consultarComponentes(final UUID usuario) {
        return queryPort.consultarComponentes(exigirUsuario(usuario));
    }

    @Override
    public List<EstadoSistemaProjection> consultarEstadosSistema() {
        return queryPort.consultarEstadosSistema();
    }

    /**
     * Rango [desde, hasta) en UTC: sin hasta se usa el instante actual y sin desde los 7 dias anteriores a hasta. El
     * rango debe ser positivo y de maximo 90 dias para no recorrer toda la tabla de auditoria.
     */
    private FiltroAuditoria normalizar(final FiltroAuditoria filtro) {
        final FiltroAuditoria entrada = ObjectHelper.isNull(filtro)
                ? new FiltroAuditoria(null, null, null, null, null, null, null, true) : filtro;
        final OffsetDateTime hasta = ObjectHelper.isNull(entrada.hasta())
                ? OffsetDateTime.now(clock) : entrada.hasta();
        final OffsetDateTime desde = ObjectHelper.isNull(entrada.desde())
                ? hasta.minus(RANGO_AUDITORIA_POR_DEFECTO) : entrada.desde();
        if (!desde.isBefore(hasta)) {
            throw new ValidationException("La fecha inicial debe ser anterior a la fecha final.");
        }
        if (Duration.between(desde, hasta).compareTo(RANGO_AUDITORIA_MAXIMO) > 0) {
            throw new ValidationException("El rango de fechas no puede superar " + RANGO_AUDITORIA_MAXIMO.toDays()
                    + " dias.");
        }
        final String resultado = filtroTexto(entrada.resultado(), "resultado");
        final String resultadoNormalizado = ObjectHelper.isNull(resultado) ? null : resultado.toUpperCase(Locale.ROOT);
        if (!ObjectHelper.isNull(resultadoNormalizado) && !RESULTADOS_AUDITORIA.contains(resultadoNormalizado)) {
            throw new ValidationException("El resultado debe ser SUCCESS o FAILURE.");
        }
        final String accion = filtroTexto(entrada.accion(), "accion");
        return new FiltroAuditoria(desde.withOffsetSameInstant(ZoneOffset.UTC),
                hasta.withOffsetSameInstant(ZoneOffset.UTC),
                ObjectHelper.isNull(accion) ? null : accion.toUpperCase(Locale.ROOT),
                filtroTexto(entrada.tipoRecurso(), "tipo de recurso"), filtroTexto(entrada.idRecurso(), "recurso"),
                entrada.idUsuarioActor(), resultadoNormalizado, entrada.soloAsistencia());
    }

    private static String filtroTexto(final String valor, final String nombre) {
        final String filtro = ObjectHelper.isNull(valor) || valor.isBlank() ? null : valor.trim();
        if (!ObjectHelper.isNull(filtro) && filtro.length() > LONGITUD_MAXIMA_FILTRO_AUDITORIA) {
            throw new ValidationException("El filtro " + nombre + " excede la longitud permitida.");
        }
        return filtro;
    }

    private static UUID exigirUsuario(final UUID usuario) {
        if (ObjectHelper.isNull(usuario)) {
            throw new ForbiddenException("No fue posible resolver el usuario autenticado.");
        }
        return usuario;
    }

    private static FiltroConsulta normalizar(final FiltroConsulta filtro) {
        if (ObjectHelper.isNull(filtro)) {
            return new FiltroConsulta(null, null, null);
        }
        return new FiltroConsulta(ConsultaPaginadaRules.busqueda(filtro.busqueda()), filtro.idPadre(),
                filtro.estaActivo());
    }
}
