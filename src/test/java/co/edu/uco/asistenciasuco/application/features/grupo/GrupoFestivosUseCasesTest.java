package co.edu.uco.asistenciasuco.application.features.grupo;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.primaryports.AsegurarFestivosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.domain.ActualizarGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.impl.ActualizarGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.grupo.common.domain.FranjaHorarioDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.domain.CrearGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.impl.CrearGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoCommandRepositoryProjection;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CULM-001 B.1 (CONTRACT 3.2.3): crear un grupo con horario o cambiar su horario precarga sesiones; antes de abrir esa
 * transaccion se aseguran los festivos de los anios del periodo. Sin horario no hay precarga ni festivos que asegurar.
 */
class GrupoFestivosUseCasesTest {

    private static final List<FranjaHorarioDomain> FRANJAS =
            List.of(new FranjaHorarioDomain("LU", "Lunes", LocalTime.of(8, 0), LocalTime.of(10, 0)));

    private final GrupoRepositoryPort repositorio = mock(GrupoRepositoryPort.class);
    private final AsegurarFestivosPeriodoInputPort asegurar = mock(AsegurarFestivosPeriodoInputPort.class);

    @Test
    void crear_con_horario_asegura_los_festivos_del_periodo_antes_de_crear() {
        final UUID periodo = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        when(repositorio.crearGrupo(any())).thenReturn(new GrupoCommandRepositoryProjection(grupo, "ok"));

        final var resultado = new CrearGrupoUseCaseImpl(repositorio, asegurar).execute(new CrearGrupoDomain(
                UUID.randomUUID(), periodo, 1, "G1", UUID.randomUUID(), false, UUID.randomUUID(), FRANJAS));

        assertEquals(grupo, resultado.id());
        final InOrder orden = inOrder(asegurar, repositorio);
        orden.verify(asegurar).execute(AsegurarFestivosPeriodoInputPort.Comando.dePeriodo(periodo));
        orden.verify(repositorio).crearGrupo(any());
    }

    @Test
    void crear_sin_horario_no_asegura_festivos_y_la_generacion_automatica_sigue_no_disponible() {
        when(repositorio.crearGrupo(any())).thenReturn(new GrupoCommandRepositoryProjection(UUID.randomUUID(), "ok"));
        final CrearGrupoUseCaseImpl useCase = new CrearGrupoUseCaseImpl(repositorio, asegurar);
        useCase.execute(new CrearGrupoDomain(UUID.randomUUID(), UUID.randomUUID(), 1, "G1", UUID.randomUUID(), false,
                UUID.randomUUID()));
        verifyNoInteractions(asegurar);

        assertThrows(FeatureUnavailableException.class, () -> useCase.execute(new CrearGrupoDomain(UUID.randomUUID(),
                UUID.randomUUID(), 1, "G1", UUID.randomUUID(), true, UUID.randomUUID(), FRANJAS)));
        verifyNoInteractions(asegurar);
        assertThrows(NullPointerException.class, () -> new CrearGrupoUseCaseImpl(repositorio, null));
    }

    @Test
    void actualizar_con_horario_asegura_los_festivos_del_grupo_y_sin_horario_no() {
        final UUID grupo = UUID.randomUUID();
        when(repositorio.actualizarGrupo(any())).thenReturn(new GrupoCommandRepositoryProjection(grupo, "ok"));
        final ActualizarGrupoUseCaseImpl useCase = new ActualizarGrupoUseCaseImpl(repositorio, asegurar);

        useCase.execute(new ActualizarGrupoDomain(grupo, 1, "G1", UUID.randomUUID(), 30, UUID.randomUUID(), FRANJAS));
        final InOrder orden = inOrder(asegurar, repositorio);
        orden.verify(asegurar).execute(AsegurarFestivosPeriodoInputPort.Comando.deGrupo(grupo));
        orden.verify(repositorio).actualizarGrupo(any());

        final ActualizarGrupoUseCaseImpl otro = new ActualizarGrupoUseCaseImpl(repositorio, mock(AsegurarFestivosPeriodoInputPort.class));
        assertEquals(grupo, otro.execute(new ActualizarGrupoDomain(grupo, 1, "G1", UUID.randomUUID(), 30, UUID.randomUUID()))
                .id());
        assertThrows(NullPointerException.class, () -> new ActualizarGrupoUseCaseImpl(repositorio, null));
    }
}
