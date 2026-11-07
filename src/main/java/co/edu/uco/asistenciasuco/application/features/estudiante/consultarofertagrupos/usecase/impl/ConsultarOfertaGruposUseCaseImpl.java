package co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.ConsultarOfertaGruposUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.domain.ConsultarOfertaGruposDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.domain.GrupoOfertaDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.domain.OfertaGruposPaginaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.OfertaGruposEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.GrupoOfertaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.OfertaGruposEstudiantePagina;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Map;
import java.util.Objects;

/**
 * HU008: oferta de grupos del estudiante autenticado en periodos vigentes o proximos: especificas de sus programas y
 * transversales (basicas e institucionales) de su institucion, con cupos y la marca de matricula (solo
 * la matricula inscrita cuenta, la resuelve el SP). El ambito y el parametro OFERTA.INCLUIR_TRANSVERSALES los aplica el SP.
 */
public final class ConsultarOfertaGruposUseCaseImpl implements ConsultarOfertaGruposUseCase {

    static final int PAGINA_POR_DEFECTO = 1;
    static final int TAMANO_POR_DEFECTO = 10;
    static final int TAMANO_MAXIMO = 100;
    static final int BUSQUEDA_MAXIMA = 50;
    /** Regla de John (2026-10-27): ESP es propia del programa; BAS e INS son transversales. */
    private static final Map<String, String> TIPO_ASIGNATURA = Map.of("ESP", "ESPECIFICA", "BAS", "BASICA", "INS", "INSTITUCIONAL");

    private final InstitutionalScopePort scopePort;
    private final OfertaGruposEstudianteQueryPort port;

    public ConsultarOfertaGruposUseCaseImpl(final InstitutionalScopePort scopePort, final OfertaGruposEstudianteQueryPort port) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.port = Objects.requireNonNull(port, "OfertaGruposEstudianteQueryPort es obligatorio.");
    }

    @Override
    public OfertaGruposPaginaDomain execute(final ConsultarOfertaGruposDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos de la consulta son obligatorios.");
        }
        final int pagina = domain.pagina() == null ? PAGINA_POR_DEFECTO : domain.pagina();
        final int tamano = domain.tamano() == null ? TAMANO_POR_DEFECTO : domain.tamano();
        if (pagina < 1) {
            throw new ValidationException("El numero de pagina debe ser mayor o igual a 1.");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new ValidationException("El tamano de pagina debe estar entre 1 y " + TAMANO_MAXIMO + ".");
        }
        final String busqueda = domain.busqueda() == null || domain.busqueda().isBlank() ? null : domain.busqueda().trim();
        if (busqueda != null && busqueda.length() > BUSQUEDA_MAXIMA) {
            throw new ValidationException("La busqueda excede la longitud permitida.");
        }
        scopePort.findEstudianteIdByUsuario(domain.actorUsuarioId())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));

        final OfertaGruposEstudiantePagina oferta = port.consultarOferta(domain.actorUsuarioId(), busqueda, pagina, tamano);
        return new OfertaGruposPaginaDomain(oferta.grupos().stream().map(ConsultarOfertaGruposUseCaseImpl::toDomain).toList(),
                oferta.total());
    }

    private static GrupoOfertaDomain toDomain(final GrupoOfertaEstudianteProjection g) {
        return new GrupoOfertaDomain(g.idGrupo(), g.codigoGrupo(), g.nombreGrupo(), g.idAsignatura(), g.codigoAsignatura(),
                g.nombreAsignatura(), g.nombreDocente(), g.idPeriodoAcademico(), g.nombrePeriodoAcademico(), g.fechaInicioPeriodo(),
                g.fechaFinPeriodo(), g.estadoPeriodo(), g.horario(), g.capacidadMaxima(), g.estudiantesActivos(), g.cuposDisponibles(),
                g.matriculado(),
                g.codigoComponente() == null ? null : TIPO_ASIGNATURA.getOrDefault(g.codigoComponente(), g.codigoComponente()),
                "BAS".equals(g.codigoComponente()) || "INS".equals(g.codigoComponente()));
    }
}
