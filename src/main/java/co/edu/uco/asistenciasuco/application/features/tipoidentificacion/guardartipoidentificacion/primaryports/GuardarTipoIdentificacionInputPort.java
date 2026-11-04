package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports.dto.GuardarTipoIdentificacionDTO;

public interface GuardarTipoIdentificacionInputPort {
    void execute(GuardarTipoIdentificacionDTO dto);
}
