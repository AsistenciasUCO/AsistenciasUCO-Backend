package co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.primaryports;

import java.util.UUID;

/**
 * CULM-001 B.1 (§3.2.3): antes de crear un grupo o de generar o precargar sus sesiones, asegura que los anios del
 * periodo academico tengan festivos cargados. Es de mejor esfuerzo: nunca bloquea la creacion ni la generacion.
 */
public interface AsegurarFestivosPeriodoInputPort {

    void execute(Comando comando);

    /** Exactamente uno: el periodo (crear grupo) o el grupo existente (generar, actualizar horario). */
    record Comando(UUID periodoAcademicoId, UUID grupoId) {

        public static Comando dePeriodo(final UUID periodoAcademicoId) {
            return new Comando(periodoAcademicoId, null);
        }

        public static Comando deGrupo(final UUID grupoId) {
            return new Comando(null, grupoId);
        }
    }
}
