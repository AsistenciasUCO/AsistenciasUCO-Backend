package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.AsistenciaTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.FaltanteTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomasSesionResumen;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvTomaAsistenciaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 B.2: adaptador JPA de las tomas (SP canonicos con conteo exacto y consultas leidas por nombre de columna). */
class TomaAsistenciaJpaRepositoryTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TOMA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USUARIO = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final JpaProcedureExecutor executor = mock(JpaProcedureExecutor.class);
    private final Query query = mock(Query.class);
    private final TomaAsistenciaJpaRepository repository = new TomaAsistenciaJpaRepository(entityManager, executor);
    private UUID correlacion;

    @BeforeEach
    void preparar() {
        correlacion = UUID.randomUUID();
        CorrelationIdContext.set(correlacion);
        when(entityManager.createNativeQuery(anyString(), eq(Tuple.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    /** Tupla en memoria (sin Mockito, para poder crearla dentro de un stubbing). */
    static Tuple tupla(final Map<String, Object> valores) {
        return new Tuple() {
            @Override
            public <X> X get(final jakarta.persistence.TupleElement<X> elemento) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <X> X get(final String alias, final Class<X> tipo) {
                return tipo.cast(valores.get(alias));
            }

            @Override
            public Object get(final String alias) {
                return valores.get(alias);
            }

            @Override
            public <X> X get(final int i, final Class<X> tipo) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object get(final int i) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object[] toArray() {
                return valores.values().toArray();
            }

            @Override
            public List<jakarta.persistence.TupleElement<?>> getElements() {
                return List.of();
            }
        };
    }

    @Test
    @SuppressWarnings("unchecked")
    void abrir_envia_los_parametros_y_lee_las_ocho_columnas_del_sp() {
        when(executor.executeWithExtras(eq("abrirTomaAsistencia"), eq(TomaAsistenciaJpaRepository.SQL_ABRIR), anyMap(), eq(correlacion), eq(8)))
                .thenReturn(new ProcedureRow(null, Arrays.asList(TOMA.toString(), (short) 2, "QR", true,
                        Timestamp.valueOf("2026-11-07 13:05:00"), LocalDateTime.of(2026, 11, 7, 15, 30), 2, new BigDecimal("1.000000"))));

        assertEquals(new TomaAsistenciaRepositoryPort.TomaAbierta(TOMA, 2, "QR", Instant.parse("2026-11-07T13:05:00Z")),
                repository.abrir(SESION, "QR", USUARIO));
        final ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.forClass(Map.class);
        verify(executor).executeWithExtras(anyString(), anyString(), parametros.capture(), any(), eq(8));
        assertEquals(Map.of("idSesion", SESION, "codigoModo", "QR", "idCorrelacion", correlacion, "idUsuarioEjecutor", USUARIO),
                parametros.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void cerrar_envia_el_bit_como_texto_o_nulo_y_lee_siete_columnas() {
        when(executor.executeWithExtras(eq("cerrarTomaAsistencia"), eq(TomaAsistenciaJpaRepository.SQL_CERRAR), anyMap(), eq(correlacion), eq(7)))
                .thenReturn(new ProcedureRow(null, Arrays.asList(TOMA, SESION, GRUPO, "2026-11-07 14:00:00", 3, 4, false)));

        assertEquals(new TomaAsistenciaRepositoryPort.CierreToma(TOMA, SESION, GRUPO, 3, 4, false), repository.cerrar(TOMA, null, USUARIO));
        repository.cerrar(TOMA, true, USUARIO);
        repository.cerrar(TOMA, false, USUARIO);
        final ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.forClass(Map.class);
        verify(executor, org.mockito.Mockito.times(3)).executeWithExtras(anyString(), anyString(), parametros.capture(), any(), eq(7));
        assertEquals(Arrays.asList(null, "1", "0"), parametros.getAllValues().stream().map(p -> p.get("marcarAusentes")).toList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void registrar_lote_serializa_el_json_exacto_y_lee_cuatro_columnas() {
        final UUID e1 = UUID.fromString("55555555-5555-5555-5555-555555555555");
        when(executor.executeWithExtras(eq("registrarAsistenciasToma"), anyString(), anyMap(), eq(correlacion), eq(4)))
                .thenReturn(new ProcedureRow(null, Arrays.asList(TOMA, SESION, GRUPO, 1)));

        assertEquals(new TomaAsistenciaRepositoryPort.LoteRegistrado(TOMA, SESION, GRUPO, 1),
                repository.registrarLote(TOMA, List.of(new TomaAsistenciaRepositoryPort.RegistroToma(e1, "AN")), USUARIO));
        final ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.forClass(Map.class);
        verify(executor).executeWithExtras(anyString(), anyString(), parametros.capture(), any(), eq(4));
        assertEquals("[{\"idEstudiante\":\"" + e1 + "\",\"estado\":\"AN\"}]", parametros.getValue().get("asistenciaJSON"));
    }

    @Test
    void cerrar_vencidas_lee_las_filas_por_nombre() {
        when(query.getResultList()).thenReturn(List.of(tupla(Map.of("idToma", TOMA.toString(), "idSesion", SESION.toString(),
                "idGrupo", GRUPO.toString(), "totalAusentesMarcados", 5))));

        assertEquals(List.of(new TomaAsistenciaRepositoryPort.TomaCerradaAutomaticamente(TOMA, SESION, GRUPO, 5)), repository.cerrarVencidas());
        verify(query).setParameter("idCorrelacion", correlacion);
    }

    @Test
    void consultar_tomas_de_la_sesion_agrupa_la_sesion_y_omite_la_fila_sin_toma() {
        final Map<String, Object> sesion = new HashMap<>(Map.of("idSesion", SESION.toString(), "idGrupo", GRUPO.toString(),
                "horasSesion", new BigDecimal("2.000000"), "estaSesionCancelada", false,
                "ventanaAperturaUtc", Timestamp.valueOf("2026-11-07 12:45:00"), "ventanaCierreUtc", Timestamp.valueOf("2026-11-07 15:30:00"),
                "ventanaAbierta", true, "maximoTomas", 4));
        final Map<String, Object> conToma = new HashMap<>(sesion);
        conToma.putAll(Map.of("idToma", TOMA.toString(), "numero", (short) 1, "codigoModo", "MANUAL", "codigoEstado", "CERRADA",
                "estaAbierta", false, "esImplicita", true, "fechaAperturaUtc", Timestamp.valueOf("2026-11-07 13:00:00"),
                "fechaCierreUtc", Timestamp.valueOf("2026-11-07 13:00:00"), "horasToma", new BigDecimal("1.000000")));
        conToma.putAll(Map.of("totalRegistros", 30, "totalPresentes", 28, "totalAusentes", 2, "totalExcusas", 0, "tieneQrActivo", false));
        when(query.getResultList()).thenReturn(List.of(tupla(conToma)));

        final TomasSesionResumen resumen = repository.consultarTomasSesion(SESION, USUARIO);

        assertEquals(new BigDecimal("2.00"), resumen.horasSesion());
        assertEquals(Instant.parse("2026-11-07T12:45:00Z"), resumen.ventanaAperturaUtc());
        assertEquals(4, resumen.maximoTomas());
        final TomaAsistenciaResumen toma = resumen.tomas().getFirst();
        assertEquals(new BigDecimal("1.00"), toma.horasToma());
        assertTrue(toma.esImplicita());
        assertEquals(28, toma.totalPresentes());

        when(query.getResultList()).thenReturn(List.of(tupla(sesion)));
        assertTrue(repository.consultarTomasSesion(SESION, USUARIO).tomas().isEmpty());

        when(query.getResultList()).thenReturn(List.of());
        assertThrows(DatabaseOperationException.class, () -> repository.consultarTomasSesion(SESION, USUARIO));
    }

    @Test
    void consultas_paginadas_leen_por_nombre_y_el_total_de_la_primera_fila() {
        final UUID estudiante = UUID.randomUUID();
        final Map<String, Object> asistencia = new HashMap<>();
        asistencia.put("idEstudiante", estudiante.toString());
        asistencia.put("nombreCompletoEstudiante", "Ana Perez");
        asistencia.put("codigoRazonCausa", null);
        asistencia.put("registradoPorEstudiante", null);
        asistencia.put("fechaRegistroUtc", null);
        asistencia.put("totalRegistros", 31);
        when(query.getResultList()).thenReturn(List.of(tupla(asistencia)));

        final PaginaResultado<AsistenciaTomaFila> pagina = repository.consultarAsistencias(TOMA, 1, 50, USUARIO);
        assertEquals(31, pagina.totalElementos());
        assertEquals(new AsistenciaTomaFila(estudiante, "Ana Perez", null, false, null), pagina.elementos().getFirst());

        when(query.getResultList()).thenReturn(List.of(tupla(Map.of("idEstudiante", estudiante.toString(),
                "nombreCompletoEstudiante", "Ana Perez", "tieneRegistro", true, "codigoRazonCausa", "SJC",
                "fueMarcadoAlCerrar", 1, "totalRegistros", 1))));
        final PaginaResultado<FaltanteTomaFila> faltantes = repository.consultarFaltantes(TOMA, 2, 10, USUARIO);
        assertEquals(new FaltanteTomaFila(estudiante, "Ana Perez", true, "SJC", true), faltantes.elementos().getFirst());
        assertEquals(2, faltantes.numeroPagina());
        verify(query).setParameter("numeroPagina", 2);

        when(query.getResultList()).thenReturn(List.of());
        assertEquals(0, repository.consultarFaltantes(TOMA, 1, 50, USUARIO).totalElementos());
    }

    @Test
    void los_rechazos_de_ambito_de_las_consultas_se_traducen() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_002|ajena"));
        assertThrows(ForbiddenException.class, () -> repository.consultarAsistencias(TOMA, 1, 50, USUARIO));
        assertThrows(ForbiddenException.class, () -> repository.consultarTomasSesion(SESION, USUARIO));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultar_toma_mapea_la_entidad_de_la_vista() {
        final TypedQuery<UvTomaAsistenciaEntity> hql = mock(TypedQuery.class);
        when(entityManager.createQuery(TomaAsistenciaJpaRepository.HQL_TOMA, UvTomaAsistenciaEntity.class)).thenReturn(hql);
        when(hql.setParameter(anyString(), any())).thenReturn(hql);
        when(hql.getResultList()).thenReturn(List.of());

        assertEquals(Optional.empty(), repository.consultarToma(TOMA));
        assertFalse(TomaAsistenciaJpaRepository.serializar(List.of()).contains("idEstudiante"));
        assertThrows(NullPointerException.class, () -> new TomaAsistenciaJpaRepository(null, executor));
        assertThrows(NullPointerException.class, () -> new TomaAsistenciaJpaRepository(entityManager, null));
    }
}
