package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** NOTIF-002B: datos de muestra de vistas previas y correos de prueba. */
class MuestrasNotificacionTest {

    @Test
    void completo_incluye_todos_los_marcadores_conocidos_en_una_copia_nueva() {
        final Map<String, String> uno = MuestrasNotificacion.completo();
        final Map<String, String> dos = MuestrasNotificacion.completo();

        assertNotSame(uno, dos);
        assertTrue(uno.keySet().containsAll(java.util.List.of(
                "grupo", "estudiante", "fecha", "porcentaje", "umbral", "limite", "motivo", "estado", "observacion",
                "periodo", "totalEstudiantes", "totalAprobados", "totalReprobados", "nombre", "enlace", "resultado")));
    }

    @Test
    void valor_para_devuelve_el_valor_conocido_o_el_marcador_entre_corchetes() {
        assertEquals("2026-2", MuestrasNotificacion.valorPara("periodo"));
        assertEquals("[inventado]", MuestrasNotificacion.valorPara("inventado"));
    }

    @Test
    void basico_de_vista_previa_tiene_solo_los_marcadores_comunes() {
        final Map<String, String> basico = MuestrasNotificacion.basicoVistaPrevia();

        assertEquals(8, basico.size());
        assertTrue(basico.containsKey("estudiante"));
    }
}
