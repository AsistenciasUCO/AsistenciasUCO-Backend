package co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.ConsultarProgramasInstitucionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.dto.ProgramaInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.usecase.ConsultarProgramasInstitucionUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;

import java.util.Objects;
import java.util.UUID;

public final class ConsultarProgramasInstitucionInteractor implements ConsultarProgramasInstitucionInputPort {

    private final ConsultarProgramasInstitucionUseCase useCase;

    public ConsultarProgramasInstitucionInteractor(final ConsultarProgramasInstitucionUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ConsultarProgramasInstitucionUseCase es obligatorio.");
    }

    @Override
    public PaginaResultado<ProgramaInstitucionDTO> execute(
            final UUID usuarioEjecutor,
            final String texto,
            final UUID idFacultad,
            final Integer pagina,
            final Integer tamano
    ) {
        final var resultado = useCase.execute(usuarioEjecutor, texto, idFacultad, pagina, tamano);
        return new PaginaResultado<>(
                resultado.elementos().stream()
                        .map(programa -> new ProgramaInstitucionDTO(programa.id(), programa.nombre(), programa.idFacultad(), programa.nombreFacultad()))
                        .toList(),
                resultado.numeroPagina(), resultado.tamanoPagina(), resultado.totalElementos());
    }
}
