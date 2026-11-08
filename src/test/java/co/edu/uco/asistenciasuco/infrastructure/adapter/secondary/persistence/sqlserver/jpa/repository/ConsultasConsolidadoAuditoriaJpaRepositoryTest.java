package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroAuditoria;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsolidado;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU061/HU172/HU147: native EXEC del consolidado, la auditoria y los componentes. */
class ConsultasConsolidadoAuditoriaJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final OffsetDateTime DESDE = OffsetDateTime.parse("2026-11-01T00:00Z");
    private static final OffsetDateTime HASTA = OffsetDateTime.parse("2026-11-04T00:00Z");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private ConsultasTransversalesJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new ConsultasTransversalesJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void T_061_PAT_01_consolidado_enlaza_filtros_y_mapea_metricas() {
        when(query.getResultList()).thenReturn(filas(new Object[]{ID.toString(), 1, "G1", ID, "2026-2", ID,
                "Sistemas", ID, "Calculo", ID, "Ana Docente", 20, 8, 150, 10, 0, new BigDecimal("93.75"), 6.25,
                2, 1, 12}));

        final var pagina = repository.consultarConsolidadoAsistencia(EJECUTOR, new FiltroConsolidado(ID, null, ID, null),
                1, 10);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_consolidado_asistencia_grupos_paginado"));
        verify(query).setParameter("idPeriodoAcademico", ID);
        verify(query).setParameter("idPrograma", null);
        verify(query).setParameter("idAsignatura", ID);
        verify(query).setParameter("idDocente", null);
        assertEquals(12, pagina.total());
        final var fila = pagina.elementos().getFirst();
        assertEquals(ID, fila.idGrupo());
        assertEquals(20, fila.estudiantesActivos());
        assertEquals(new BigDecimal("93.75"), fila.porcentajeAsistencia());
        assertEquals(new BigDecimal("6.25"), fila.porcentajeInasistencia());
        assertEquals(1, fila.estudiantesPerdida());
    }

    @Test
    void T_172_PAT_01_auditoria_enlaza_rango_utc_y_mapea_evento() {
        when(query.getResultList()).thenReturn(filas(new Object[]{ID, HASTA, EJECUTOR.toString(), "USER", "Ana",
                "CREAR_SESION", "SESION", "S1", "SUCCESS", CORRELATION.toString(), "POST", "/api/v1/sesiones",
                (short) 201, null, null, null, true, 1}));

        final var pagina = repository.consultarAuditoria(EJECUTOR, new FiltroAuditoria(DESDE, HASTA, "CREAR_SESION",
                null, null, null, "SUCCESS", true, null, false), 1, 10);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_auditoria_eventos_paginado"));
        verify(query).setParameter("desdeUtc", DESDE);
        verify(query).setParameter("hastaUtc", HASTA);
        verify(query).setParameter("soloAsistencia", true);
        assertEquals(1, pagina.total());
        final var evento = pagina.elementos().getFirst();
        assertEquals(HASTA, evento.ocurridoEn());
        assertEquals(CORRELATION, evento.idTraza());
        assertEquals(201, evento.estadoHttp());
        assertTrue(evento.accionAsistencia());
    }

    @Test
    void T_172_PAT_02_fecha_hora_acepta_los_tipos_del_driver() {
        final Instant instante = Instant.parse("2026-11-04T10:15:30Z");
        assertNull(ConsultasTransversalesJpaRepository.fechaHora(null));
        assertEquals(instante, ConsultasTransversalesJpaRepository.fechaHora(instante).toInstant());
        assertEquals(instante, ConsultasTransversalesJpaRepository.fechaHora(LocalDateTime.ofInstant(instante, ZoneOffset.UTC)).toInstant());
        assertEquals(OffsetDateTime.parse("2026-11-04T10:15:30.1234567-05:00"),
                ConsultasTransversalesJpaRepository.fechaHora("2026-11-04 10:15:30.1234567 -05:00"));
        assertEquals(instante, ConsultasTransversalesJpaRepository.fechaHora("2026-11-04 10:15:30 +00:00").toInstant());
    }

    @Test
    void T_147_PAT_01_componentes_mapea_codigo_y_total() {
        when(query.getResultList()).thenReturn(filas(new Object[]{ID.toString(), "BAS", "Basicas", 3},
                new Object[]{ID, "ESP ", "Especificas", null}));

        final var componentes = repository.consultarComponentes(EJECUTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_componentes_formacion"));
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        assertEquals(2, componentes.size());
        assertEquals("ESP", componentes.get(1).codigo());
        assertEquals(0, componentes.get(1).totalAsignaturas());
    }

    @Test
    void T_172_ERR_01_sec_001_es_403() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_001|perfil requerido"));

        assertThrows(ForbiddenException.class, () -> repository.consultarAuditoria(EJECUTOR,
                new FiltroAuditoria(DESDE, HASTA, null, null, null, null, null, true, null, false), 1, 10));
    }

    private static List<Object> filas(final Object[]... filas) {
        return new ArrayList<>(List.of((Object[]) filas));
    }

    @Test
    void T_155_PAT_01_estados_lee_las_dos_vistas_de_catalogo() {
        when(query.getResultList()).thenReturn(filas(new Object[]{"SOLICITUD", ID.toString(), "P", "pendiente"},
                new Object[]{"ESTUDIANTE_GRUPO", ID, "A ", "activo"}));

        final var estados = repository.consultarEstadosSistema();

        verify(entityManager).createNativeQuery(contains("dbo.uv_estado_estudiante_grupo"));
        assertEquals(2, estados.size());
        assertEquals(ID, estados.getFirst().id());
        assertEquals("A", estados.get(1).codigo());
    }

    @Test
    void T_005_PAT_01_categorias_leen_la_vista_del_catalogo() {
        when(query.getResultList()).thenReturn(filas(new Object[]{ID.toString(), "MED", "Médico / Salud", "Enfermedad"},
                new Object[]{ID, "OTR ", "Otro", null}));

        final var categorias = repository.consultarCategoriasJustificacion();

        verify(entityManager).createNativeQuery(contains("dbo.uv_categoria_justificacion"));
        assertEquals(2, categorias.size());
        assertEquals(ID, categorias.getFirst().id());
        assertEquals("Médico / Salud", categorias.getFirst().nombre());
        assertEquals("OTR", categorias.get(1).codigo());
        assertEquals(null, categorias.get(1).descripcion());
    }
}
