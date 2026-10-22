package co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.CrearProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.domain.CrearProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public final class CrearProgramaUseCaseImpl implements CrearProgramaUseCase {

    private static final Pattern PATRON_SOLO_TEXTO = Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s\\-]+$");

    private final ProgramaCommandPort programaCommandPort;
    private final ProgramaQueryPort programaQueryPort;
    private final InstitutionalScopePort scopePort;

    public CrearProgramaUseCaseImpl(
            final ProgramaCommandPort programaCommandPort,
            final InstitutionalScopePort scopePort
    ) {
        this(programaCommandPort, null, scopePort);
    }

    public CrearProgramaUseCaseImpl(
            final ProgramaCommandPort programaCommandPort,
            final ProgramaQueryPort programaQueryPort,
            final InstitutionalScopePort scopePort
    ) {
        this.programaCommandPort = Objects.requireNonNull(programaCommandPort, "El puerto de comando de programas es obligatorio.");
        this.programaQueryPort = programaQueryPort;
        this.scopePort = Objects.requireNonNull(scopePort, "El puerto de alcance institucional es obligatorio.");
    }

    @Override
    public void execute(final CrearProgramaDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos del programa academico son obligatorios.");
        }

        final String nombre = domain.nombre() != null ? domain.nombre().trim() : "";
        if (nombre.length() < 3 || nombre.length() > 50) {
            throw new ValidationException("El nombre del programa academico es obligatorio y debe tener entre 3 y 50 caracteres.");
        }

        if (!PATRON_SOLO_TEXTO.matcher(nombre).matches()) {
            throw new ValidationException("El nombre del programa academico solo debe contener letras y espacios, no debe incluir numeros ni caracteres especiales.");
        }

        if (domain.idTipoPrograma() == null) {
            throw new ValidationException("El tipo de programa academico es obligatorio.");
        }

        if (domain.idCoordinador() == null) {
            throw new ValidationException("El coordinador del programa academico es obligatorio.");
        }

        final UUID idFacultad = scopePort.findFacultadIdByDecanoUsuario(domain.actorUsuarioId())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver la facultad del decano autenticado."));

        if (programaQueryPort != null) {
            final boolean yaExiste = programaQueryPort.consultarProgramasPorFacultad(idFacultad).stream()
                    .anyMatch(p -> p.nombrePrograma() != null && p.nombrePrograma().trim().equalsIgnoreCase(nombre));
            if (yaExiste) {
                throw new ConflictException("Ya existe un programa academico registrado con el nombre '" + nombre + "' en la facultad.");
            }
        }

        final UUID idPrograma = domain.idPrograma() != null ? domain.idPrograma() : UUID.randomUUID();

        programaCommandPort.crearPrograma(new ProgramaCommandPort.CrearProgramaCommand(
                idPrograma,
                domain.codigo(),
                nombre,
                domain.idCoordinador(),
                idFacultad,
                domain.idTipoPrograma(),
                domain.actorUsuarioId()
        ));
    }
}
