package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 B.2: resumen por horas leido por nombre de columna (28 columnas de la vista + nombre + total). */
class ResumenAsistenciaJpaRepositoryTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final ResumenAsistenciaJpaRepository repository = new ResumenAsistenciaJpaRepository(entityManager);

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(UUID.randomUUID());
        when(entityManager.createNativeQuery(anyString(), eq(Tuple.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Map<String, Object> fila() {
        final Map<String, Object> f = new HashMap<>();
        f.put("idGrupo", GRUPO.toString());
        f.put("idEstudiante", ESTUDIANTE.toString());
        f.put("codigoEstadoEstudiante", "A");
        f.put("sesionesEjecutadas", 10);
        f.put("horasDictadas", new BigDecimal("20.000000"));
        f.put("horasAsistidas", new BigDecimal("16.000000"));
        f.put("horasFalta", new BigDecimal("2.666667"));
        f.put("horasExcusa", new BigDecimal("1.333333"));
        f.put("horasSinRegistro", BigDecimal.ZERO);
        f.put("porcentajeInasistencia", new BigDecimal("13.33"));
        f.put("porcentajeAsistenciaHoras", new BigDecimal("80.00"));
        f.put("porcentajeInasistenciaSesiones", new BigDecimal("99.99"));
        f.put("codigoPermanencia", "REGULAR");
        f.put("umbralAdvertencia", new BigDecimal("15.00"));
        f.put("umbralPerdida", new BigDecimal("20.00"));
        return f;
    }

    @Test
    void resumen_del_grupo_lee_las_horas_y_porcentajes_por_horas_con_2_decimales() {
        final Map<String, Object> f = fila();
        f.put("nombreCompletoEstudiante", "Ana Perez");
        f.put("totalRegistros", 31);
        when(query.getResultList()).thenReturn(List.of(TomaAsistenciaJpaRepositoryTest.tupla(f)));

        final PaginaResultado<ResumenAsistenciaQueryPort.FilaResumen> pagina = repository.consultarResumenGrupoDocente(GRUPO, 1, 50, USUARIO);

        assertEquals(31, pagina.totalElementos());
        assertEquals(new ResumenAsistenciaQueryPort.FilaResumen(GRUPO, ESTUDIANTE, "Ana Perez", "A", new IndicadoresAsistenciaDTO(10,
                new BigDecimal("20.00"), new BigDecimal("16.00"), new BigDecimal("2.67"), new BigDecimal("1.33"), new BigDecimal("0.00"),
                new BigDecimal("13.33"), new BigDecimal("80.00"), "REGULAR", new BigDecimal("15.00"), new BigDecimal("20.00"))),
                pagina.elementos().getFirst());
        verify(query).setParameter("idGrupo", GRUPO);

        when(query.getResultList()).thenReturn(List.of());
        assertEquals(0, repository.consultarResumenGrupoDocente(GRUPO, 1, 50, USUARIO).totalElementos());
    }

    @Test
    void resumen_del_estudiante_y_rechazo_de_ambito() {
        when(query.getResultList()).thenReturn(List.of(TomaAsistenciaJpaRepositoryTest.tupla(fila())));
        final ResumenAsistenciaQueryPort.FilaResumen resumen = repository.consultarResumenEstudiante(USUARIO).getFirst();
        assertNull(resumen.nombreCompleto());
        assertEquals(GRUPO, resumen.idGrupo());

        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_002|ajeno"));
        assertThrows(ForbiddenException.class, () -> repository.consultarResumenGrupoDocente(GRUPO, 1, 50, USUARIO));
        assertThrows(NullPointerException.class, () -> new ResumenAsistenciaJpaRepository(null));
    }
}
