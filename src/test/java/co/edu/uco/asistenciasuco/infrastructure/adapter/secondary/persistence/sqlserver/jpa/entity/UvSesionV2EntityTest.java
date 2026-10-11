package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UvSesionV2EntityTest {

    @Test
    void expone_todas_las_columnas_de_la_vista_sin_transformar_datetime2() throws Exception {
        final UvSesionV2Entity entity = new UvSesionV2Entity();
        final UUID sesion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.parse("2042-07-15T14:00:00.1234567");
        final LocalDateTime fin = LocalDateTime.parse("2042-07-15T15:30:00.7654321");
        set(entity, "id", sesion);
        set(entity, "idGrupo", grupo);
        set(entity, "nombre", "Sesion");
        set(entity, "numero", 1);
        set(entity, "codigo", "S-1");
        set(entity, "numeroSemana", 2);
        set(entity, "codigoGrupo", 7);
        set(entity, "nombreGrupo", "Grupo");
        set(entity, "fechaHoraInicio", inicio);
        set(entity, "fechaHoraFin", fin);
        set(entity, "procedenciaTemporal", "UTC_V2");

        assertEquals(sesion, entity.id());
        assertEquals(grupo, entity.idGrupo());
        assertEquals("Sesion", entity.nombre());
        assertEquals(1, entity.numero());
        assertEquals("S-1", entity.codigo());
        assertEquals(2, entity.numeroSemana());
        assertEquals(7, entity.codigoGrupo());
        assertEquals("Grupo", entity.nombreGrupo());
        assertEquals(inicio, entity.fechaHoraInicio());
        assertEquals(fin, entity.fechaHoraFin());
        assertEquals("UTC_V2", entity.procedenciaTemporal());
    }

    private static void set(final UvSesionV2Entity target, final String name, final Object value) throws Exception {
        final var field = UvSesionV2Entity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
