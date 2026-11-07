package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para consultar asistencias por grupo.
 */
public final class ConsultarAsistenciasPorGrupoRepositoryDTO {

    private UUID grupo;
    private UUID sesion;
    private UUID usuarioEjecutor;

    public ConsultarAsistenciasPorGrupoRepositoryDTO() {
        super();
    }

    public ConsultarAsistenciasPorGrupoRepositoryDTO(final UUID grupo, final UUID sesion) {
        setGrupo(grupo);
        setSesion(sesion);
    }

    /** CULM-001 A.2-2: el ambito del grupo lo valida el SP con el ejecutor autenticado. */
    public ConsultarAsistenciasPorGrupoRepositoryDTO(final UUID grupo, final UUID sesion, final UUID usuarioEjecutor) {
        this(grupo, sesion);
        setUsuarioEjecutor(usuarioEjecutor);
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

    public void setUsuarioEjecutor(final UUID usuarioEjecutor) {
        this.usuarioEjecutor = usuarioEjecutor;
    }

    public UUID getGrupo() {
        return grupo;
    }

    public void setGrupo(final UUID grupo) {
        this.grupo = grupo;
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

}
