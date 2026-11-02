package co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.dto.AsignaturaOfertaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.dto.OfertaCreacionGruposDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.domain.OfertaCreacionGruposDocenteDomain;

public final class ConsultarOfertaCreacionGruposDocenteMapper {
    private ConsultarOfertaCreacionGruposDocenteMapper() {
    }

    public static OfertaCreacionGruposDocenteDTO toDTO(final OfertaCreacionGruposDocenteDomain domain) {
        return new OfertaCreacionGruposDocenteDTO(domain.idDocente(), domain.asignaturas().stream()
                .map(a -> new AsignaturaOfertaDocenteDTO(a.idAsignatura(), a.codigoAsignatura(), a.nombreAsignatura(),
                        a.idPrograma(), a.nombrePrograma()))
                .toList());
    }
}
