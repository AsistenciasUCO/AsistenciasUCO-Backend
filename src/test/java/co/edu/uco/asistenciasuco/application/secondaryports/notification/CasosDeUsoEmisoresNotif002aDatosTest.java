package co.edu.uco.asistenciasuco.application.secondaryports.notification;

import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.domain.RetirarEstudianteGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.impl.RetirarEstudianteGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.RetiroEstudianteGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioRetiroProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TEST_PLAN §2.6 (BE-U01 y BE-U02): verifica que el caso de uso emisor incluya las claves nuevas del CONTRACT §4.4
 * manteniendo intactos tipo, asunto, mensaje y las claves previas de datos. CULM-001 A.1: los 3 emisores de
 * solicitudes de ingreso (HU053-HU055) se retiran con sus casos; queda el retiro del grupo (HU051).
 */
class CasosDeUsoEmisoresNotif002aDatosTest {

    @Test
    @DisplayName("BE-U01 y BE-U02 (1/4): RetirarEstudianteGrupoUseCaseImpl agrega grupo y estudiante preservando claves y textos previos")
    void BE_U01_U02_retiro_estudiante_grupo() {
        final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
        final RetiroEstudianteGrupoPort retiroPort = mock(RetiroEstudianteGrupoPort.class);
        final NotificacionPort notificacionPort = mock(NotificacionPort.class);

        final UUID actor = UUID.randomUUID();
        final UUID grupoId = UUID.randomUUID();
        final UUID estudianteId = UUID.randomUUID();
        when(scopePort.findDocenteIdByUsuario(actor)).thenReturn(Optional.of(UUID.randomUUID()));
        when(retiroPort.consultarDestinatarios(grupoId, estudianteId, actor)).thenReturn(List.of(
                new DestinatarioRetiroProjection("ESTUDIANTE", UUID.randomUUID(), "Ana María Gómez", "ana@uco.edu.co",
                        grupoId, 1, "G01", "Ingeniería de Software"),
                new DestinatarioRetiroProjection("DOCENTE", actor, "Docente Titular", "doc@uco.edu.co",
                        grupoId, 1, "G01", "Ingeniería de Software")
        ));

        final RetirarEstudianteGrupoUseCaseImpl useCase = new RetirarEstudianteGrupoUseCaseImpl(scopePort, retiroPort, notificacionPort);
        useCase.execute(new RetirarEstudianteGrupoDomain(actor, grupoId, estudianteId, "Inasistencia del 20%"));

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        final Notificacion n = captor.getValue();

        // BE-U02: tipo, asunto, mensaje y claves previas intactas
        assertEquals(Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO, n.tipo());
        assertEquals("Retiro del grupo G01 de Ingeniería de Software", n.asunto());
        assertEquals("Se retiro a Ana María Gómez del grupo G01 de Ingeniería de Software. Motivo: Inasistencia del 20%", n.mensaje());
        assertEquals(grupoId.toString(), n.datos().get("idGrupo"));
        assertEquals(estudianteId.toString(), n.datos().get("idEstudiante"));
        assertEquals("Inasistencia del 20%", n.datos().get("motivo"));

        // BE-U01: claves nuevas CONTRACT §4.4
        assertEquals("G01 de Ingeniería de Software", n.datos().get("grupo"));
        assertEquals("Ana María Gómez", n.datos().get("estudiante"));
    }
}
