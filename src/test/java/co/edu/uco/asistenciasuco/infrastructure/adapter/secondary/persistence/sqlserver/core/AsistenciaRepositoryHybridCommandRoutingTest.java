package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaAutonomaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * LB-002.2C — CMD-CFG-013: el hibrido enruta cada metodo del puerto al delegado correcto y sin transformar
 * el argumento. Con el constructor de 3 argumentos SOLO {@code registrarAsistenciasSesion} va a la
 * persistencia de command; los demas commands siguen SIEMPRE en el baseline JDBC. El constructor de 2
 * argumentos (compatibilidad LB-002.1) mantiene {@code registrarAsistenciasSesion} en el baseline.
 */
class AsistenciaRepositoryHybridCommandRoutingTest {

    private final AsistenciaRepositoryPort jdbcCommands = mock(AsistenciaRepositoryPort.class);
    private final AsistenciaQueryPersistence query = mock(AsistenciaQueryPersistence.class);
    private final AsistenciaCommandPersistence batchCommand = mock(AsistenciaCommandPersistence.class);
    private final AsistenciaRepositoryHybridSqlServerAdapter adapter =
            new AsistenciaRepositoryHybridSqlServerAdapter(jdbcCommands, query, batchCommand);

    @Test
    void registrar_asistencias_sesion_va_a_la_persistencia_de_command_con_la_misma_instancia_y_no_a_jdbc() {
        final RegistrarAsistenciasSesionRepositoryDTO lote = mock(RegistrarAsistenciasSesionRepositoryDTO.class);

        adapter.registrarAsistenciasSesion(lote);

        verify(batchCommand).registrarAsistenciasSesion(lote);
        verifyNoMoreInteractions(batchCommand);
        verifyNoInteractions(jdbcCommands, query);
    }

    @Test
    void los_demas_commands_siguen_en_jdbc_aunque_el_command_de_lote_sea_otra_tecnologia() {
        final RegistrarAsistenciaRepositoryDTO individual = mock(RegistrarAsistenciaRepositoryDTO.class);
        final RegistrarAsistenciaAutonomaRepositoryDTO autonoma = mock(RegistrarAsistenciaAutonomaRepositoryDTO.class);
        final SolicitarRevisionAsistenciaRepositoryDTO solicitud = mock(SolicitarRevisionAsistenciaRepositoryDTO.class);
        final ResolverSolicitudRevisionAsistenciaRepositoryDTO resolucion =
                mock(ResolverSolicitudRevisionAsistenciaRepositoryDTO.class);

        adapter.registrarAsistencia(individual);
        adapter.registrarAsistenciaAutonoma(autonoma);
        adapter.solicitarRevisionAsistencia(solicitud);
        adapter.resolverSolicitudRevisionAsistencia(resolucion);

        verify(jdbcCommands).registrarAsistencia(individual);
        verify(jdbcCommands).registrarAsistenciaAutonoma(autonoma);
        verify(jdbcCommands).solicitarRevisionAsistencia(solicitud);
        verify(jdbcCommands).resolverSolicitudRevisionAsistencia(resolucion);
        verifyNoMoreInteractions(jdbcCommands);
        verifyNoInteractions(batchCommand, query);
    }

    @Test
    void la_query_va_solo_a_la_persistencia_de_query() {
        final ConsultarAsistenciasPorGrupoRepositoryDTO dto =
                new ConsultarAsistenciasPorGrupoRepositoryDTO(UUID.randomUUID(), UUID.randomUUID());
        final List<AsistenciaRepositoryProjection> esperado = List.of();
        when(query.consultarAsistenciasPorGrupo(dto)).thenReturn(esperado);

        assertSame(esperado, adapter.consultarAsistenciasPorGrupo(dto));

        verifyNoInteractions(jdbcCommands, batchCommand);
    }

    @Test
    void la_excepcion_de_la_persistencia_de_command_se_propaga_sin_reintento_ni_fallback_a_jdbc() {
        final RegistrarAsistenciasSesionRepositoryDTO lote = mock(RegistrarAsistenciasSesionRepositoryDTO.class);
        final IllegalStateException falla = new IllegalStateException("marker");
        doThrow(falla).when(batchCommand).registrarAsistenciasSesion(lote);

        assertSame(falla, assertThrows(IllegalStateException.class, () -> adapter.registrarAsistenciasSesion(lote)));

        verifyNoInteractions(jdbcCommands);
    }

    @Test
    void el_constructor_de_dos_argumentos_conserva_registrar_asistencias_sesion_en_el_baseline_jdbc() {
        final AsistenciaRepositoryHybridSqlServerAdapter compatible =
                new AsistenciaRepositoryHybridSqlServerAdapter(jdbcCommands, query);
        final RegistrarAsistenciasSesionRepositoryDTO lote = mock(RegistrarAsistenciasSesionRepositoryDTO.class);

        compatible.registrarAsistenciasSesion(lote);

        verify(jdbcCommands).registrarAsistenciasSesion(lote);
        verifyNoInteractions(query, batchCommand);
    }

    @Test
    void exige_los_tres_colaboradores() {
        assertThrows(NullPointerException.class,
                () -> new AsistenciaRepositoryHybridSqlServerAdapter(null, query, batchCommand));
        assertThrows(NullPointerException.class,
                () -> new AsistenciaRepositoryHybridSqlServerAdapter(jdbcCommands, null, batchCommand));
        assertThrows(NullPointerException.class,
                () -> new AsistenciaRepositoryHybridSqlServerAdapter(jdbcCommands, query, null));
    }
}
