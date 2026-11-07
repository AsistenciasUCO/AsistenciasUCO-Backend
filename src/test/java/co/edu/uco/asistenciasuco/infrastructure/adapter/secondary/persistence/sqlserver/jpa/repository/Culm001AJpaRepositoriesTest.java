package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CULM-001 A.2: adaptadores JPA nuevos o migrados a SP. A2-B-17 busqueda por documento, A2-B-13 estado del decano,
 * A2-B-15 materias y horario del estudiante por SP con el usuario ejecutor (ya no usan HQL sobre vistas), A2-B-19 correo
 * del estudiante resuelto.
 */
class Culm001AJpaRepositoriesTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    // --- busqueda por documento ---

    private GrupoJpaRepository grupos() {
        return new GrupoJpaRepository(manager, mock(TransactionOperations.class), new JpaProcedureExecutor(manager));
    }

    @Test
    void busqueda_ejecuta_el_sp_con_titularidad_y_mapea_9_columnas() {
        final UUID grupo = UUID.randomUUID();
        final UUID tipo = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{estudiante.toString(), "Ana ", null, "Perez", "Gomez",
                "ANA PEREZ GOMEZ", 1017123456, "a***@uco.edu.co", null}));

        final var encontrado = grupos().buscarEstudiantePorDocumento(grupo, tipo, 1017123456, ACTOR).orElseThrow();

        verify(manager).createNativeQuery(GrupoJpaRepository.SQL_BUSCAR_ESTUDIANTE_POR_DOCUMENTO);
        verify(query).setParameter("idGrupo", grupo);
        verify(query).setParameter("idTipoIdentificacion", tipo);
        verify(query).setParameter("numeroIdentificacion", 1017123456);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        assertEquals(estudiante, encontrado.idEstudiante());
        assertEquals("Ana", encontrado.primerNombre());
        assertNull(encontrado.segundoNombre());
        assertEquals("a***@uco.edu.co", encontrado.correoEnmascarado());
        assertNull(encontrado.codigoEstadoMatriculaGrupo());
    }

    @Test
    void busqueda_sin_filas_es_vacia_no_titular_es_403_y_columnas_distintas_fallan() {
        when(query.getResultList()).thenReturn(List.of());
        assertTrue(grupos().buscarEstudiantePorDocumento(UUID.randomUUID(), UUID.randomUUID(), 123456, ACTOR).isEmpty());

        when(query.getResultList()).thenThrow(new PersistenceException("x", new RuntimeException("DBCODE=SEC_002|no titular")));
        assertThrows(ForbiddenException.class,
                () -> grupos().buscarEstudiantePorDocumento(UUID.randomUUID(), UUID.randomUUID(), 123456, ACTOR));

        final Query corta = mock(Query.class, RETURNS_SELF);
        when(corta.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[8]));
        when(manager.createNativeQuery(GrupoJpaRepository.SQL_BUSCAR_ESTUDIANTE_POR_DOCUMENTO)).thenReturn(corta);
        assertInstanceOf(IllegalStateException.class, assertThrows(DatabaseOperationException.class,
                () -> grupos().buscarEstudiantePorDocumento(UUID.randomUUID(), UUID.randomUUID(), 123456, ACTOR)).getCause());
    }

    @Test
    void busqueda_y_consultas_de_grupo_sin_datos_obligatorios_no_llegan_a_la_db() {
        final GrupoJpaRepository repository = grupos();
        assertThrows(CrosscuttingException.class, () -> repository.buscarEstudiantePorDocumento(null, UUID.randomUUID(), 1, ACTOR));
        assertThrows(CrosscuttingException.class, () -> repository.buscarEstudiantePorDocumento(UUID.randomUUID(), null, 1, ACTOR));
        assertThrows(CrosscuttingException.class, () -> repository.buscarEstudiantePorDocumento(UUID.randomUUID(), UUID.randomUUID(), null, ACTOR));
        assertThrows(CrosscuttingException.class, () -> repository.buscarEstudiantePorDocumento(UUID.randomUUID(), UUID.randomUUID(), 1, null));
        assertThrows(CrosscuttingException.class, () -> repository.consultarGrupos(null));
        assertThrows(CrosscuttingException.class, () -> repository.consultarEstudiantesGrupo(null, ACTOR));
        assertThrows(CrosscuttingException.class, () -> repository.consultarEstudiantesGrupo(UUID.randomUUID(), null));
        verify(manager, never()).createNativeQuery(anyString());
    }

    @Test
    void estudiantes_del_grupo_fuera_de_ambito_son_403_y_grupos_sin_filas_es_lista_vacia() {
        when(query.getResultList()).thenReturn(List.of());
        assertTrue(grupos().consultarGrupos(ACTOR).isEmpty());

        when(query.getResultList()).thenThrow(new PersistenceException("x", new RuntimeException("DBCODE=SEC_002|ajeno")));
        assertThrows(ForbiddenException.class, () -> grupos().consultarEstudiantesGrupo(UUID.randomUUID(), ACTOR));
    }

    @Test
    void filas_de_grupo_y_de_estudiante_con_otra_cantidad_de_columnas_fallan() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[12]));
        assertThrows(DatabaseOperationException.class, () -> grupos().consultarGrupos(ACTOR));
        assertThrows(DatabaseOperationException.class, () -> grupos().consultarEstudiantesGrupo(UUID.randomUUID(), ACTOR));
    }

    // --- estado del decano ---

    @Test
    void cambiar_estado_decano_envia_nulo_para_invertir_y_lee_las_3_columnas_extra() {
        final UUID decano = UUID.randomUUID();
        final UUID usuario = UUID.randomUUID();
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{CORRELACION, "Se actualizo el estado del decano.", "DEC_003",
                true, usuario.toString(), 1, Boolean.FALSE}));

        final var resultado = new DecanoJpaRepository(manager, new JpaProcedureExecutor(manager)).cambiarEstadoDecano(decano, null, ACTOR);

        verify(manager).createNativeQuery(DecanoJpaRepository.SQL_CAMBIAR_ESTADO_DECANO);
        verify(query).setParameter("idDecano", decano);
        verify(query).setParameter("estaActivo", null);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        assertEquals(usuario, resultado.idUsuario());
        assertTrue(resultado.personaSinRolesActivos());
        assertFalse(resultado.estaActivoDecano());
    }

    @Test
    void cambiar_estado_decano_explicito_y_errores_de_contrato() {
        final DecanoJpaRepository repository = new DecanoJpaRepository(manager, new JpaProcedureExecutor(manager));
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{CORRELACION, "ok", "DEC_003", true, UUID.randomUUID(), 0, 1}));
        assertTrue(repository.cambiarEstadoDecano(UUID.randomUUID(), Boolean.TRUE, ACTOR).estaActivoDecano());
        verify(query).setParameter("estaActivo", "1");
        repository.cambiarEstadoDecano(UUID.randomUUID(), Boolean.FALSE, ACTOR);
        verify(query).setParameter("estaActivo", "0");

        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{CORRELACION, "ok", "DEC_003", true, null, 0, 1}));
        assertThrows(DatabaseOperationException.class, () -> repository.cambiarEstadoDecano(UUID.randomUUID(), null, ACTOR));
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{CORRELACION, "ok", "DEC_003", true, "no-uuid", 0, 1}));
        assertThrows(DatabaseOperationException.class, () -> repository.cambiarEstadoDecano(UUID.randomUUID(), null, ACTOR));
    }

    @Test
    void cambiar_estado_decano_traduce_dec_002_a_409_y_dec_001_a_404() {
        final DecanoJpaRepository repository = new DecanoJpaRepository(manager, new JpaProcedureExecutor(manager));
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{CORRELACION, "No se puede desactivar.",
                "DBCODE=DEC_002|programas activos", false, null, null, null}));
        assertThrows(ConflictException.class, () -> repository.cambiarEstadoDecano(UUID.randomUUID(), null, ACTOR));

        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{CORRELACION, "No existe.",
                "DBCODE=DEC_001|fuera de ambito", false, null, null, null}));
        assertThrows(ResourceNotFoundException.class, () -> repository.cambiarEstadoDecano(UUID.randomUUID(), null, ACTOR));
    }

    // --- materias y horario del estudiante ---

    @Test
    void materias_del_estudiante_por_sp_con_el_usuario_ejecutor() {
        final UUID asignatura = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[]{asignatura, "Algebra ", grupo.toString(), "G1"}));

        final var materias = new MateriaEstudianteJpaRepository(manager).consultarMateriasEstudiante(ACTOR);

        verify(manager).createNativeQuery(MateriaEstudianteJpaRepository.SQL_MATERIAS);
        assertTrue(MateriaEstudianteJpaRepository.SQL_MATERIAS.contains("usp_consultar_materias_estudiante"));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(manager, never()).createQuery(anyString(), any(Class.class));
        assertEquals(asignatura, materias.getFirst().idAsignatura());
        assertEquals("Algebra", materias.getFirst().nombreAsignatura());
        assertEquals(grupo, materias.getFirst().idGrupo());
    }

    @Test
    void horario_del_estudiante_por_sp_con_el_usuario_ejecutor() {
        final UUID estudiante = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.<Object>of(
                new Object[]{UUID.randomUUID(), estudiante, UUID.randomUUID(), "MAT1", "Algebra", "G1", "LU", "08:00", LocalTime.of(10, 0), "Doc"},
                new Object[]{UUID.randomUUID(), estudiante, UUID.randomUUID(), "MAT2", "Fisica", "G2", "MA", null, "12:00:00.0000000", null}));

        final var horario = new HorarioEstudianteJpaRepository(manager).consultarHorarioEstudiante(ACTOR);

        verify(manager).createNativeQuery(HorarioEstudianteJpaRepository.SQL_HORARIO);
        assertTrue(HorarioEstudianteJpaRepository.SQL_HORARIO.contains("usp_consultar_horario_estudiante"));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(manager, never()).createQuery(anyString(), any(Class.class));
        assertEquals(LocalTime.of(8, 0), horario.getFirst().horaInicio());
        assertEquals(LocalTime.of(10, 0), horario.getFirst().horaFin());
        assertNull(horario.get(1).horaInicio());
        assertEquals(LocalTime.of(12, 0), horario.get(1).horaFin());
        assertNull(horario.get(1).docente());
    }

    @Test
    void materias_y_horario_sin_ejecutor_no_llegan_a_la_db_y_columnas_distintas_fallan() {
        assertThrows(CrosscuttingException.class, () -> new MateriaEstudianteJpaRepository(manager).consultarMateriasEstudiante(null));
        assertThrows(CrosscuttingException.class, () -> new HorarioEstudianteJpaRepository(manager).consultarHorarioEstudiante(null));
        verify(manager, never()).createNativeQuery(anyString());

        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[3]));
        assertThrows(DatabaseOperationException.class, () -> new MateriaEstudianteJpaRepository(manager).consultarMateriasEstudiante(ACTOR));
        assertThrows(DatabaseOperationException.class, () -> new HorarioEstudianteJpaRepository(manager).consultarHorarioEstudiante(ACTOR));
        assertThrows(NullPointerException.class, () -> new MateriaEstudianteJpaRepository(null));
        assertThrows(NullPointerException.class, () -> new HorarioEstudianteJpaRepository(null));
    }

    // --- correo del estudiante resuelto (uso interno de la matricula) ---

    @Test
    @SuppressWarnings("unchecked")
    void correo_por_estudiante_lee_el_correo_del_usuario_del_estudiante() {
        final TypedQuery<String> correo = mock(TypedQuery.class, RETURNS_SELF);
        when(manager.createQuery(eq(UsuarioJpaRepository.HQL_CORREO_POR_ESTUDIANTE), eq(String.class))).thenReturn(correo);
        final UUID estudiante = UUID.randomUUID();
        when(correo.getResultList()).thenReturn(List.of("ana.perez@uco.edu.co"), List.of(" "));
        final UsuarioJpaRepository repository = new UsuarioJpaRepository(manager, new JpaProcedureExecutor(manager));

        assertEquals("ana.perez@uco.edu.co", repository.consultarCorreoPorEstudiante(estudiante).orElseThrow());
        verify(correo).setParameter("idEstudiante", estudiante);
        assertTrue(repository.consultarCorreoPorEstudiante(estudiante).isEmpty());
        assertTrue(repository.consultarCorreoPorEstudiante(null).isEmpty());
    }
}
