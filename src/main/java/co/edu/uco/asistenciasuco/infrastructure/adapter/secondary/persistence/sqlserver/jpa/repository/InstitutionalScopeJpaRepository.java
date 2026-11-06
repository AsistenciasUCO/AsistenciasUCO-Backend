package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.InstitutionalIdentityAmbiguityException;
import jakarta.persistence.EntityManager;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Lecturas JPA-only del alcance institucional (LB-008 JPA-05). Solo cambia la tecnologia de lectura:
 * las reglas RBAC y de ownership siguen en Application/Infrastructure segun el contrato previo.
 *
 * <p>Semantica conservada del SQL original: un id ausente produce conteo 0 igual que la comparacion con NULL en SQL.
 * Las decisiones de ownership docente y estudiante ({@link #canDocenteAccessGrupo}, {@link #canEstudianteAccessGrupo})
 * se resuelven en UNA sentencia nativa con la subconsulta {@code (SELECT TOP 1 ...)} original, para que la
 * decision no tenga ventana TOCTOU entre lecturas. El resto de lecturas de alcance usa JPQL.</p>
 */
@Repository
public class InstitutionalScopeJpaRepository implements InstitutionalScopePort {

    static final String HQL_USUARIO_POR_ID = """
            select u.id from UvUsuarioEntity u where u.id = :usuarioId
            """;
    static final String HQL_USUARIO_POR_EMAIL = """
            select u.id from UvUsuarioEntity u where lower(u.correo) = lower(:email)
            """;
    static final String HQL_USUARIO_ACTIVO = """
            select u.estaActivoUsuario from UvUsuarioEntity u where u.id = :usuarioId
            """;
    static final String HQL_DOCENTE_POR_USUARIO = """
            select d.id from UvDocenteIdentidadEntity d where d.idUsuario = :usuarioId
            """;
    static final String HQL_ESTUDIANTE_POR_USUARIO = """
            select e.id from UvEstudianteIdentidadEntity e where e.idUsuario = :usuarioId
            """;
    static final String HQL_COORDINADOR_POR_USUARIO = """
            select c.id from UvCoordinadorIdentidadEntity c where c.idUsuario = :usuarioId
            """;
    static final String HQL_DECANO_POR_USUARIO = """
            select d.id from UvDecanoIdentidadEntity d where d.idUsuario = :usuarioId
            """;
    /**
     * La regla 1 a 1 (PROG_005) impide hoy dos programas por coordinador, pero una base con datos anteriores a la regla
     * puede tenerlos (certificación Fase 3). Para que todas las pantallas del coordinador resuelvan el MISMO programa,
     * se prefiere el programa activo y se ordena de forma estable.
     */
    static final String HQL_PROGRAMA_POR_COORDINADOR = """
            select c.idPrograma from UvCoordinadorEntity c, UvProgramaEntity p
            where p.id = c.idPrograma and c.idUsuario = :usuarioId and c.estaActivoCoordinador = 1
            order by case when p.estaActivoPrograma = true then 0 else 1 end, p.nombrePrograma, c.idPrograma
            """;
    static final String HQL_FACULTAD_POR_DECANO = """
            select d.idFacultad from UvDecanoEntity d
            where d.idUsuario = :usuarioId and d.estaActivoDecano = 1
            """;
    static final String SQL_DOCENTE_ACCEDE_GRUPO = """
            SELECT COUNT(1)
            FROM dbo.uv_grupo
            WHERE id = :grupoId
              AND idDocente = (
                  SELECT TOP 1 id FROM dbo.uv_docente_identidad WHERE idUsuario = :usuarioId
              )
            """;
    static final String SQL_ESTUDIANTE_ACCEDE_GRUPO = """
            SELECT COUNT(1)
            FROM dbo.uv_estudiante_grupo
            WHERE idGrupo = :grupoId
              AND idEstudiante = (
                  SELECT TOP 1 id FROM dbo.uv_estudiante_identidad WHERE idUsuario = :usuarioId
              )
            """;
    static final String SQL_COORDINADOR_ACCEDE_GRUPO = """
            SELECT COUNT(1)
            FROM dbo.uv_grupo_programa gp
            WHERE gp.idGrupo = :grupoId
              AND EXISTS (
                  SELECT 1 FROM dbo.uv_coordinador c
                  WHERE c.idUsuario = :usuarioId AND c.idPrograma = gp.idPrograma AND c.estaActivoCoordinador = 1
              )
            """;
    static final String SQL_DECANO_ACCEDE_GRUPO = """
            SELECT COUNT(1)
            FROM dbo.uv_grupo_programa gp
            JOIN dbo.uv_programa p ON p.id = gp.idPrograma
            WHERE gp.idGrupo = :grupoId
              AND EXISTS (
                  SELECT 1 FROM dbo.uv_decano d
                  WHERE d.idUsuario = :usuarioId AND d.idFacultad = p.idFacultad AND d.estaActivoDecano = 1
              )
            """;
    static final String SQL_ROLES_ACTIVOS = """
            SELECT rol FROM (
                SELECT 'ADMINISTRADOR' AS rol FROM dbo.uv_administrador WHERE idUsuario = :usuarioId AND estaActivoUsuario = 1
                UNION ALL
                SELECT 'DECANO' FROM dbo.uv_decano WHERE idUsuario = :usuarioId AND estaActivoUsuario = 1 AND estaActivoDecano = 1
                UNION ALL
                SELECT 'COORDINADOR' FROM dbo.uv_coordinador WHERE idUsuario = :usuarioId AND estaActivoUsuario = 1 AND estaActivoCoordinador = 1
                UNION ALL
                SELECT 'DOCENTE' FROM dbo.uv_docente_identidad WHERE idUsuario = :usuarioId AND estaActivoUsuario = 1
                UNION ALL
                SELECT 'ESTUDIANTE' FROM dbo.uv_estudiante_identidad WHERE idUsuario = :usuarioId AND estaActivoUsuario = 1
            ) AS roles
            """;
    static final String SQL_ADMINISTRADOR_ACTIVO = """
            SELECT COUNT(1) FROM dbo.uv_administrador WHERE idUsuario = :usuarioId AND estaActivoUsuario = 1
            """;
    static final String SQL_GESTIONA_CUENTA = """
            SELECT COUNT(1) WHERE
                EXISTS (SELECT 1 FROM dbo.uv_administrador a WHERE a.idUsuario = :actorId AND a.estaActivoUsuario = 1)
                OR EXISTS (
                    SELECT 1 FROM dbo.uv_decano d
                    WHERE d.idUsuario = :actorId AND d.estaActivoDecano = 1 AND (
                        EXISTS (SELECT 1 FROM dbo.uv_coordinador c WHERE c.idUsuario = :destinoId AND c.idFacultad = d.idFacultad)
                        OR EXISTS (SELECT 1 FROM dbo.uv_docente_programa dp WHERE dp.idUsuario = :destinoId AND dp.idFacultad = d.idFacultad)
                        OR EXISTS (
                            SELECT 1 FROM dbo.uv_estudiante_programa ep
                            JOIN dbo.uv_estudiante_identidad ei ON ei.id = ep.idEstudiante
                            JOIN dbo.uv_programa p ON p.id = ep.idPrograma
                            WHERE ei.idUsuario = :destinoId AND p.idFacultad = d.idFacultad)))
                OR EXISTS (
                    SELECT 1 FROM dbo.uv_coordinador cc
                    WHERE cc.idUsuario = :actorId AND cc.estaActivoCoordinador = 1 AND (
                        EXISTS (SELECT 1 FROM dbo.uv_docente_programa dp WHERE dp.idUsuario = :destinoId AND dp.idPrograma = cc.idPrograma)
                        OR EXISTS (
                            SELECT 1 FROM dbo.uv_estudiante_programa ep
                            JOIN dbo.uv_estudiante_identidad ei ON ei.id = ep.idEstudiante
                            WHERE ei.idUsuario = :destinoId AND ep.idPrograma = cc.idPrograma)))
            """;
    static final String HQL_COORDINADOR_DE_PROGRAMA = """
            select count(c) from UvCoordinadorEntity c
            where c.idUsuario = :usuarioId and c.idPrograma = :programaId and c.estaActivoCoordinador = 1
            """;
    static final String HQL_DECANO_DE_FACULTAD = """
            select count(d) from UvDecanoEntity d
            where d.idUsuario = :usuarioId and d.idFacultad = :facultadId and d.estaActivoDecano = 1
            """;
    static final String HQL_DECANO_DE_PROGRAMA = """
            select count(p) from UvProgramaEntity p, UvDecanoEntity d
            where p.id = :programaId and p.idFacultad = d.idFacultad
              and d.idUsuario = :usuarioId and d.estaActivoDecano = 1
            """;

    private final EntityManager entityManager;

    public InstitutionalScopeJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de alcance institucional es obligatorio.");
    }

    @Override
    public Optional<UUID> findUsuarioIdById(final UUID usuarioId) {
        return singleUuid(HQL_USUARIO_POR_ID, "usuarioId", usuarioId, "findUsuarioIdById");
    }

    @Override
    public Optional<UUID> findUsuarioIdByEmail(final String email) {
        return uniqueUuidByEmail(email);
    }

    @Override
    public boolean isUsuarioActivo(final UUID usuarioId) {
        return JpaQueryExecutor.execute(
                "isUsuarioActivo",
                "No fue posible validar el estado del usuario institucional.",
                () -> {
                    final List<Boolean> rows = entityManager.createQuery(HQL_USUARIO_ACTIVO, Boolean.class)
                            .setParameter("usuarioId", usuarioId)
                            .setMaxResults(1)
                            .getResultList();
                    return !rows.isEmpty() && Boolean.TRUE.equals(rows.getFirst());
                }
        );
    }

    @Override
    public Optional<UUID> findDocenteIdByUsuario(final UUID usuarioId) {
        return singleUuid(HQL_DOCENTE_POR_USUARIO, "usuarioId", usuarioId, "findDocenteIdByUsuario");
    }

    @Override
    public Optional<UUID> findEstudianteIdByUsuario(final UUID usuarioId) {
        return singleUuid(HQL_ESTUDIANTE_POR_USUARIO, "usuarioId", usuarioId, "findEstudianteIdByUsuario");
    }

    @Override
    public Optional<UUID> findProgramaIdByCoordinadorUsuario(final UUID usuarioId) {
        return singleUuid(HQL_PROGRAMA_POR_COORDINADOR, "usuarioId", usuarioId, "findProgramaIdByCoordinadorUsuario");
    }

    @Override
    public Optional<UUID> findCoordinadorIdByUsuario(final UUID usuarioId) {
        return singleUuid(HQL_COORDINADOR_POR_USUARIO, "usuarioId", usuarioId, "findCoordinadorIdByUsuario");
    }

    @Override
    public Optional<UUID> findFacultadIdByDecanoUsuario(final UUID usuarioId) {
        return singleUuid(HQL_FACULTAD_POR_DECANO, "usuarioId", usuarioId, "findFacultadIdByDecanoUsuario");
    }

    @Override
    public Optional<UUID> findDecanoIdByUsuario(final UUID usuarioId) {
        return singleUuid(HQL_DECANO_POR_USUARIO, "usuarioId", usuarioId, "findDecanoIdByUsuario");
    }

    @Override
    public boolean canDocenteAccessGrupo(final UUID usuarioId, final UUID grupoId) {
        return nativeCount(SQL_DOCENTE_ACCEDE_GRUPO, "canDocenteAccessGrupo",
                new Parameter("grupoId", grupoId), new Parameter("usuarioId", usuarioId)) > 0;
    }

    @Override
    public boolean canEstudianteAccessGrupo(final UUID usuarioId, final UUID grupoId) {
        return nativeCount(SQL_ESTUDIANTE_ACCEDE_GRUPO, "canEstudianteAccessGrupo",
                new Parameter("grupoId", grupoId), new Parameter("usuarioId", usuarioId)) > 0;
    }

    @Override
    public boolean isAdministradorActivo(final UUID usuarioId) {
        return nativeCount(SQL_ADMINISTRADOR_ACTIVO, "isAdministradorActivo", new Parameter("usuarioId", usuarioId)) > 0;
    }

    @Override
    public Set<InstitutionalRole> findRolesActivosByUsuario(final UUID usuarioId) {
        return JpaQueryExecutor.execute(
                "findRolesActivosByUsuario",
                "No fue posible resolver los roles institucionales activos.",
                () -> {
                    final Set<InstitutionalRole> roles = EnumSet.noneOf(InstitutionalRole.class);
                    for (final Object fila : entityManager.createNativeQuery(SQL_ROLES_ACTIVOS)
                            .setParameter("usuarioId", usuarioId).getResultList()) {
                        roles.add(InstitutionalRole.valueOf(String.valueOf(fila)));
                    }
                    return roles;
                }
        );
    }

    @Override
    public boolean canGestionarCuenta(final UUID actorUsuarioId, final UUID destinoUsuarioId) {
        return nativeCount(SQL_GESTIONA_CUENTA, "canGestionarCuenta",
                new Parameter("actorId", actorUsuarioId), new Parameter("destinoId", destinoUsuarioId)) > 0;
    }

    @Override
    public boolean canCoordinadorAccessGrupo(final UUID usuarioId, final UUID grupoId) {
        return nativeCount(SQL_COORDINADOR_ACCEDE_GRUPO, "canCoordinadorAccessGrupo",
                new Parameter("grupoId", grupoId), new Parameter("usuarioId", usuarioId)) > 0;
    }

    @Override
    public boolean canDecanoAccessGrupo(final UUID usuarioId, final UUID grupoId) {
        return nativeCount(SQL_DECANO_ACCEDE_GRUPO, "canDecanoAccessGrupo",
                new Parameter("grupoId", grupoId), new Parameter("usuarioId", usuarioId)) > 0;
    }

    @Override
    public boolean canCoordinadorAccessPrograma(final UUID usuarioId, final UUID programaId) {
        return count(HQL_COORDINADOR_DE_PROGRAMA, "canCoordinadorAccessPrograma",
                new Parameter("usuarioId", usuarioId), new Parameter("programaId", programaId)) > 0;
    }

    @Override
    public boolean canDecanoAccessFacultad(final UUID usuarioId, final UUID facultadId) {
        return count(HQL_DECANO_DE_FACULTAD, "canDecanoAccessFacultad",
                new Parameter("usuarioId", usuarioId), new Parameter("facultadId", facultadId)) > 0;
    }

    @Override
    public boolean canDecanoAccessPrograma(final UUID usuarioId, final UUID programaId) {
        return count(HQL_DECANO_DE_PROGRAMA, "canDecanoAccessPrograma",
                new Parameter("usuarioId", usuarioId), new Parameter("programaId", programaId)) > 0;
    }

    private Optional<UUID> singleUuid(final String hql, final String parameterName, final Object value,
                                      final String operation) {
        return JpaQueryExecutor.execute(
                operation,
                "No fue posible resolver el alcance institucional.",
                () -> {
                    final List<UUID> rows = entityManager.createQuery(hql, UUID.class)
                            .setParameter(parameterName, value)
                            .setMaxResults(1)
                            .getResultList();
                    return rows.isEmpty() ? Optional.<UUID>empty() : Optional.ofNullable(rows.getFirst());
                }
        );
    }

    private Optional<UUID> uniqueUuidByEmail(final String email) {
        return JpaQueryExecutor.execute(
                "findUsuarioIdByEmail",
                "No fue posible resolver la identidad institucional por correo.",
                () -> {
                    final List<UUID> rows = entityManager.createQuery(HQL_USUARIO_POR_EMAIL, UUID.class)
                            .setParameter("email", email)
                            .getResultList();
                    if (rows.size() > 1) {
                        throw new InstitutionalIdentityAmbiguityException();
                    }
                    return rows.isEmpty() ? Optional.<UUID>empty() : Optional.ofNullable(rows.getFirst());
                }
        );
    }

    private int nativeCount(final String sql, final String operation, final Parameter... parameters) {
        return JpaQueryExecutor.execute(
                operation,
                "No fue posible validar el alcance institucional.",
                () -> {
                    var query = entityManager.createNativeQuery(sql);
                    for (Parameter parameter : parameters) {
                        query.setParameter(parameter.name(), parameter.value());
                    }
                    final Object result = query.getSingleResult();
                    return result instanceof Number number ? Math.toIntExact(number.longValue()) : 0;
                }
        );
    }

    private int count(final String hql, final String operation, final Parameter... parameters) {
        return JpaQueryExecutor.execute(
                operation,
                "No fue posible validar el alcance institucional.",
                () -> {
                    var query = entityManager.createQuery(hql, Long.class);
                    for (Parameter parameter : parameters) {
                        query.setParameter(parameter.name(), parameter.value());
                    }
                    final Long count = query.getSingleResult();
                    return count == null ? 0 : Math.toIntExact(count);
                }
        );
    }

    private record Parameter(String name, Object value) {
    }
}

