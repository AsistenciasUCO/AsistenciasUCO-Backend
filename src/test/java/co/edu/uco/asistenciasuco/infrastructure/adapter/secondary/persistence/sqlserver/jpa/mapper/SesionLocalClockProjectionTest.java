package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvSesionEntity;
import org.junit.jupiter.api.Test;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Regression for MAINT-003I. SQL Server DATETIME2(7) must keep its date and clock
 * without interpreting them in the process timezone. This is an invariant of
 * the JPA projection, not a claim that legacy v1 timestamps are proven UTC.
 */
class SesionLocalClockProjectionTest {

    @Test
    void sessionViewTimesAreMappedAsTimezoneFreeDatetime2Values() throws NoSuchFieldException {
        for (String column : new String[] { "fechaHoraInicio", "fechaHoraFin" }) {
            final Field field = UvSesionEntity.class.getDeclaredField(column);
            assertEquals(LocalDateTime.class, field.getType(), column + " must not use java.util.Date");
            final JdbcType mapping = field.getAnnotation(JdbcType.class);
            assertNotNull(mapping, column + " must declare a timezone-free Hibernate JDBC type");
            assertEquals(LocalDateTimeJdbcType.class, mapping.value(), column);
        }
    }

    @Test
    void sessionProjectionPreservesExactSqlClockAndSevenFractionalDigits() {
        UvSesionEntity row = mock(UvSesionEntity.class);
        UUID id = UUID.randomUUID();
        LocalDateTime inicio = LocalDateTime.of(2042, 7, 16, 14, 0, 0, 123456700);
        LocalDateTime fin = LocalDateTime.of(2042, 7, 16, 16, 30, 0, 765432100);
        when(row.id()).thenReturn(id);
        when(row.fechaHoraInicio()).thenReturn(inicio);
        when(row.fechaHoraFin()).thenReturn(fin);
        var projected = CoreViewJpaProjectionMapper.toSesion(row);
        assertEquals(id, projected.getSesion());
        assertEquals(inicio, projected.getFechaHoraInicio());
        assertEquals(fin, projected.getFechaHoraFin());
    }

    @Test
    void sessionProjectionPreservesUnknownClockValuesAsNull() {
        UvSesionEntity row = mock(UvSesionEntity.class);
        var projected = CoreViewJpaProjectionMapper.toSesion(row);
        assertNull(projected.getFechaHoraInicio());
        assertNull(projected.getFechaHoraFin());
    }
}
