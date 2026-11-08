package co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase;

import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain.AdmisionEstudianteResultadoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain.AdmitirEstudianteDomain;

public interface AdmitirEstudianteUseCase {

    AdmisionEstudianteResultadoDomain execute(AdmitirEstudianteDomain domain);
}
