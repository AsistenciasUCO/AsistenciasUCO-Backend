package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.exception.MatriculaQrErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort.CredencialMatricula;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort.CursoMatricula;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort.MatriculaRealizada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort.RegistroPublico;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort.VentanaAbierta;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort.VentanaPorRotar;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort.VentanaRotada;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU176B: pruebas del adaptador JPA de matricula por QR/PIN, traduccion de codigos DBCODE=MQR_* y conversion de fechas UTC.
 */
class MatriculaQrJpaRepositoryTest {

    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DOCENTE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID VENTANA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USUARIO = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ESTUDIANTE = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID TIPO_DOC = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final String TOKEN = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-AbCde";
    private static final String PIN = "482913";
    private static final Instant INSTANTE = Instant.parse("2026-11-04T15:00:00Z");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final JpaProcedureExecutor procedureExecutor = mock(JpaProcedureExecutor.class);
    private final MatriculaQrJpaRepository repository = new MatriculaQrJpaRepository(entityManager, procedureExecutor);

    @BeforeEach
    void correlacion() {
        CorrelationIdContext.set(UUID.randomUUID());
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new MatriculaQrJpaRepository(null, procedureExecutor));
        assertThrows(NullPointerException.class, () -> new MatriculaQrJpaRepository(entityManager, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void abrir_envia_hash_sha256_y_convierte_uuid_y_fecha() {
        when(procedureExecutor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(2)))
                .thenReturn(new ProcedureRow(mock(ProcedureResult.class), List.of(VENTANA.toString(), LocalDateTime.ofInstant(INSTANTE, ZoneOffset.UTC))));

        final VentanaAbierta abierta = repository.abrirVentana(GRUPO, TOKEN, PIN, DOCENTE);

        assertEquals(VENTANA, abierta.idVentana());
        assertEquals(INSTANTE, abierta.fechaExpiracionUtc());
        final ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(procedureExecutor).executeWithExtras(
                eq("abrirVentanaMatriculaGrupo"), eq(MatriculaQrJpaRepository.SQL_ABRIR), captor.capture(), any(), eq(2));
        assertEquals(GRUPO, captor.getValue().get("idGrupo"));
        assertArrayEquals(TokenHashHelper.sha256(TOKEN), (byte[]) captor.getValue().get("tokenHash"));
        assertEquals(PIN, captor.getValue().get("pin"));
    }

    @Test
    void cerrar_ejecuta_el_sp_de_cierre() {
        repository.cerrarVentana(GRUPO, DOCENTE);

        verify(procedureExecutor).execute(eq("cerrarVentanaMatriculaGrupo"), eq(MatriculaQrJpaRepository.SQL_CERRAR), anyMap(), any());
    }

    @Test
    void rotar_manual_convierte_distintos_formatos_de_fecha_utc() {
        final OffsetDateTime rotacion = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
        final Date expiracion = new Date(INSTANTE.toEpochMilli());
        when(procedureExecutor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(4)))
                .thenReturn(new ProcedureRow(mock(ProcedureResult.class), List.of(VENTANA, DOCENTE, rotacion, expiracion)));

        final VentanaRotada rotada = repository.rotarManual(GRUPO, TOKEN, PIN, DOCENTE);

        assertEquals(VENTANA, rotada.idVentana());
        assertEquals(DOCENTE, rotada.idDocenteEmisor());
        assertEquals(INSTANTE, rotada.fechaRotacionUtc());
    }

    @Test
    void rotar_automatico_confirma_vigencia_y_devuelve_nulo_si_no_aplico_o_vencio() {
        when(procedureExecutor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(4)))
                .thenReturn(new ProcedureRow(mock(ProcedureResult.class), List.of(VENTANA, DOCENTE, INSTANTE, "2026-11-04 16:00:00")));
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(MatriculaQrJpaRepository.SQL_VALIDAR_TOKEN)).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{GRUPO, "Calculo", "G1", "Docente", INSTANTE, 10}));

        final VentanaRotada vigente = repository.rotarAutomatico(GRUPO, TOKEN, PIN);
        assertEquals(VENTANA, vigente.idVentana());

        // Otra instancia ya roto el token: validar responde MQR_001 -> rotarAutomatico devuelve null
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=MQR_001|Token no vigente"));
        assertNull(repository.rotarAutomatico(GRUPO, TOKEN, PIN));

        // La ventana ya habia vencido al intentar rotar: el SP devuelve MQR_001 -> rotarAutomatico devuelve null
        when(procedureExecutor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(4)))
                .thenThrow(new ResourceNotFoundException(MatriculaQrErrorCode.MQR_001));
        assertNull(repository.rotarAutomatico(GRUPO, TOKEN, PIN));
    }

    @Test
    void rotar_automatico_propaga_errores_distintos_de_mqr_001() {
        when(procedureExecutor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(4)))
                .thenThrow(new ResourceNotFoundException("OTRO", "Otro error"));

        assertThrows(ResourceNotFoundException.class, () -> repository.rotarAutomatico(GRUPO, TOKEN, PIN));

        when(procedureExecutor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(4)))
                .thenReturn(new ProcedureRow(mock(ProcedureResult.class), List.of(VENTANA, DOCENTE, INSTANTE, INSTANTE)));
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(MatriculaQrJpaRepository.SQL_VALIDAR_TOKEN)).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=GEN_001|Otro"));

        assertThrows(ResourceNotFoundException.class, () -> repository.rotarAutomatico(GRUPO, TOKEN, PIN));
    }

    @Test
    void consultar_ventanas_por_rotar_mapea_las_filas() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(MatriculaQrJpaRepository.SQL_POR_ROTAR)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{GRUPO, DOCENTE.toString()}));

        final List<VentanaPorRotar> resultado = repository.consultarVentanasPorRotar();

        assertEquals(List.of(new VentanaPorRotar(GRUPO, DOCENTE)), resultado);
    }

    @Test
    void validar_por_pin_y_errores_de_validacion_o_resultado_vacio() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(MatriculaQrJpaRepository.SQL_VALIDAR_PIN)).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{GRUPO, "Calculo", "G1", "Docente", INSTANTE, 5}));

        final CredencialMatricula porPin = new CredencialMatricula(null, PIN);
        final CursoMatricula curso = repository.validar(porPin);
        assertEquals(5, curso.cupoDisponible());

        when(query.getResultList()).thenReturn(List.of());
        assertThrows(DatabaseOperationException.class, () -> repository.validar(porPin));

        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=MQR_007|Credencial ambigua"));
        assertThrows(ValidationException.class, () -> repository.validar(porPin));
    }

    @Test
    void registrar_publico_y_matricular_autenticado_soportan_token_y_pin() {
        when(procedureExecutor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(3)))
                .thenReturn(new ProcedureRow(mock(ProcedureResult.class), List.of(ESTUDIANTE, USUARIO, GRUPO)));

        final MatriculaRealizada pubToken = repository.registrarPublico(new RegistroPublico(
                new CredencialMatricula(TOKEN, null), TIPO_DOC, 1017555444, "Ana", "", "Gomez", "", "ana@uco.edu.co", "HASH"));
        final MatriculaRealizada pubPin = repository.registrarPublico(new RegistroPublico(
                new CredencialMatricula(null, PIN), TIPO_DOC, 1017555444, "Ana", "", "Gomez", "", "ana@uco.edu.co", "HASH"));
        final MatriculaRealizada autToken = repository.matricularAutenticado(new CredencialMatricula(TOKEN, null), USUARIO);
        final MatriculaRealizada autPin = repository.matricularAutenticado(new CredencialMatricula(null, PIN), USUARIO);

        assertEquals(GRUPO, pubToken.idGrupo());
        assertEquals(GRUPO, pubPin.idGrupo());
        assertEquals(ESTUDIANTE, autToken.idEstudiante());
        assertEquals(USUARIO, autPin.idUsuario());
    }
}
