package co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.primaryports.dto.TipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.usecase.domain.TipoProgramaDomain;

public final class ConsultarTiposProgramaMapper {

    private ConsultarTiposProgramaMapper() {
    }

    public static TipoProgramaDTO toDTO(final TipoProgramaDomain domain) {
        return new TipoProgramaDTO(
                domain.id(),
                domain.nombre(),
                domain.estaActivoTipoPrograma(),
                domain.estaActivoTextoTipoPrograma()
        );
    }
}
