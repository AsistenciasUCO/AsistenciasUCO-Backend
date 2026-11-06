package co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.dto;

import java.util.UUID;

public record PlanEstudioDTO(UUID id, UUID idPrograma, String nombrePrograma, String inp,
                             boolean estaActivoPlanEstudio, String estaActivoTextoPlanEstudio,
                             String justificacionEstado, int totalSemestres, int totalAsignaturas,
                             int totalCreditos) {

    /** Compatibilidad: sin totales de la malla (certificacion F3). */
    public PlanEstudioDTO(final UUID id, final UUID idPrograma, final String nombrePrograma, final String inp,
            final boolean estaActivoPlanEstudio, final String estaActivoTextoPlanEstudio,
            final String justificacionEstado, final int totalSemestres) {
        this(id, idPrograma, nombrePrograma, inp, estaActivoPlanEstudio, estaActivoTextoPlanEstudio,
                justificacionEstado, totalSemestres, 0, 0);
    }

    public PlanEstudioDTO(final UUID id, final UUID idPrograma, final String nombrePrograma, final String inp,
                          final boolean estaActivoPlanEstudio, final String estaActivoTextoPlanEstudio,
                          final String justificacionEstado) {
        this(id, idPrograma, nombrePrograma, inp, estaActivoPlanEstudio, estaActivoTextoPlanEstudio,
                justificacionEstado, 0);
    }
}
