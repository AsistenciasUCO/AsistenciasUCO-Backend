package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.response;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.ResumenAsistenciaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartoma.primaryports.CerrarTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.AsistenciaTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.FaltanteTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultartomassesion.primaryports.ConsultarTomasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.registrarasistenciastoma.primaryports.RegistrarAsistenciasTomaInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.CancelacionSesionResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * CULM-001 B.2 (contrato 3.2.5 y 3.2.8): cuerpos HTTP de las tomas y del resumen por horas. Instantes UTC ISO-8601 con
 * {@code Z}; horas y porcentajes con 2 decimales (ya redondeados por el adaptador). Los nulos se serializan.
 */
public final class TomaAsistenciaHttpResponses {

    private TomaAsistenciaHttpResponses() {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record TomaAsistenciaResponse(UUID idToma, UUID idSesion, UUID idGrupo, int numero, String modo, String estado,
                                         boolean esImplicita, String fechaAperturaUtc, String fechaCierreUtc, BigDecimal horasToma,
                                         int totalRegistros, int totalPresentes, int totalAusentes, int totalExcusas,
                                         boolean tieneQrActivo) {

        public static TomaAsistenciaResponse from(final TomaAsistenciaResumen t) {
            return new TomaAsistenciaResponse(t.idToma(), t.idSesion(), t.idGrupo(), t.numero(), t.modo(), t.estado(),
                    t.esImplicita(), utc(t.fechaAperturaUtc()), utc(t.fechaCierreUtc()), t.horasToma(), t.totalRegistros(),
                    t.totalPresentes(), t.totalAusentes(), t.totalExcusas(), t.tieneQrActivo());
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record VentanaResponse(String aperturaUtc, String cierreUtc, boolean abierta) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record TomasSesionResponse(UUID idSesion, String estado, CancelacionSesionResponse cancelacion, VentanaResponse ventana,
                                      BigDecimal horasSesion, int maximoTomas, List<TomaAsistenciaResponse> tomas) {

        public static TomasSesionResponse from(final ConsultarTomasSesionInputPort.Resultado r) {
            return new TomasSesionResponse(r.tomas().idSesion(), r.estado().name(), CancelacionSesionResponse.from(r.cancelacion()),
                    new VentanaResponse(utc(r.tomas().ventanaAperturaUtc()), utc(r.tomas().ventanaCierreUtc()), r.tomas().ventanaAbierta()),
                    r.tomas().horasSesion(), r.tomas().maximoTomas(),
                    r.tomas().tomas().stream().map(TomaAsistenciaResponse::from).toList());
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record CierreTomaResponse(TomaAsistenciaResponse toma, int totalAusentesMarcados, int totalFaltantes, boolean yaEstabaCerrada) {

        public static CierreTomaResponse from(final CerrarTomaAsistenciaInputPort.Resultado r) {
            return new CierreTomaResponse(TomaAsistenciaResponse.from(r.toma()), r.totalAusentesMarcados(), r.totalFaltantes(),
                    r.yaEstabaCerrada());
        }
    }

    public record LoteTomaResponse(UUID idToma, UUID idSesion, int totalRegistros) {

        public static LoteTomaResponse from(final RegistrarAsistenciasTomaInputPort.Resultado r) {
            return new LoteTomaResponse(r.idToma(), r.idSesion(), r.totalRegistros());
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record AsistenciaTomaResponse(UUID estudianteId, String nombreCompleto, String estado, boolean registradoPorEstudiante,
                                         String fechaRegistroUtc) {

        public static AsistenciaTomaResponse from(final AsistenciaTomaFila f) {
            return new AsistenciaTomaResponse(f.estudianteId(), f.nombreCompleto(), f.estado(), f.registradoPorEstudiante(),
                    utc(f.fechaRegistroUtc()));
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record FaltanteTomaResponse(UUID estudianteId, String nombreCompleto, boolean tieneRegistro, String estado,
                                       boolean marcadoAlCerrar) {

        public static FaltanteTomaResponse from(final FaltanteTomaFila f) {
            return new FaltanteTomaResponse(f.estudianteId(), f.nombreCompleto(), f.tieneRegistro(), f.estado(), f.marcadoAlCerrar());
        }
    }

    /** Resumen del estudiante en la sabana del docente: identificacion mas los indicadores por horas (contrato 3.2.8). */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ResumenAsistenciaEstudianteResponse(UUID estudianteId, String nombreCompleto, String codigoEstadoEstudiante,
                                                      int sesionesEjecutadas, BigDecimal horasDictadas, BigDecimal horasAsistidas,
                                                      BigDecimal horasFalta, BigDecimal horasExcusa, BigDecimal horasSinRegistro,
                                                      BigDecimal porcentajeInasistencia, BigDecimal porcentajeAsistencia,
                                                      String codigoPermanencia, BigDecimal umbralAdvertencia, BigDecimal umbralPerdida) {

        public static ResumenAsistenciaEstudianteResponse from(final ResumenAsistenciaEstudianteDTO r) {
            final IndicadoresAsistenciaDTO i = r.indicadores();
            return new ResumenAsistenciaEstudianteResponse(r.estudianteId(), r.nombreCompleto(), r.codigoEstadoEstudiante(),
                    i.sesionesEjecutadas(), i.horasDictadas(), i.horasAsistidas(), i.horasFalta(), i.horasExcusa(),
                    i.horasSinRegistro(), i.porcentajeInasistencia(), i.porcentajeAsistencia(), i.codigoPermanencia(),
                    i.umbralAdvertencia(), i.umbralPerdida());
        }
    }

    public static <T, R> ApiPageResponse<R> pagina(final PaginaResultado<T> pagina, final Function<T, R> mapper) {
        return ApiPageResponse.of(pagina.elementos().stream().map(mapper).toList(), pagina.numeroPagina(), pagina.tamanoPagina(),
                pagina.totalElementos());
    }

    static String utc(final Instant instante) {
        return instante == null ? null : instante.toString();
    }
}
