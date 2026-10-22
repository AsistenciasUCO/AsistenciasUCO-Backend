package co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.usecase.ConsultarTiposProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.usecase.domain.TipoProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaQueryPort;

import java.util.List;
import java.util.Objects;

public final class ConsultarTiposProgramaUseCaseImpl implements ConsultarTiposProgramaUseCase {

    private final TipoProgramaQueryPort tipoProgramaQueryPort;

    public ConsultarTiposProgramaUseCaseImpl(final TipoProgramaQueryPort tipoProgramaQueryPort) {
        this.tipoProgramaQueryPort = Objects.requireNonNull(tipoProgramaQueryPort, "El puerto de consulta de tipos de programa es obligatorio.");
    }

    @Override
    public List<TipoProgramaDomain> execute() {
        return tipoProgramaQueryPort.consultarTiposPrograma().stream()
                .filter(Objects::nonNull)
                .map(row -> new TipoProgramaDomain(
                        row.id(),
                        row.nombre(),
                        row.estaActivoTipoPrograma(),
                        row.estaActivoTextoTipoPrograma()
                ))
                .toList();
    }
}
