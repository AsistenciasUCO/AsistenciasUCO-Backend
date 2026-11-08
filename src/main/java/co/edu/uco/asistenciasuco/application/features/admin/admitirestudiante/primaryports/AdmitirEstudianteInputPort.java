package co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.dto.AdmisionEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.dto.AdmitirEstudianteCommandDTO;

public interface AdmitirEstudianteInputPort {

    AdmisionEstudianteDTO execute(AdmitirEstudianteCommandDTO command);
}
