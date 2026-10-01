package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoCommandRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.GrupoRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrupoRepositorySqlServerAdapterTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID CORRELACION = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID ASIGNATURA = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID DOCENTE = UUID.fromString("43641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO_EJECUTOR = UUID.fromString("53641bab-e3cd-485c-b275-47e7b731e18c");

    @AfterEach
    void clearCorrelationContext() {
        CorrelationIdContext.clear();
    }

    @Test
    void registrarEstudiante_envia_parametros_exactos_al_ejecutor_canonico() {
        final CanonicalStoredProcedureExecutor procedureExecutor = mock(CanonicalStoredProcedureExecutor.class);
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        when(procedureExecutor.execute(anyString(), anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(new CanonicalProcedureResult(CORRELACION, "Estudiante registrado.", "detalle interno", true));
        final AtomicBoolean transaccionEjecutada = new AtomicBoolean(false);
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                procedureExecutor,
                jdbcOperations,
                transactionOperations(transaccionEjecutada)
        );
        CorrelationIdContext.set(CORRELACION);

        final RegistrarEstudianteRepositoryProjection resultado = adapter.registrarEstudianteEnGrupo(dtoValido());

        final ArgumentCaptor<String> operationCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<MapSqlParameterSource> parametersCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedureExecutor).execute(operationCaptor.capture(), sqlCaptor.capture(), parametersCaptor.capture());
        final MapSqlParameterSource parametros = parametersCaptor.getValue();

        assertTrue(transaccionEjecutada.get());
        assertEquals("registrarEstudianteEnGrupo", operationCaptor.getValue());
        assertTrue(sqlCaptor.getValue().contains("dbo.usp_registrar_estudiante_en_grupo\n") || sqlCaptor.getValue().contains("dbo.usp_registrar_estudiante_en_grupo\r\n"));
        assertTrue(sqlCaptor.getValue().contains("@idTipoIdIdentificacion = :idTipoIdIdentificacion"));
        assertEquals(GRUPO, parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_ID_GRUPO));
        assertEquals(123456789, parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_NUMERO_IDENTIFICACION));
        assertEquals("ANA", parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_PRIMER_NOMBRE));
        assertEquals("MARIA", parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_SEGUNDO_NOMBRE));
        assertEquals("PEREZ", parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_PRIMER_APELLIDO));
        assertEquals("GOMEZ", parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_SEGUNDO_APELLIDO));
        assertEquals("ana.perez@uco.edu.co", parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_CORREO));
        assertEquals("Clave123!", parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_PASSWORD));
        assertEquals(CORRELACION, parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_ID_CORRELACION));
        assertEquals(USUARIO_EJECUTOR, parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_ID_USUARIO_EJECUTOR));
        assertEquals(TIPO_IDENTIFICACION, parametros.getValue(GrupoRepositorySqlServerAdapter.PARAM_ID_TIPO_ID_IDENTIFICACION));
        assertEquals(11, parametros.getValues().size());
        assertEquals("Estudiante registrado.", resultado.getMensajeUsuario());
    }

    @Test
    void registrarEstudiante_delega_a_jpa_cuando_esta_configurado() {
        final co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.GrupoJpaCommandPersistence jpaPersistence =
                mock(co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.GrupoJpaCommandPersistence.class);
        when(jpaPersistence.registrarEstudianteEnGrupo(any()))
                .thenReturn(new RegistrarEstudianteRepositoryProjection("Estudiante registrado via JPA."));

        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                mock(CanonicalStoredProcedureExecutor.class),
                mock(NamedParameterJdbcOperations.class),
                transactionOperations(new AtomicBoolean(false)),
                jpaPersistence
        );

        final RegistrarEstudianteRepositoryProjection resultado = adapter.registrarEstudianteEnGrupo(dtoValido());

        assertEquals("Estudiante registrado via JPA.", resultado.getMensajeUsuario());
        verify(jpaPersistence).registrarEstudianteEnGrupo(any());
    }

    @Test
    void registrarEstudiante_propaga_error_de_negocio_del_ejecutor_canonico() {
        final CanonicalStoredProcedureExecutor procedureExecutor = mock(CanonicalStoredProcedureExecutor.class);
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                procedureExecutor,
                mock(NamedParameterJdbcOperations.class),
                transactionOperations(new AtomicBoolean(false))
        );
        when(procedureExecutor.execute(anyString(), anyString(), any(MapSqlParameterSource.class)))
                .thenThrow(new ConflictException(GrupoErrorCode.ERR_MATRICULA_DUPLICADA));
        CorrelationIdContext.set(CORRELACION);

        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> adapter.registrarEstudianteEnGrupo(dtoValido())
        );

        assertEquals("ERR_MATRICULA_DUPLICADA", exception.getCode());
        assertEquals(GrupoErrorCode.ERR_MATRICULA_DUPLICADA.defaultMessage(), exception.getMessage());
    }

    @Test
    void registrarEstudiante_propaga_error_tecnico_del_ejecutor_canonico() {
        final CanonicalStoredProcedureExecutor procedureExecutor = mock(CanonicalStoredProcedureExecutor.class);
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                procedureExecutor,
                mock(NamedParameterJdbcOperations.class),
                transactionOperations(new AtomicBoolean(false))
        );
        when(procedureExecutor.execute(anyString(), anyString(), any(MapSqlParameterSource.class)))
                .thenThrow(new DatabaseOperationException(
                        DatabaseErrorCode.ERR_DB_UNCLASSIFIED,
                        DatabaseErrorCode.ERR_DB_UNCLASSIFIED.defaultMessage()
                ));
        CorrelationIdContext.set(CORRELACION);

        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> adapter.registrarEstudianteEnGrupo(dtoValido())
        );

        assertEquals("ERR_DB_UNCLASSIFIED", exception.getCode());
        assertEquals(DatabaseErrorCode.ERR_DB_UNCLASSIFIED.defaultMessage(), exception.getMessage());
    }

    @Test
    void errores_jdbc_en_consultarGrupos_se_convierten_en_databaseOperationException() {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        when(jdbcOperations.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenThrow(new DataAccessResourceFailureException("fallo tecnico"));
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                mock(CanonicalStoredProcedureExecutor.class),
                jdbcOperations,
                transactionOperations(new AtomicBoolean(false))
        );

        assertThrows(DatabaseOperationException.class, adapter::consultarGrupos);
    }

    @Test
    void registrarEstudiante_sin_contexto_no_genera_correlationId_nuevo() {
        final AtomicBoolean spEjecutado = new AtomicBoolean(false);
        final CanonicalStoredProcedureExecutor procedureExecutor = mock(CanonicalStoredProcedureExecutor.class);
        when(procedureExecutor.execute(anyString(), anyString(), any(MapSqlParameterSource.class)))
                .thenAnswer(invocation -> {
                    spEjecutado.set(true);
                    return new CanonicalProcedureResult(CORRELACION, "ok", "ok", true);
                });
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                procedureExecutor,
                mock(NamedParameterJdbcOperations.class),
                transactionOperations(new AtomicBoolean(false))
        );

        assertThrows(CrosscuttingException.class, () -> adapter.registrarEstudianteEnGrupo(dtoValido()));
        assertFalse(spEjecutado.get());
        assertEquals(null, CorrelationIdContext.get());
    }

    @Test
    void consultarGrupos_usa_procedimiento_usp_consultar_grupos_paginado_y_mapea_columnas() {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        final GrupoRepositoryProjection entity = new GrupoRepositoryProjection(
                GRUPO,
                "G1",
                "Grupo 1",
                ASIGNATURA,
                "Backend",
                DOCENTE,
                30,
                12,
                18,
                true,
                LocalDate.of(2026, 1, 20),
                LocalDate.of(2026, 5, 30)
        );
        when(jdbcOperations.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(entity));
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                mock(CanonicalStoredProcedureExecutor.class),
                jdbcOperations,
                transactionOperations(new AtomicBoolean(false))
        );

        final List<GrupoRepositoryProjection> resultado = adapter.consultarGrupos();

        final ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcOperations).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));
        assertEquals(1, resultado.size());
        assertTrue(sqlCaptor.getValue().contains("dbo.usp_consultar_grupos_paginado"));
        assertEquals(1, paramsCaptor.getValue().getValue(GrupoRepositorySqlServerAdapter.PARAM_NUMERO_PAGINA));
        assertEquals(100, paramsCaptor.getValue().getValue(GrupoRepositorySqlServerAdapter.PARAM_TAMANO_PAGINA));
        assertEquals("Backend", resultado.getFirst().getNombreAsignatura());
    }

    @Test
    void grupoRepositoryRowMapper_convierte_tipos_de_sql_server() throws SQLException {
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id")).thenReturn(GRUPO.toString());
        when(resultSet.getString("codigo")).thenReturn("G1");
        when(resultSet.getString("nombre")).thenReturn("Grupo 1");
        when(resultSet.getObject("idAsignatura")).thenReturn(ASIGNATURA);
        when(resultSet.getString("nombreAsignatura")).thenReturn("Backend");
        when(resultSet.getObject("idDocente")).thenReturn(DOCENTE.toString());
        when(resultSet.getObject("capacidadMaximaPermitida")).thenReturn(30L);
        when(resultSet.getObject("estudiantesActivos")).thenReturn("12");
        when(resultSet.getObject("cuposDisponibles")).thenReturn(18);
        when(resultSet.getObject("grupoEstaHablitado")).thenReturn(1);
        when(resultSet.getObject("fechaInicioPeriodoAcademico")).thenReturn(Timestamp.valueOf(
                LocalDateTime.of(2026, 1, 20, 8, 0)
        ));
        when(resultSet.getObject("fechaFinPeriodoAcademico")).thenReturn(java.sql.Date.valueOf("2026-05-30"));

        final GrupoRepositoryProjection entity = new GrupoRepositoryRowMapper().mapRow(resultSet, 0);

        assertEquals(GRUPO, entity.getId());
        assertEquals(ASIGNATURA, entity.getIdAsignatura());
        assertEquals(DOCENTE, entity.getIdDocente());
        assertEquals(30, entity.getCapacidadMaximaPermitida());
        assertEquals(12, entity.getEstudiantesActivos());
        assertTrue(entity.isGrupoHabilitado());
        assertEquals(LocalDate.of(2026, 1, 20), entity.getFechaInicioPeriodoAcademico());
        assertEquals(LocalDate.of(2026, 5, 30), entity.getFechaFinPeriodoAcademico());
    }

    @Test
    void crearGrupo_ejecuta_procedimiento_dentro_de_transaccion_y_retorna_mensaje() {
        final CanonicalStoredProcedureExecutor procedureExecutor = mock(CanonicalStoredProcedureExecutor.class);
        when(procedureExecutor.execute(anyString(), anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(new CanonicalProcedureResult(CORRELACION, "Grupo creado.", "detalle", true));
        final AtomicBoolean transaccionEjecutada = new AtomicBoolean(false);
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                procedureExecutor,
                mock(NamedParameterJdbcOperations.class),
                transactionOperations(transaccionEjecutada)
        );
        CorrelationIdContext.set(CORRELACION);

        final UUID usuarioEjecutor = UUID.randomUUID();
        final GrupoCommandRepositoryProjection resultado = adapter.crearGrupo(new CrearGrupoRepositoryDTO(
                GRUPO, ASIGNATURA, UUID.randomUUID(), 1, "Grupo 1", DOCENTE, usuarioEjecutor
        ));

        final var operation = ArgumentCaptor.forClass(String.class);
        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedureExecutor).execute(operation.capture(), sql.capture(), params.capture());
        assertTrue(transaccionEjecutada.get());
        assertEquals("crearGrupo", operation.getValue());
        assertTrue(sql.getValue().contains("dbo.usp_crear_grupo"));
        assertTrue(sql.getValue().contains("@idUsuarioEjecutor"));
        assertFalse(sql.getValue().contains("@aula"));
        assertEquals(GRUPO, params.getValue().getValue(GrupoRepositorySqlServerAdapter.PARAM_ID_GRUPO));
        assertFalse(params.getValue().hasValue("aula"));
        assertEquals(CORRELACION, params.getValue().getValue(GrupoRepositorySqlServerAdapter.PARAM_ID_CORRELACION));
        assertEquals(usuarioEjecutor, params.getValue().getValue(GrupoRepositorySqlServerAdapter.PARAM_ID_USUARIO_EJECUTOR));
        assertEquals(GRUPO, resultado.idGrupo());
        assertEquals("Grupo creado.", resultado.mensajeUsuario());
    }

    @Test
    void crearGrupo_con_dto_nulo_lanza_excepcion_de_dominio() {
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                mock(CanonicalStoredProcedureExecutor.class),
                mock(NamedParameterJdbcOperations.class),
                transactionOperations(new AtomicBoolean(false))
        );

        assertThrows(CrosscuttingException.class, () -> adapter.crearGrupo(null));
    }

    @Test
    void actualizarGrupo_ejecuta_procedimiento_y_retorna_mensaje() {
        final CanonicalStoredProcedureExecutor procedureExecutor = mock(CanonicalStoredProcedureExecutor.class);
        when(procedureExecutor.execute(anyString(), anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(new CanonicalProcedureResult(CORRELACION, "Grupo actualizado.", "detalle", true));
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                procedureExecutor,
                mock(NamedParameterJdbcOperations.class),
                transactionOperations(new AtomicBoolean(false))
        );
        CorrelationIdContext.set(CORRELACION);

        final UUID usuarioEjecutor = UUID.randomUUID();
        final GrupoCommandRepositoryProjection resultado = adapter.actualizarGrupo(new ActualizarGrupoRepositoryDTO(
                GRUPO, 1, "Grupo 1", DOCENTE, 40, usuarioEjecutor
        ));

        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(procedureExecutor).execute(anyString(), sql.capture(), params.capture());
        assertTrue(sql.getValue().contains("dbo.usp_actualizar_grupo"));
        assertTrue(sql.getValue().contains("@idUsuarioEjecutor"));
        assertFalse(sql.getValue().contains("@aula"));
        assertFalse(params.getValue().hasValue("aula"));
        assertEquals(40, params.getValue().getValue(GrupoRepositorySqlServerAdapter.PARAM_CUPO_MAXIMO));
        assertEquals(usuarioEjecutor, params.getValue().getValue(GrupoRepositorySqlServerAdapter.PARAM_ID_USUARIO_EJECUTOR));
        assertEquals("Grupo actualizado.", resultado.mensajeUsuario());
    }

    @Test
    void actualizarGrupo_con_dto_nulo_lanza_excepcion_de_dominio() {
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                mock(CanonicalStoredProcedureExecutor.class),
                mock(NamedParameterJdbcOperations.class),
                transactionOperations(new AtomicBoolean(false))
        );

        assertThrows(CrosscuttingException.class, () -> adapter.actualizarGrupo(null));
    }

    @Test
    void consultarEstudiantesGrupo_mapea_proyeccion_desde_vista_confirmada() throws SQLException {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        final UUID matricula = UUID.randomUUID();
        final UUID estudianteId = UUID.randomUUID();
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id")).thenReturn(matricula);
        when(resultSet.getObject("idEstudiante")).thenReturn(estudianteId);
        when(resultSet.getObject("documento")).thenReturn("123456789");
        when(resultSet.getObject("nombreCompleto")).thenReturn("Ana Perez");
        when(resultSet.getObject("correo")).thenReturn("ana@uco.edu.co");
        when(resultSet.getObject("codigoEstado")).thenReturn("ACTIVO");
        when(resultSet.getObject("nombreEstado")).thenReturn("Activo");
        when(jdbcOperations.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<EstudianteGrupoRepositoryProjection>) invocation.getArgument(2))
                        .mapRow(resultSet, 0)));
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                mock(CanonicalStoredProcedureExecutor.class),
                jdbcOperations,
                transactionOperations(new AtomicBoolean(false))
        );

        final List<EstudianteGrupoRepositoryProjection> resultado = adapter.consultarEstudiantesGrupo(GRUPO);

        assertEquals(1, resultado.size());
        assertEquals(estudianteId, resultado.getFirst().idEstudiante());
        assertEquals("Ana Perez", resultado.getFirst().nombreCompleto());
        final var sql = ArgumentCaptor.forClass(String.class);
        final var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcOperations).query(sql.capture(), params.capture(), any(RowMapper.class));
        assertTrue(sql.getValue().contains("dbo.usp_consultar_estudiantes_grupo_paginado"));
        assertEquals(GRUPO, params.getValue().getValue(GrupoRepositorySqlServerAdapter.PARAM_ID_GRUPO));
        assertEquals(1, params.getValue().getValue(GrupoRepositorySqlServerAdapter.PARAM_NUMERO_PAGINA));
        assertEquals(100, params.getValue().getValue(GrupoRepositorySqlServerAdapter.PARAM_TAMANO_PAGINA));
    }

    @Test
    void consultarEstudiantesGrupo_traduce_error_jdbc_a_databaseOperationException() {
        final NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        when(jdbcOperations.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenThrow(new DataAccessResourceFailureException("fallo tecnico"));
        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                mock(CanonicalStoredProcedureExecutor.class),
                jdbcOperations,
                transactionOperations(new AtomicBoolean(false))
        );

        assertThrows(DatabaseOperationException.class, () -> adapter.consultarEstudiantesGrupo(GRUPO));
    }

    @Test
    void queries_delega_a_jpa_cuando_esta_configurado() {
        final var jpaQueryPersistence = mock(co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.GrupoJpaQueryPersistence.class);
        final var proyGrupo = mock(GrupoRepositoryProjection.class);
        final var proyEstudiante = mock(EstudianteGrupoRepositoryProjection.class);

        when(jpaQueryPersistence.consultarGrupos(org.mockito.ArgumentMatchers.isNull())).thenReturn(List.of(proyGrupo));
        when(jpaQueryPersistence.consultarEstudiantesGrupo(org.mockito.ArgumentMatchers.eq(GRUPO), org.mockito.ArgumentMatchers.isNull())).thenReturn(List.of(proyEstudiante));

        final GrupoRepositorySqlServerAdapter adapter = new GrupoRepositorySqlServerAdapter(
                mock(CanonicalStoredProcedureExecutor.class),
                mock(NamedParameterJdbcOperations.class),
                transactionOperations(new AtomicBoolean(false)),
                null,
                jpaQueryPersistence
        );

        final List<GrupoRepositoryProjection> grupos = adapter.consultarGrupos();
        final List<EstudianteGrupoRepositoryProjection> estudiantes = adapter.consultarEstudiantesGrupo(GRUPO);

        assertEquals(1, grupos.size());
        assertEquals(proyGrupo, grupos.getFirst());
        verify(jpaQueryPersistence).consultarGrupos(null);

        assertEquals(1, estudiantes.size());
        assertEquals(proyEstudiante, estudiantes.getFirst());
        verify(jpaQueryPersistence).consultarEstudiantesGrupo(GRUPO, null);
    }

    private RegistrarEstudianteRepositoryDTO dtoValido() {
        return new RegistrarEstudianteRepositoryDTO(
                TIPO_IDENTIFICACION,
                123456789,
                "PEREZ",
                "GOMEZ",
                "ANA",
                "MARIA",
                "ana.perez@uco.edu.co",
                "Clave123!",
                GRUPO,
                USUARIO_EJECUTOR
        );
    }

    private TransactionOperations transactionOperations(final AtomicBoolean transaccionEjecutada) {
        return new TransactionOperations() {
            @Override
            public <T> T execute(final TransactionCallback<T> action) {
                transaccionEjecutada.set(true);
                return action.doInTransaction(mock(TransactionStatus.class));
            }
        };
    }
}
