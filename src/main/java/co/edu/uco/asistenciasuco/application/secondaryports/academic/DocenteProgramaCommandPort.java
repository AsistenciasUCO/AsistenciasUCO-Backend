package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface DocenteProgramaCommandPort {

    void crearDocente(UUID idDocente, String numeroIdentificacion, String primerNombre, String segundoNombre,
                      String primerApellido, String segundoApellido, String correo, UUID idPrograma,
                      String password, UUID usuarioEjecutor);
}
