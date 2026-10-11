package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.SesionV2ConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.dto.ConsultarSesionV2DTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

/**
 * Puerto de entrada para consultar una sesion por el contrato v2 (instantes UTC + procedencia).
 */
public interface ConsultarSesionV2InputPort extends InteractorWithReturn<ConsultarSesionV2DTO, SesionV2ConsultadaDTO> {
}
