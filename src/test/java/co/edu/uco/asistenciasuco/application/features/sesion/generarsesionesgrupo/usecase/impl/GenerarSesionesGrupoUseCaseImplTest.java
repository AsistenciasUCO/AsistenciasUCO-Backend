package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.primaryports.AsegurarFestivosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.domain.GenerarSesionesGrupoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort.GeneracionSesiones;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GenerarSesionesGrupoUseCaseImplTest {

    private final SesionRepositoryPort sesionRepositoryPort = mock(SesionRepositoryPort.class);
    private final AsegurarFestivosPeriodoInputPort asegurarFestivos = mock(AsegurarFestivosPeriodoInputPort.class);
    private final GenerarSesionesGrupoUseCaseImpl useCase =
            new GenerarSesionesGrupoUseCaseImpl(sesionRepositoryPort, asegurarFestivos);

    @Test
    void execute_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
        verifyNoInteractions(asegurarFestivos, sesionRepositoryPort);
    }

    @Test
    void execute_propaga_grupo_y_usuarioEjecutor_al_repositorio() {
        final UUID grupo = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();
        final GenerarSesionesGrupoDomain domain = new GenerarSesionesGrupoDomain(grupo, usuarioEjecutor);

        useCase.execute(domain);

        final ArgumentCaptor<GenerarSesionesGrupoRepositoryDTO> captor =
                ArgumentCaptor.forClass(GenerarSesionesGrupoRepositoryDTO.class);
        verify(sesionRepositoryPort).generarSesionesGrupo(captor.capture());
        assertEquals(grupo, captor.getValue().grupo());
        assertEquals(usuarioEjecutor, captor.getValue().usuarioEjecutor());
    }

    // CULM-001 B.1 (3.2.3): primero se aseguran los festivos del periodo del grupo y luego se genera con el v2.
    @Test
    void execute_asegura_los_festivos_del_grupo_antes_de_generar_y_devuelve_los_conteos_del_v2() {
        final UUID grupo = UUID.randomUUID();
        when(sesionRepositoryPort.generarSesionesGrupo(any())).thenReturn(new GeneracionSesiones(3, 1));

        final GeneracionSesiones resultado = useCase.execute(new GenerarSesionesGrupoDomain(grupo, UUID.randomUUID()));

        assertEquals(new GeneracionSesiones(3, 1), resultado);
        final InOrder orden = inOrder(asegurarFestivos, sesionRepositoryPort);
        orden.verify(asegurarFestivos).execute(AsegurarFestivosPeriodoInputPort.Comando.deGrupo(grupo));
        orden.verify(sesionRepositoryPort).generarSesionesGrupo(any());
    }

    @Test
    void execute_sin_conteos_del_repositorio_devuelve_ceros() {
        assertEquals(new GeneracionSesiones(0, 0),
                useCase.execute(new GenerarSesionesGrupoDomain(UUID.randomUUID(), UUID.randomUUID())));
    }
}
