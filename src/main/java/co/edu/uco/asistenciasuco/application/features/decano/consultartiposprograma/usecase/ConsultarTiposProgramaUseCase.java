package co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.usecase.domain.TipoProgramaDomain;

import java.util.List;

public interface ConsultarTiposProgramaUseCase {

    List<TipoProgramaDomain> execute();
}
