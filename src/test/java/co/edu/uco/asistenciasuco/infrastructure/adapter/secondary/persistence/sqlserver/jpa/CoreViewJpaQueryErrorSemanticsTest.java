package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.*;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.CoreViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.*;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoreViewJpaQueryErrorSemanticsTest {

    @Test
    void errores_de_provider_conservan_database_operation_exception_en_las_seis_capabilities() {
        final EntityManager entityManager = mock(EntityManager.class);
        when(entityManager.createQuery(anyString(), eq(UvSesionEntity.class))).thenThrow(new PersistenceException("db"));
        // CULM-001 B.1: la lectura de sesiones se une a uv_sesion_estado y consulta Object[].
        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenThrow(new PersistenceException("db"));
        when(entityManager.createQuery(anyString(), eq(UvGrupoEntity.class))).thenThrow(new PersistenceException("db"));
        when(entityManager.createQuery(anyString(), eq(UvUsuarioEntity.class))).thenThrow(new PersistenceException("db"));
        when(entityManager.createQuery(anyString(), eq(UvDocenteIdentidadEntity.class))).thenThrow(new PersistenceException("db"));
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenThrow(new PersistenceException("db"));
        when(entityManager.createQuery(anyString(), eq(UvTipoIdentificacionEntity.class))).thenThrow(new PersistenceException("db"));

        assertThrows(DatabaseOperationException.class,
                () -> new SesionJpaRepository(entityManager, new JpaProcedureExecutor(entityManager))
                        .consultarSesionesPorGrupo(UUID.randomUUID()));
        assertThrows(DatabaseOperationException.class,
                () -> new GrupoJpaRepository(entityManager,
                        mock(org.springframework.transaction.support.TransactionOperations.class),
                        new JpaProcedureExecutor(entityManager)).consultarGrupos());
        assertThrows(DatabaseOperationException.class,
                () -> new UsuarioJpaRepository(entityManager, new JpaProcedureExecutor(entityManager))
                        .consultarUsuarioPorId(UUID.randomUUID()));
        assertThrows(DatabaseOperationException.class,
                () -> new DocenteJpaRepository(entityManager).consultarDocentes());
        assertThrows(DatabaseOperationException.class,
                () -> new EstudianteJpaRepository(entityManager).consultarEstudiantes(emptyPage()));
        assertThrows(DatabaseOperationException.class,
                () -> new TipoIdentificacionJpaRepository(entityManager).consultarTiposIdentificacion());
    }

    @Test
    void mapper_de_sesion_proyecta_el_instante_en_utc_sin_depender_del_timezone_default() {
        final TimeZone original = TimeZone.getDefault();
        final Instant instant = Instant.parse("2026-08-17T23:00:00Z");
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
            assertEquals(LocalDateTime.ofInstant(instant, ZoneOffset.UTC),
                    CoreViewJpaProjectionMapper.toUtcLocalDateTime(Date.from(instant)));
            assertEquals(LocalDateTime.ofInstant(instant.plusSeconds(3600), ZoneOffset.UTC),
                    CoreViewJpaProjectionMapper.toUtcLocalDateTime(Date.from(instant.plusSeconds(3600))));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static ConsultarEstudiantesRepositoryDTO emptyPage() {
        return new ConsultarEstudiantesRepositoryDTO(
                null, null, null, null, null, null, null, null, null, 0, 10);
    }
}




