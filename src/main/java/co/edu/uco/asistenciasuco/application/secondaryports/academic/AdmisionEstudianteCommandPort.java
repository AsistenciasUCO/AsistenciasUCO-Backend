package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AdmisionEstudianteCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AdmisionEstudianteResultadoProjection;

public interface AdmisionEstudianteCommandPort {

    /**
     * Admite al estudiante en un programa de la institucion del administrador, sin matricularlo en grupos (CULM-001 D.4).
     * La base de datos valida RBAC, ambito, unicidad, correo institucional y clave inicial; sus rechazos se traducen a
     * ADS_002 (409), ADS_003 (403), ADS_004 (409), ADS_005 y ADS_006 (400).
     */
    AdmisionEstudianteResultadoProjection admitir(AdmisionEstudianteCommand orden);
}
