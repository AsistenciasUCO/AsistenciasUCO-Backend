package co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports;

import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.ConfiguracionCorreccionIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.CorregirIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.ResultadoCorreccionIdentidadDTO;

/**
 * HU159: correccion de identidad de estudiantes (coordinador) y decanos (administrador), y la configuracion que el
 * frontend necesita para mostrar u ocultar los campos de documento.
 */
public interface CorreccionIdentidadInputPort {

    ResultadoCorreccionIdentidadDTO corregir(CorregirIdentidadDTO dto);

    ConfiguracionCorreccionIdentidadDTO configuracion();
}
