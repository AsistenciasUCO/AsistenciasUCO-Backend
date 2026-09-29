package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GrupoJpaCommandPersistenceTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID TIPO_ID = UUID.randomUUID();
    private static final UUID USUARIO_EJECUTOR = UUID.randomUUID();

    private final EntityManagerFactory factory = mock(EntityManagerFactory.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final StoredProcedureQuery procedureQuery = mock(StoredProcedureQuery.class);
    private final GrupoJpaCommandPersistence persistence = new GrupoJpaCommandPersistence(factory);

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    private void wireProcedure() {
        when(factory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.createStoredProcedureQuery("dbo.usp_registrar_estudiante_en_grupo"))
                .thenReturn(procedureQuery);
        when(procedureQuery.registerStoredProcedureParameter(anyString(), any(), any(ParameterMode.class)))
                .thenReturn(procedureQuery);
        when(procedureQuery.setParameter(anyString(), any())).thenReturn(procedureQuery);
    }

    @Test
    void dto_nulo_lanza_excepcion_sin_abrir_entity_manager() {
        final CrosscuttingException exception = assertThrows(
                CrosscuttingException.class,
                () -> persistence.registrarEstudianteEnGrupo(null)
        );
        assertEquals("El DTO para registrar estudiante en grupo es obligatorio.", exception.getMessage());
        verifyNoInteractions(factory);
    }

    @Test
    void constructor_exige_entity_manager_factory() {
        assertThrows(NullPointerException.class, () -> new GrupoJpaCommandPersistence(null));
    }

    @Test
    void registra_parametros_ejecuta_valida_correlacion_y_retorna_proyeccion() {
        wireProcedure();
        CorrelationIdContext.set(CORRELACION);

        final Object[] row = new Object[]{
                CORRELACION,
                "Estudiante registrado exitosamente en el grupo.",
                "OPERACION_EXITOSA",
                true
        };
        when(procedureQuery.getResultList()).thenReturn(List.of((Object) row));

        final RegistrarEstudianteRepositoryDTO dto = new RegistrarEstudianteRepositoryDTO(
                TIPO_ID,
                123456789,
                "PEREZ",
                "GOMEZ",
                "ANA",
                "MARIA",
                "ana.perez@uco.edu.co",
                "ClaveSegura123!",
                GRUPO,
                USUARIO_EJECUTOR
        );

        final RegistrarEstudianteRepositoryProjection resultado = persistence.registrarEstudianteEnGrupo(dto);

        assertNotNull(resultado);
        assertEquals("Estudiante registrado exitosamente en el grupo.", resultado.getMensajeUsuario());

        verify(procedureQuery).registerStoredProcedureParameter("idGrupo", UUID.class, ParameterMode.IN);
        verify(procedureQuery).registerStoredProcedureParameter("numeroIdentificacion", Integer.class, ParameterMode.IN);
        verify(procedureQuery).registerStoredProcedureParameter("primerNombre", String.class, ParameterMode.IN);
        verify(procedureQuery).registerStoredProcedureParameter("segundoNombre", String.class, ParameterMode.IN);
        verify(procedureQuery).registerStoredProcedureParameter("primerApellido", String.class, ParameterMode.IN);
        verify(procedureQuery).registerStoredProcedureParameter("segundoApellido", String.class, ParameterMode.IN);
        verify(procedureQuery).registerStoredProcedureParameter("correo", String.class, ParameterMode.IN);
        verify(procedureQuery).registerStoredProcedureParameter("password", String.class, ParameterMode.IN);
        verify(procedureQuery).registerStoredProcedureParameter("idCorrelacion", UUID.class, ParameterMode.IN);
        verify(procedureQuery).registerStoredProcedureParameter("idUsuarioEjecutor", UUID.class, ParameterMode.IN);
        verify(procedureQuery).registerStoredProcedureParameter("idTipoIdIdentificacion", UUID.class, ParameterMode.IN);

        verify(procedureQuery).setParameter("idGrupo", GRUPO);
        verify(procedureQuery).setParameter("numeroIdentificacion", 123456789);
        verify(procedureQuery).setParameter("primerNombre", "ANA");
        verify(procedureQuery).setParameter("segundoNombre", "MARIA");
        verify(procedureQuery).setParameter("primerApellido", "PEREZ");
        verify(procedureQuery).setParameter("segundoApellido", "GOMEZ");
        verify(procedureQuery).setParameter("correo", "ana.perez@uco.edu.co");
        verify(procedureQuery).setParameter("password", "ClaveSegura123!");
        verify(procedureQuery).setParameter("idCorrelacion", CORRELACION);
        verify(procedureQuery).setParameter("idUsuarioEjecutor", USUARIO_EJECUTOR);
        verify(procedureQuery).setParameter("idTipoIdIdentificacion", TIPO_ID);

        verify(entityManager).close();
    }

    @Test
    void persistence_exception_se_traduce_a_database_operation_exception() {
        wireProcedure();
        CorrelationIdContext.set(CORRELACION);
        when(procedureQuery.getResultList()).thenThrow(new PersistenceException("Fallo conexion DB"));

        final RegistrarEstudianteRepositoryDTO dto = new RegistrarEstudianteRepositoryDTO(
                TIPO_ID, 123456789, "PEREZ", null, "ANA", null, "ana@uco.edu.co", null, GRUPO, USUARIO_EJECUTOR
        );

        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> persistence.registrarEstudianteEnGrupo(dto)
        );

        assertEquals("No fue posible registrar el estudiante en el grupo.", exception.getMessage());
        verify(entityManager).close();
    }
}
