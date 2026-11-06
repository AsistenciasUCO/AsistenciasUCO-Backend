package co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.ConsultasTransversalesInputPort;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.AsignaturaInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.CategoriaJustificacionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ComponenteFormacionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ConsolidadoGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.DocenteFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EstadoSistemaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EventoAuditoriaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FiltroAuditoriaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FiltroConsolidadoDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EstudianteInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FichaUnificadaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.MatriculaHistorialDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ProgramaCreditosDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ResumenAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.ConsultasTransversalesUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.DocenteFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroAuditoria;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsulta;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsolidado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.HistorialEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ResumenAsistenciaProjection;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public final class ConsultasTransversalesInteractor implements ConsultasTransversalesInputPort {

    private final ConsultasTransversalesUseCase useCase;

    public ConsultasTransversalesInteractor(final ConsultasTransversalesUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ConsultasTransversalesUseCase es obligatorio.");
    }

    @Override
    public PaginaDTO<ProgramaCreditosDTO> consultarProgramasCreditos(final UUID usuario, final String busqueda,
                                                                     final Boolean estaActivo, final Integer pagina,
                                                                     final Integer tamano) {
        return pagina(useCase.consultarProgramasCreditos(usuario, busqueda, estaActivo, pagina, tamano),
                p -> new ProgramaCreditosDTO(p.idPrograma(), p.nombrePrograma(), p.nombreTipoPrograma(),
                        p.nombreModalidad(), p.nombreJornada(), p.programaActivo(), p.idPlanEstudio(),
                        p.inpPlanEstudio(), p.planActivo(), p.totalSemestres(), p.totalAsignaturas(),
                        p.totalCreditos()));
    }

    @Override
    public PaginaDTO<DocenteFacultadDTO> consultarDocentes(final UUID usuario, final String busqueda,
                                                           final UUID idPrograma, final Boolean estaActivo,
                                                           final Integer pagina, final Integer tamano) {
        return pagina(useCase.consultarDocentes(usuario, new FiltroConsulta(busqueda, idPrograma, estaActivo), pagina,
                        tamano), ConsultasTransversalesInteractor::docente);
    }

    @Override
    public PaginaDTO<DocenteFacultadDTO> consultarDocentesInstitucion(final UUID usuario, final String busqueda,
                                                                      final UUID idFacultad, final UUID idPrograma,
                                                                      final Boolean estaActivo, final String estadoVinculo,
                                                                      final Integer pagina, final Integer tamano) {
        return pagina(useCase.consultarDocentesInstitucion(usuario, new FiltroConsulta(busqueda, idPrograma, estaActivo),
                idFacultad, estadoVinculo, pagina, tamano), ConsultasTransversalesInteractor::docente);
    }

    private static DocenteFacultadDTO docente(final DocenteFacultadProjection d) {
        return new DocenteFacultadDTO(d.idDocente(), d.numeroIdentificacion(), d.nombreCompleto(), d.correo(),
                d.activo(), d.programas(), d.totalProgramas(), d.totalGrupos(), d.tieneVinculoActivo());
    }

    @Override
    public PaginaDTO<AsignaturaInstitucionDTO> buscarAsignaturas(final UUID usuario, final String busqueda,
                                                                 final UUID idFacultad, final Boolean estaActiva,
                                                                 final Integer pagina, final Integer tamano) {
        return pagina(useCase.buscarAsignaturas(usuario, new FiltroConsulta(busqueda, idFacultad, estaActiva), pagina,
                        tamano),
                a -> new AsignaturaInstitucionDTO(a.idAsignatura(), a.codigo(), a.nombre(), a.creditos(),
                        a.nombreArea(), a.nombreComponente(), a.codigoSemestre(), a.idPlanEstudio(),
                        a.inpPlanEstudio(), a.idPrograma(), a.nombrePrograma(), a.idFacultad(), a.nombreFacultad(),
                        a.activa()));
    }

    @Override
    public PaginaDTO<EstudianteInstitucionDTO> buscarEstudiantes(final UUID usuario, final String busqueda,
                                                                 final UUID idFacultad, final Boolean estaActivo,
                                                                 final Integer pagina, final Integer tamano) {
        return pagina(useCase.buscarEstudiantes(usuario, new FiltroConsulta(busqueda, idFacultad, estaActivo), pagina,
                        tamano),
                e -> new EstudianteInstitucionDTO(e.idEstudiante(), e.numeroIdentificacion(), e.nombreCompleto(),
                        e.correo(), e.activo(), e.facultades(), e.programas(), e.totalGrupos()));
    }

    /** Los datos personales se repiten en cada fila del provider: se toman de la primera. */
    @Override
    public FichaUnificadaDTO consultarHistorial(final UUID usuario, final UUID idEstudiante) {
        final List<HistorialEstudianteProjection> filas = useCase.consultarHistorial(usuario, idEstudiante);
        final HistorialEstudianteProjection primera = filas.getFirst();
        return new FichaUnificadaDTO(primera.idEstudiante(), primera.tipoIdentificacion(),
                primera.numeroIdentificacion(), primera.nombreCompleto(), primera.correo(), primera.activo(),
                filas.stream().map(ConsultasTransversalesInteractor::matricula).toList());
    }

    @Override
    public PaginaDTO<ConsolidadoGrupoDTO> consultarConsolidadoAsistencia(final UUID usuario,
                                                                         final FiltroConsolidadoDTO filtro,
                                                                         final Integer pagina, final Integer tamano) {
        final FiltroConsolidado consulta = filtro == null ? null : new FiltroConsolidado(filtro.idPeriodoAcademico(),
                filtro.idPrograma(), filtro.idAsignatura(), filtro.idDocente());
        return pagina(useCase.consultarConsolidadoAsistencia(usuario, consulta, pagina, tamano),
                c -> new ConsolidadoGrupoDTO(c.idGrupo(), c.codigoGrupo(), c.nombreGrupo(), c.idPeriodoAcademico(),
                        c.nombrePeriodoAcademico(), c.idPrograma(), c.nombrePrograma(), c.idAsignatura(),
                        c.nombreAsignatura(), c.idDocente(), c.nombreDocente(), c.estudiantesActivos(),
                        c.sesionesEjecutadas(), c.asistencias(), c.faltasSinJustaCausa(), c.excusas(),
                        c.porcentajeAsistencia(), c.porcentajeInasistencia(), c.estudiantesRiesgo(),
                        c.estudiantesPerdida()));
    }

    @Override
    public PaginaDTO<EventoAuditoriaDTO> consultarAuditoria(final UUID usuario, final FiltroAuditoriaDTO filtro,
                                                            final Integer pagina, final Integer tamano) {
        final FiltroAuditoria consulta = filtro == null ? null : new FiltroAuditoria(filtro.desde(), filtro.hasta(),
                filtro.accion(), filtro.tipoRecurso(), filtro.idRecurso(), filtro.idUsuarioActor(), filtro.resultado(),
                !Boolean.FALSE.equals(filtro.soloAsistencia()));
        return pagina(useCase.consultarAuditoria(usuario, consulta, pagina, tamano),
                e -> new EventoAuditoriaDTO(e.id(), e.ocurridoEn(), e.idActor(), e.tipoActor(), e.nombreActor(),
                        e.accion(), e.tipoRecurso(), e.idRecurso(), e.resultado(), e.idTraza(), e.metodoHttp(),
                        e.ruta(), e.estadoHttp(), e.ipCliente(), e.codigoError(), e.metadatos(),
                        e.accionAsistencia()));
    }

    @Override
    public List<ComponenteFormacionDTO> consultarComponentes(final UUID usuario) {
        return useCase.consultarComponentes(usuario).stream()
                .map(c -> new ComponenteFormacionDTO(c.idComponente(), c.codigo(), c.nombre(), c.totalAsignaturas()))
                .toList();
    }

    @Override
    public List<EstadoSistemaDTO> consultarEstadosSistema() {
        return useCase.consultarEstadosSistema().stream()
                .map(e -> new EstadoSistemaDTO(e.catalogo(), e.id(), e.codigo(), e.nombre()))
                .toList();
    }

    @Override
    public List<CategoriaJustificacionDTO> consultarCategoriasJustificacion() {
        return useCase.consultarCategoriasJustificacion().stream()
                .map(c -> new CategoriaJustificacionDTO(c.id(), c.codigo(), c.nombre(), c.descripcion()))
                .toList();
    }

    private static MatriculaHistorialDTO matricula(final HistorialEstudianteProjection h) {
        final ResumenAsistenciaProjection r = h.resumen();
        return new MatriculaHistorialDTO(h.idFacultad(), h.nombreFacultad(), h.idPrograma(), h.nombrePrograma(),
                h.inpPlanEstudio(), h.idPeriodoAcademico(), h.codigoPeriodo(), h.nombrePeriodo(), h.idGrupo(),
                h.codigoGrupo(), h.idAsignatura(), h.nombreAsignatura(), h.codigoEstadoEstudiante(),
                h.nombreEstadoEstudiante(),
                r == null ? null : new ResumenAsistenciaDTO(r.sesionesEjecutadas(), r.asistencias(),
                        r.faltasSinJustaCausa(), r.excusas(), r.porcentajeInasistencia(), r.codigoPermanencia()));
    }

    private static <P, D> PaginaDTO<D> pagina(final Pagina<P> resultado, final Function<P, D> mapper) {
        return new PaginaDTO<>(resultado.elementos().stream().map(mapper).toList(), resultado.total());
    }
}
