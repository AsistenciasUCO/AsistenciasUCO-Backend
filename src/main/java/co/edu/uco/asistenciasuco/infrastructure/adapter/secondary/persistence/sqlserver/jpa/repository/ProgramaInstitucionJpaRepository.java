package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaInstitucionQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaInstitucionProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Programas activos de la institucion del administrador (CULM-001 D.4, HU156) sobre
 * {@code usp_consultar_programas_institucion_paginado}: RBAC ADMINISTRADOR, ambito (institucion del administrador), filtros y
 * paginacion con {@code COUNT(1) OVER()} dentro del SP. Los rechazos (SEC_001, PAG_001) se traducen con {@link DbCodeTranslator}.
 */
@Repository
public class ProgramaInstitucionJpaRepository implements ProgramaInstitucionQueryPort {

    static final String SQL_PROGRAMAS = """
            EXEC dbo.usp_consultar_programas_institucion_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @texto = :texto,
                 @idFacultad = :idFacultad
            """;
    static final int COLUMNAS_PROGRAMAS = 5;
    private static final int COLUMNA_TOTAL = 4;

    private final EntityManager entityManager;

    public ProgramaInstitucionJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de programas de la institucion es obligatorio.");
    }

    @Override
    public PaginaResultado<ProgramaInstitucionProjection> consultarProgramas(
            final UUID usuarioEjecutor,
            final String texto,
            final UUID idFacultad,
            final int numeroPagina,
            final int tamanoPagina
    ) {
        if (usuarioEjecutor == null) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta de programas es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarProgramasInstitucion",
                "No fue posible consultar los programas de la institucion desde base de datos.",
                () -> {
                    final List<?> filas = entityManager.createNativeQuery(SQL_PROGRAMAS)
                            .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                            .setParameter("idCorrelacion", correlationId)
                            .setParameter("numeroPagina", numeroPagina)
                            .setParameter("tamanoPagina", tamanoPagina)
                            .setParameter("texto", texto)
                            .setParameter("idFacultad", idFacultad)
                            .getResultList();
                    final List<ProgramaInstitucionProjection> programas = filas.stream()
                            .map(Object[].class::cast)
                            .map(ProgramaInstitucionJpaRepository::toProjection)
                            .toList();
                    final long total = filas.isEmpty() ? 0 : NativeRowValues.entero(((Object[]) filas.getFirst())[COLUMNA_TOTAL]);
                    return new PaginaResultado<>(programas, numeroPagina, tamanoPagina, total);
                },
                DbCodeTranslator::traducir
        );
    }

    private static ProgramaInstitucionProjection toProjection(final Object[] f) {
        if (f.length != COLUMNAS_PROGRAMAS) {
            throw new IllegalStateException("Los programas de la institucion devolvieron " + f.length + " columnas; se esperaban "
                    + COLUMNAS_PROGRAMAS + ".");
        }
        return new ProgramaInstitucionProjection(NativeRowValues.uuid(f[0]), f[1] == null ? null : f[1].toString().strip(),
                NativeRowValues.uuid(f[2]), f[3] == null ? null : f[3].toString().strip());
    }
}
