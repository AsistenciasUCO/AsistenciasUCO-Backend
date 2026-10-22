package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain.DocenteProgramaDomain;

import java.util.List;
import java.util.UUID;

public interface ConsultarDocentesProgramaUseCase {

    List<DocenteProgramaDomain> execute(UUID actorUsuarioId);
}
