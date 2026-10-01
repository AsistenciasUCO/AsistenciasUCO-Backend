package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SesionRepositorySqlServerAdapterTest {

    private static final UUID CORRELATION = UUID.randomUUID();
    private static final UUID SESSION = UUID.randomUUID();
    private static final UUID GROUP = UUID.randomUUID();
    private static final UUID TEACHER = UUID.randomUUID();
    private final CanonicalStoredProcedureExecutor procedures = mock(CanonicalStoredProcedureExecutor.class);
    private final NamedParameterJdbcOperations jdbc = mock(NamedParameterJdbcOperations.class);
    private final SesionRepositorySqlServerAdapter adapter = new SesionRepositorySqlServerAdapter(procedures, jdbc);

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
    }

    /**
     * Contrato TARGET (LB-001B.1, CONTRACT_FREEZE.md secc. 6/7#17 — YA GREEN): SQL_CREAR_SESION no
     * contiene {@code @descripcion}/{@code @aula}/{@code @tipo}. Estas 3 aserciones son REGRESION,
     * no RED de esta fase.
     *
     * <p>Contrato TARGET (LB-001B.3, CONTRACT_FREEZE.md secc. 1 / punto A de TASK_AUTORIZADA.md
     * &sect;27): {@code usp_crear_sesion} congelado NO declara {@code @idDocente}
     * (firma exacta: {@code @idGrupo, @nombre, @fechaHoraInicio, @fechaHoraFin, @idCorrelacion,
     * @idUsuarioEjecutor}). RED esperado: {@code SQL_CREAR_SESION} del adapter AS-IS todavia
     * contiene {@code @idDocente = :idDocente} y {@code crearSesion(...)} todavia envia
     * {@code dto.getDocente()} como parametro {@code idDocente} al SP — la asercion de ausencia
     * de abajo falla hoy contra produccion sin modificar. CrearSesionRepositoryDTO conserva el
     * campo/getter {@code docente} sin cambio (CONTRACT_FREEZE.md secc. 1: solo deja de viajar al
     * SP, se sigue usando como pre-check de InstitutionalScopePort en la capa usecase).</p>
     */
    @Test
    void createPassesConfirmedProcedureAndParameters() {
        final LocalDateTime start = LocalDateTime.of(2026, 9, 14, 8, 0);
        final UUID usuarioEjecutor = UUID.randomUUID();
        CorrelationIdContext.set(CORRELATION);
        adapter.crearSesion(new CrearSesionRepositoryDTO(GROUP, "Nombre sesion", start,
                start.plusHours(1), usuarioEjecutor));

        final var operation = ArgumentCaptor.forClass(String.class);
        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedures).execute(operation.capture(), sql.capture(), params.capture());
        assertEquals("crearSesion", operation.getValue());
        assertTrue(sql.getValue().contains("dbo.usp_crear_sesion"));
        assertTrue(sql.getValue().contains("@idUsuarioEjecutor"));
        assertFalse(sql.getValue().contains("@descripcion"));
        assertFalse(sql.getValue().contains("@aula"));
        assertFalse(sql.getValue().contains("@tipo"));
        // LB-001B.3 / punto A — RED esperado hoy: SQL_CREAR_SESION AS-IS todavia declara @idDocente.
        assertFalse(sql.getValue().contains("@idDocente"),
                "usp_crear_sesion congelado no declara @idDocente (CONTRACT_FREEZE.md secc. 1)");
        assertEquals(GROUP, params.getValue().getValue("idGrupo"));
        assertEquals(start, params.getValue().getValue("fechaHoraInicio"));
        assertEquals(CORRELATION, params.getValue().getValue("idCorrelacion"));
        assertEquals(usuarioEjecutor, params.getValue().getValue("idUsuarioEjecutor"));
        assertFalse(params.getValue().hasValue("aula"));
        assertFalse(params.getValue().hasValue("descripcion"));
        assertFalse(params.getValue().hasValue("tipo"));
        // LB-001B.3 / punto A — RED esperado hoy: crearSesion(...) AS-IS todavia envia dto.getDocente()
        // como parametro idDocente (SesionRepositorySqlServerAdapter.java l.134).
        assertFalse(params.getValue().hasValue("idDocente"),
                "crearSesion(...) no debe enviar idDocente al SP congelado (CONTRACT_FREEZE.md secc. 1)");
        // LB-001B.4A: firma EXACTA usp_crear_sesion(@idGrupo,@nombre,@fechaHoraInicio,@fechaHoraFin,
        // @idCorrelacion,@idUsuarioEjecutor) y nombre viaja como @nombre (nunca tema).
        assertEquals("Nombre sesion", params.getValue().getValue("nombre"));
        assertFalse(params.getValue().hasValue("tema"));
        assertEquals(6, params.getValue().getParameterNames().length);
        assertEquals(java.util.List.of("@idGrupo", "@nombre", "@fechaHoraInicio", "@fechaHoraFin",
                        "@idCorrelacion", "@idUsuarioEjecutor"),
                signatureOf(sql.getValue()));
    }

    private static java.util.List<String> signatureOf(final String sql) {
        final var matcher = java.util.regex.Pattern.compile("@\\w+").matcher(sql);
        final java.util.List<String> names = new java.util.ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group());
        }
        return names;
    }

    /**
     * Contrato TARGET (LB-001B.1, CONTRACT_FREEZE.md secc. 6/7#17 — YA GREEN): SQL_ACTUALIZAR_SESION
     * no contiene {@code @aula}/{@code @descripcion}. Estas aserciones son REGRESION, no RED de esta fase.
     *
     * <p>Contrato TARGET (LB-001B.3, CONTRACT_FREEZE.md secc. 2 / punto B de TASK_AUTORIZADA.md
     * &sect;27): {@code usp_actualizar_sesion} congelado NO declara {@code @idDocente} (firma exacta:
     * {@code @idSesion, @nombre, @fechaHoraInicio, @fechaHoraFin, @idCorrelacion, @idUsuarioEjecutor}).
     * RED esperado: {@code SQL_ACTUALIZAR_SESION} del adapter AS-IS todavia contiene
     * {@code @idDocente = :idDocente} y {@code actualizarSesion(...)} todavia envia {@code dto.docente()}
     * como parametro {@code idDocente} al SP.</p>
     *
     * <p>{@code usp_cerrar_sesion} (CONTRACT_FREEZE.md secc. 3) NO esta congelado en esta fase: la
     * firma exacta de ese SP legacy no esta documentada en DB_BASELINE_CONTRACT.md, por lo que
     * {@code @idDocente} permanece AS-IS en {@code SQL_CERRAR_SESION}/{@code cerrarSesion(...)}. Las
     * aserciones de {@code cerrarSesion} de abajo confirman ese AS-IS preservado, no piden su retiro.</p>
     */
    @Test
    void updateCloseAndGeneratePassThePublicProcedures() {
        final LocalDateTime start = LocalDateTime.of(2026, 9, 14, 8, 0);
        final UUID usuarioEjecutor = UUID.randomUUID();
        CorrelationIdContext.set(CORRELATION);
        adapter.actualizarSesion(new ActualizarSesionRepositoryDTO(SESSION, "Nuevo", start,
                start.plusHours(2), usuarioEjecutor));
        adapter.cerrarSesion(new CerrarSesionRepositoryDTO(SESSION, TEACHER, "Cierre", usuarioEjecutor));
        adapter.generarSesionesGrupo(new GenerarSesionesGrupoRepositoryDTO(GROUP, usuarioEjecutor));

        final var operation = ArgumentCaptor.forClass(String.class);
        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedures, org.mockito.Mockito.times(3)).execute(operation.capture(), sql.capture(), params.capture());
        assertEquals(java.util.List.of("actualizarSesion", "cerrarSesion", "generarSesionesGrupo"), operation.getAllValues());
        assertTrue(sql.getAllValues().get(0).contains("dbo.usp_actualizar_sesion"));
        assertFalse(sql.getAllValues().get(0).contains("@aula"));
        assertFalse(sql.getAllValues().get(0).contains("@descripcion"));
        // LB-001B.3 / punto B — RED esperado hoy: SQL_ACTUALIZAR_SESION AS-IS todavia declara @idDocente.
        assertFalse(sql.getAllValues().get(0).contains("@idDocente"),
                "usp_actualizar_sesion congelado no declara @idDocente (CONTRACT_FREEZE.md secc. 2)");
        assertTrue(sql.getAllValues().get(1).contains("dbo.usp_cerrar_sesion"));
        // usp_cerrar_sesion permanece AS-IS (CONTRACT_FREEZE.md secc. 3, NOT_READY): @idDocente se
        // preserva a proposito, no se retira en esta fase.
        assertTrue(sql.getAllValues().get(1).contains("@idDocente"));
        assertTrue(sql.getAllValues().get(2).contains("dbo.usp_generar_sesiones_grupo"));
        sql.getAllValues().forEach(value -> assertTrue(value.contains("@idUsuarioEjecutor")));
        assertEquals(SESSION, params.getAllValues().get(0).getValue("idSesion"));
        assertEquals("Nuevo", params.getAllValues().get(0).getValue("nombre"));
        assertFalse(params.getAllValues().get(0).hasValue("aula"));
        assertFalse(params.getAllValues().get(0).hasValue("descripcion"));
        // LB-001B.3 / punto B — RED esperado hoy: actualizarSesion(...) AS-IS todavia envia dto.docente()
        // como parametro idDocente (SesionRepositorySqlServerAdapter.java l.157).
        assertFalse(params.getAllValues().get(0).hasValue("idDocente"),
                "actualizarSesion(...) no debe enviar idDocente al SP congelado (CONTRACT_FREEZE.md secc. 2)");
        // LB-001B.4A: firma EXACTA usp_actualizar_sesion(@idSesion,@nombre,@fechaHoraInicio,
        // @fechaHoraFin,@idCorrelacion,@idUsuarioEjecutor), sin @idDocente ni @tema.
        assertEquals(java.util.List.of("@idSesion", "@nombre", "@fechaHoraInicio", "@fechaHoraFin",
                        "@idCorrelacion", "@idUsuarioEjecutor"),
                signatureOf(sql.getAllValues().get(0)));
        assertFalse(params.getAllValues().get(0).hasValue("tema"));
        assertEquals(6, params.getAllValues().get(0).getParameterNames().length);
        // cerrarSesion(...) preserva idDocente AS-IS (CONTRACT_FREEZE.md secc. 3).
        assertEquals(TEACHER, params.getAllValues().get(1).getValue("idDocente"));
        assertEquals(GROUP, params.getAllValues().get(2).getValue("idGrupo"));
        params.getAllValues().forEach(value -> assertEquals(CORRELATION, value.getValue("idCorrelacion")));
        params.getAllValues().forEach(value -> assertEquals(usuarioEjecutor, value.getValue("idUsuarioEjecutor")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryMapsConfirmedViewColumnsAndReturnsNullWhenAbsent() throws Exception {
        final ResultSet resultSet = mock(ResultSet.class);
        final LocalDateTime start = LocalDateTime.of(2026, 9, 14, 8, 0);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getObject("id")).thenReturn(SESSION.toString());
        when(resultSet.getObject("idGrupo")).thenReturn(GROUP);
        when(resultSet.getObject("nombre")).thenReturn("Sesión");
        when(resultSet.getObject("numero")).thenReturn(1L);
        when(resultSet.getObject("codigo")).thenReturn("S01");
        when(resultSet.getObject("numeroSemana")).thenReturn("2");
        when(resultSet.getObject("codigoGrupo")).thenReturn("G01");
        when(resultSet.getObject("nombreGrupo")).thenReturn("Grupo 1");
        // LB-001B.3 / CONTRACT_FREEZE.md secc. 5.4: construir el fixture de Timestamp bajo
        // TimeZone.getDefault()=UTC explicito, restaurando la zona original en finally, para que el
        // round-trip no dependa (sin saberlo) de que la maquina de build tenga zona ambiental UTC —
        // mismo patron que JdbcValueMapperTest.toLocalDateTime_no_depende_del_systemDefault_...
        final TimeZone originalDefaultTimeZoneParaFixture = TimeZone.getDefault();
        final Timestamp fechaHoraInicioTimestamp;
        final Timestamp fechaHoraFinTimestamp;
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            fechaHoraInicioTimestamp = Timestamp.valueOf(start);
            fechaHoraFinTimestamp = Timestamp.valueOf(start.plusHours(1));
        } finally {
            TimeZone.setDefault(originalDefaultTimeZoneParaFixture);
        }
        when(resultSet.getObject("fechaHoraInicio")).thenReturn(fechaHoraInicioTimestamp);
        when(resultSet.getObject("fechaHoraFin")).thenReturn(fechaHoraFinTimestamp);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(ResultSetExtractor.class)))
                .thenAnswer(invocation -> ((ResultSetExtractor<SesionRepositoryProjection>) invocation.getArgument(2))
                        .extractData(resultSet));

        final SesionRepositoryProjection result = adapter.consultarSesion(new ConsultarSesionRepositoryDTO(SESSION));
        assertEquals(SESSION, result.getSesion());
        assertEquals(GROUP, result.getGrupo());
        assertEquals("Sesión", result.getNombre());
        assertEquals(1, result.getNumero());
        assertEquals(2, result.getNumeroSemana());
        assertEquals(start, result.getFechaHoraInicio());
        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(sql.capture(), params.capture(), any(ResultSetExtractor.class));
        assertTrue(sql.getValue().contains("FROM dbo.uv_sesion"));
        assertEquals(SESSION, params.getValue().getValue("idSesion"));

        when(resultSet.next()).thenReturn(false);
        assertNull(adapter.consultarSesion(new ConsultarSesionRepositoryDTO(SESSION)));
    }

    @Test
    void rejectsMissingDtoAndWrapsJdbcFailure() {
        assertThrows(CrosscuttingException.class, () -> adapter.crearSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.actualizarSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.consultarSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.cerrarSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.generarSesionesGrupo(null));

        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(ResultSetExtractor.class)))
                .thenThrow(new DataAccessResourceFailureException("sin conexión"));
        assertThrows(DatabaseOperationException.class,
                () -> adapter.consultarSesion(new ConsultarSesionRepositoryDTO(SESSION)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultarSesionesPorGrupoMapeaFilasDeLaVistaYFiltraPorGrupo() throws Exception {
        final ResultSet resultSet = mock(ResultSet.class);
        final LocalDateTime start = LocalDateTime.of(2026, 9, 14, 8, 0);
        when(resultSet.getObject("id")).thenReturn(SESSION.toString());
        when(resultSet.getObject("idGrupo")).thenReturn(GROUP);
        when(resultSet.getObject("nombre")).thenReturn("Sesión 1");
        when(resultSet.getObject("numero")).thenReturn(3L);
        when(resultSet.getObject("codigo")).thenReturn("S03");
        when(resultSet.getObject("numeroSemana")).thenReturn("4");
        when(resultSet.getObject("codigoGrupo")).thenReturn("G01");
        when(resultSet.getObject("nombreGrupo")).thenReturn("Grupo 1");
        when(resultSet.getObject("fechaHoraInicio")).thenReturn(start);
        when(resultSet.getObject("fechaHoraFin")).thenReturn(null);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<SesionRepositoryProjection>) invocation.getArgument(2))
                        .mapRow(resultSet, 0)));

        final List<SesionRepositoryProjection> result = adapter.consultarSesionesPorGrupo(GROUP);

        assertEquals(1, result.size());
        final SesionRepositoryProjection row = result.getFirst();
        assertEquals(SESSION, row.getSesion());
        assertEquals(GROUP, row.getGrupo());
        assertEquals("Sesión 1", row.getNombre());
        assertEquals(3, row.getNumero());
        assertEquals("S03", row.getCodigo());
        assertEquals(4, row.getNumeroSemana());
        assertEquals("G01", row.getCodigoGrupo());
        assertEquals("Grupo 1", row.getNombreGrupo());
        assertEquals(start, row.getFechaHoraInicio());
        assertNull(row.getFechaHoraFin());
        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(sql.capture(), params.capture(), any(RowMapper.class));
        assertTrue(sql.getValue().contains("dbo.usp_consultar_sesiones_grupo_paginado"));
        assertEquals(GROUP, params.getValue().getValue("idGrupo"));
        assertEquals(1, params.getValue().getValue("numeroPagina"));
        assertEquals(100, params.getValue().getValue("tamanoPagina"));
    }

    @Test
    void consultarSesionesPorGrupoDevuelveListaVaciaCuandoElGrupoNoTieneSesiones() {
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());

        assertTrue(adapter.consultarSesionesPorGrupo(GROUP).isEmpty());
    }

    @Test
    void consultarSesionesPorGrupoRechazaGrupoNuloYTraduceFallosJdbc() {
        assertThrows(CrosscuttingException.class, () -> adapter.consultarSesionesPorGrupo(null));

        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenThrow(new DataAccessResourceFailureException("sin conexión"));
        assertThrows(DatabaseOperationException.class, () -> adapter.consultarSesionesPorGrupo(GROUP));
    }

    @Test
    void consultarSesionesPorGrupoDelegaAJpaCuandoEstaConfigurado() {
        final var jpaQueryPersistence = mock(co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.SesionJpaQueryPersistence.class);
        final var proySesion = mock(SesionRepositoryProjection.class);

        when(jpaQueryPersistence.consultarSesionesPorGrupo(org.mockito.ArgumentMatchers.eq(GROUP), org.mockito.ArgumentMatchers.isNull())).thenReturn(List.of(proySesion));

        final SesionRepositorySqlServerAdapter adapterJpa = new SesionRepositorySqlServerAdapter(
                procedures,
                jdbc,
                jpaQueryPersistence
        );

        final List<SesionRepositoryProjection> resultado = adapterJpa.consultarSesionesPorGrupo(GROUP);

        assertEquals(1, resultado.size());
        assertEquals(proySesion, resultado.getFirst());
        verify(jpaQueryPersistence).consultarSesionesPorGrupo(GROUP, null);
        org.mockito.Mockito.verifyNoMoreInteractions(jdbc);
    }
}
