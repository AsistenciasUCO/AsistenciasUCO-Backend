package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.OfertaCreacionGruposDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaOfertaDocenteProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Adaptador JPA de la oferta de asignaturas con las que el docente autenticado puede abrir un grupo (F3). */
@Repository
public class OfertaCreacionGruposDocenteJpaRepository implements OfertaCreacionGruposDocenteQueryPort {

    static final String SQL_OFERTA = """
            EXEC dbo.usp_consultar_oferta_creacion_grupo_docente
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion
            """;

    static final String OP_OFERTA = "consultarOfertaCreacionGrupoDocente";

    private final EntityManager entityManager;

    public OfertaCreacionGruposDocenteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public List<AsignaturaOfertaDocenteProjection> consultarOferta(final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(usuarioEjecutor)) {
            throw new CrosscuttingException("El usuario ejecutor de la oferta de creacion de grupos es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_OFERTA,
                "No fue posible consultar la oferta de creacion de grupos del docente desde base de datos.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_OFERTA)
                        .setParameter("idCorrelacion", correlationId)
                        .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                        .getResultList()).stream()
                        .map(Object[].class::cast)
                        .map(OfertaCreacionGruposDocenteJpaRepository::toProjection)
                        .toList()
        );
    }

    /** Columnas del SP: idDocente, idAsignatura, codigoAsignatura, nombreAsignatura, idPrograma, nombrePrograma. */
    private static AsignaturaOfertaDocenteProjection toProjection(final Object[] f) {
        return new AsignaturaOfertaDocenteProjection(
                NativeRowValues.uuid(f[1]), texto(f[2]), texto(f[3]), NativeRowValues.uuid(f[4]), texto(f[5]));
    }

    private static String texto(final Object valor) {
        return valor == null ? null : valor.toString().strip();
    }
}
