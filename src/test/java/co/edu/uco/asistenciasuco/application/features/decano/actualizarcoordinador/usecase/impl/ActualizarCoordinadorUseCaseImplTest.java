package co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase.domain.ActualizarCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ResultadoGestionCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.ResultadoSincronizacion;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.ActualizarCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU121 (CONTRACT HU121-HU122 §3.2): editar datos del coordinador. DB primero, identidad despues del commit.
 * Depende de IAM-001 ({@link SincronizarCuentaInstitucionalService}): sin ese servicio estas pruebas no compilan (RED).
 */
class ActualizarCoordinadorUseCaseImplTest {

    private static final UUID DECANO = UUID.randomUUID();
    private static final UUID FACULTAD = UUID.randomUUID();
    private static final UUID COORDINADOR = UUID.randomUUID();
    private static final UUID USUARIO_COORDINADOR = UUID.randomUUID();
    private static final String MENSAJE_IAM_002 = "El cambio se guardo. La cuenta de acceso se sincronizara pronto.";

    private final CoordinadorCommandPort commandPort = mock(CoordinadorCommandPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SincronizarCuentaInstitucionalService sincronizacion = mock(SincronizarCuentaInstitucionalService.class);
    private final MessageCatalogPort messageCatalog = mock(MessageCatalogPort.class);
    private final ActualizarCoordinadorUseCaseImpl useCase =
            new ActualizarCoordinadorUseCaseImpl(commandPort, scopePort, sincronizacion, messageCatalog);

    private ActualizarCoordinadorDomain dominio() {
        return new ActualizarCoordinadorDomain(COORDINADOR, "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", DECANO);
    }

    private void dbActualiza() {
        when(scopePort.findFacultadIdByDecanoUsuario(DECANO)).thenReturn(Optional.of(FACULTAD));
        when(commandPort.actualizarCoordinador(COORDINADOR, "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", FACULTAD, DECANO,
                co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO.ninguna()))
                .thenReturn(new ResultadoGestionCoordinadorProjection(USUARIO_COORDINADOR, false));
    }

    @Test
    void execute_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_lanza_forbidden_sin_tocar_db_ni_identidad_cuando_no_hay_facultad_de_decano() {
        when(scopePort.findFacultadIdByDecanoUsuario(DECANO)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(dominio()));

        verifyNoInteractions(commandPort, sincronizacion);
    }

    @Test
    void execute_actualiza_en_db_con_la_facultad_del_decano_y_luego_sincroniza_la_cuenta() {
        dbActualiza();
        when(sincronizacion.actualizarDatos(eq(USUARIO_COORDINADOR), any(ActualizarCuentaIdentidadDTO.class)))
                .thenReturn(ResultadoSincronizacion.OK);

        final ResultadoGestionCoordinadorDomain resultado = useCase.execute(dominio());

        final ArgumentCaptor<ActualizarCuentaIdentidadDTO> captor = ArgumentCaptor.forClass(ActualizarCuentaIdentidadDTO.class);
        verify(sincronizacion).actualizarDatos(eq(USUARIO_COORDINADOR), captor.capture());
        assertEquals("ana@uco.edu.co", captor.getValue().correo());
        assertEquals("ANA", captor.getValue().primerNombre());
        assertEquals("PEREZ", captor.getValue().primerApellido());
        assertEquals(COORDINADOR, resultado.idCoordinador());
        assertFalse(resultado.sincronizacionPendiente());
        assertNull(resultado.mensajeUsuario());
    }

    @Test
    void execute_con_identidad_pendiente_responde_exito_con_advertencia_iam_002() {
        dbActualiza();
        when(sincronizacion.actualizarDatos(eq(USUARIO_COORDINADOR), any(ActualizarCuentaIdentidadDTO.class)))
                .thenReturn(ResultadoSincronizacion.PENDIENTE);
        when(messageCatalog.getUserMessage("IAM_002")).thenReturn(MENSAJE_IAM_002);

        final ResultadoGestionCoordinadorDomain resultado = useCase.execute(dominio());

        assertTrue(resultado.sincronizacionPendiente());
        assertEquals(MENSAJE_IAM_002, resultado.mensajeUsuario());
        assertEquals(COORDINADOR, resultado.idCoordinador());
    }

    @Test
    void execute_no_invoca_la_identidad_si_la_db_rechaza_la_operacion() {
        when(scopePort.findFacultadIdByDecanoUsuario(DECANO)).thenReturn(Optional.of(FACULTAD));
        when(commandPort.actualizarCoordinador(COORDINADOR, "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", FACULTAD, DECANO,
                co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO.ninguna()))
                .thenThrow(new ConflictException("Correo ya registrado."));

        assertThrows(ConflictException.class, () -> useCase.execute(dominio()));

        verifyNoInteractions(sincronizacion);
    }

    @Test
    void HU159_correccion_del_documento_envia_el_documento_y_sincroniza_el_username() {
        final UUID tipo = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(DECANO)).thenReturn(Optional.of(FACULTAD));
        final co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO documento =
                new co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO(tipo, 915900021, "Digitacion");
        when(commandPort.actualizarCoordinador(COORDINADOR, "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", FACULTAD, DECANO, documento))
                .thenReturn(new ResultadoGestionCoordinadorProjection(USUARIO_COORDINADOR, false));
        when(sincronizacion.actualizarDatos(eq(USUARIO_COORDINADOR), any())).thenReturn(ResultadoSincronizacion.OK);

        useCase.execute(new ActualizarCoordinadorDomain(COORDINADOR, "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", DECANO,
                tipo, 915900021, "Digitacion"));

        verify(sincronizacion).actualizarDatos(USUARIO_COORDINADOR,
                new ActualizarCuentaIdentidadDTO("ana@uco.edu.co", "ANA", "PEREZ", "915900021"));
    }
}
