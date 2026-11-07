package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.AsistenciaTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.FaltanteTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomasSesionResumen;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CULM-001 B.2 (DEC-013): tomas de asistencia por sesion. Los comandos son SP con la tupla canonica
 * ({@code usp_abrir_toma_asistencia}, {@code usp_cerrar_toma_asistencia}, {@code usp_registrar_asistencias_toma}); los
 * rechazos de negocio ({@code TOM_*}, {@code SES_*}, {@code ATT_*}) se propagan como excepciones de aplicacion
 * clasificadas por el adaptador. La base de datos repite RBAC DOCENTE y la titularidad de la sesion.
 */
public interface TomaAsistenciaRepositoryPort {

    record TomaAbierta(UUID idToma, int numero, String codigoModo, Instant fechaAperturaUtc) {
    }

    record CierreToma(UUID idToma, UUID idSesion, UUID idGrupo, int totalAusentesMarcados, int totalFaltantes,
                      boolean yaEstabaCerrada) {
    }

    /** Toma cerrada por el job porque su ventana vencio o la sesion se cancelo ({@code usp_cerrar_tomas_vencidas}). */
    record TomaCerradaAutomaticamente(UUID idToma, UUID idSesion, UUID idGrupo, int totalAusentesMarcados) {
    }

    record RegistroToma(UUID idEstudiante, String estado) {
    }

    record LoteRegistrado(UUID idToma, UUID idSesion, UUID idGrupo, int totalRegistros) {
    }

    TomaAbierta abrir(UUID sesionId, String codigoModo, UUID usuarioEjecutor);

    /** {@code marcarAusentes} nulo aplica el valor del modo ({@code ModoTomaAsistencia.marcaAusentesAlCerrar}). */
    CierreToma cerrar(UUID tomaId, Boolean marcarAusentes, UUID usuarioEjecutor);

    List<TomaCerradaAutomaticamente> cerrarVencidas();

    LoteRegistrado registrarLote(UUID tomaId, List<RegistroToma> registros, UUID usuarioEjecutor);

    /** Lectura de {@code uv_toma_asistencia} por id (sin ambito: la titularidad la valida Application con el grupo). */
    Optional<TomaAsistenciaResumen> consultarToma(UUID tomaId);

    TomasSesionResumen consultarTomasSesion(UUID sesionId, UUID usuarioEjecutor);

    PaginaResultado<AsistenciaTomaFila> consultarAsistencias(UUID tomaId, int numeroPagina, int tamanoPagina, UUID usuarioEjecutor);

    PaginaResultado<FaltanteTomaFila> consultarFaltantes(UUID tomaId, int numeroPagina, int tamanoPagina, UUID usuarioEjecutor);
}
