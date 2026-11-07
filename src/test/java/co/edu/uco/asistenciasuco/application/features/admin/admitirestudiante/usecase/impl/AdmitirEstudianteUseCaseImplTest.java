package co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain.AdmisionEstudianteResultadoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain.AdmitirEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.admin.exception.AdmisionEstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.AsegurarCuentaIdentidadService;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.EstadoCuentaAprovisionada;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.ResultadoCuentaIdentidad;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.SolicitudCuentaIdentidad;
import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdmisionEstudianteCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AdmisionEstudianteCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AdmisionEstudianteResultadoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** CULM-001 D.4 (CONTRACT §5.3, HU156): todas las ramas de la admision de estudiantes. */
class AdmitirEstudianteUseCaseImplTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID TIPO = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final String CORREO = "ana.perez@uco.edu.co";
    private static final String CLAVE = "Clave123!";
    private static final String HASH = "{hash}Clave123!";

    private final AdmisionEstudianteCommandPort comando = mock(AdmisionEstudianteCommandPort.class);
    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final PasswordEncoderPort codificador = mock(PasswordEncoderPort.class);
    private final ParameterCatalogPort parametros = mock(ParameterCatalogPort.class);
    private final MessageCatalogPort mensajes = mock(MessageCatalogPort.class);
    private final AsegurarCuentaIdentidadService cuentas = mock(AsegurarCuentaIdentidadService.class);
    private final InstitutionalScopePort ambito = mock(InstitutionalScopePort.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);
    private final AdmitirEstudianteUseCaseImpl useCase;

    AdmitirEstudianteUseCaseImplTest() {
        when(parametros.getRequiredParameter("REGISTRO", "DOMINIO_CORREO_ESTUDIANTE")).thenReturn("@uco.edu.co");
        when(mensajes.getUserMessage(any(), any())).thenReturn("El correo debe ser institucional (@uco.edu.co).");
        when(codificador.encode(CLAVE)).thenReturn(HASH);
        when(usuarios.consultarUsuarioPorCorreo(any())).thenReturn(Optional.empty());
        when(usuarios.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.empty());
        useCase = new AdmitirEstudianteUseCaseImpl(comando, usuarios, codificador,
                new CorreoInstitucionalPolicy(parametros, mensajes), cuentas, ambito, notificaciones);
    }

    private static AdmitirEstudianteDomain domain(final String correo, final String password) {
        return new AdmitirEstudianteDomain(TIPO, 1017123456, "Ana", null, "Perez", null, correo, password, PROGRAMA, ADMIN);
    }

    private static UsuarioIdentidadRepositoryProjection canonico() {
        return new UsuarioIdentidadRepositoryProjection(USUARIO, TIPO, 1017123456, "Ana", "Perez", CORREO);
    }

    private void comandoExitoso(final boolean usuarioNuevo, final boolean estudianteNuevo) {
        when(comando.admitir(any())).thenReturn(new AdmisionEstudianteResultadoProjection(USUARIO, ESTUDIANTE, usuarioNuevo, estudianteNuevo));
        when(usuarios.consultarUsuarioPorId(USUARIO)).thenReturn(Optional.of(canonico()));
    }

    @Test
    void alta_nueva_codifica_la_clave_admite_aprovisiona_con_verificacion_y_avisa() {
        comandoExitoso(true, true);
        when(cuentas.asegurar(any())).thenReturn(new ResultadoCuentaIdentidad(EstadoCuentaAprovisionada.PENDIENTE_VERIFICACION, false));

        final AdmisionEstudianteResultadoDomain resultado = useCase.execute(domain(CORREO, CLAVE));

        assertEquals(ESTUDIANTE, resultado.idEstudiante());
        assertEquals(USUARIO, resultado.idUsuario());
        assertTrue(resultado.esUsuarioNuevo());
        assertEquals(EstadoCuentaAprovisionada.PENDIENTE_VERIFICACION, resultado.estadoCuenta());
        assertFalse(resultado.cuentaReparada());

        final ArgumentCaptor<AdmisionEstudianteCommand> orden = ArgumentCaptor.forClass(AdmisionEstudianteCommand.class);
        verify(comando).admitir(orden.capture());
        assertEquals(HASH, orden.getValue().passwordHash());
        assertEquals(PROGRAMA, orden.getValue().idPrograma());
        assertEquals(ADMIN, orden.getValue().usuarioEjecutor());
        assertEquals(CORREO, orden.getValue().correo());

        final ArgumentCaptor<SolicitudCuentaIdentidad> solicitud = ArgumentCaptor.forClass(SolicitudCuentaIdentidad.class);
        verify(cuentas).asegurar(solicitud.capture());
        assertTrue(solicitud.getValue().requiereVerificacionCorreo());
        assertEquals(CLAVE, solicitud.getValue().passwordInicial());
        assertEquals(InstitutionalRole.ESTUDIANTE, solicitud.getValue().rol());
        assertEquals(USUARIO, solicitud.getValue().idUsuario());
        assertEquals("1017123456", solicitud.getValue().username());

        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        assertEquals(Notificacion.TIPO_VERIFICACION_CORREO, aviso.getValue().tipo());
    }

    @Test
    void un_fallo_del_aviso_no_revierte_la_admision() {
        comandoExitoso(true, true);
        when(cuentas.asegurar(any())).thenReturn(new ResultadoCuentaIdentidad(EstadoCuentaAprovisionada.ACTIVA, false));
        doThrow(new IllegalStateException("cola caida")).when(notificaciones).notificar(any());

        assertEquals(EstadoCuentaAprovisionada.ACTIVA, useCase.execute(domain(CORREO, CLAVE)).estadoCuenta());
    }

    @Test
    void alta_nueva_sin_clave_es_validacion_y_no_toca_la_base() {
        final ValidationException error = assertThrows(ValidationException.class, () -> useCase.execute(domain(CORREO, null)));

        assertEquals(UsuarioErrorCode.ERR_PASSWORD_REQUERIDO.code(), error.getCode());
        verifyNoInteractions(comando, cuentas);
    }

    @Test
    void alta_nueva_con_clave_que_no_cumple_la_politica_es_validacion() {
        assertThrows(ValidationException.class, () -> useCase.execute(domain(CORREO, "corta")));
        verifyNoInteractions(comando);
    }

    @Test
    void alta_nueva_con_correo_no_institucional_es_validacion_VCE_001_y_no_toca_la_base() {
        assertThrows(ValidationException.class, () -> useCase.execute(domain("ana@gmail.com", CLAVE)));
        verifyNoInteractions(comando, cuentas);
    }

    @Test
    void persona_existente_no_revalida_correo_ni_exige_clave_y_no_pide_verificacion() {
        when(usuarios.consultarUsuarioPorCorreo(any())).thenReturn(Optional.of(canonico()));
        when(usuarios.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.of(canonico()));
        comandoExitoso(false, true);
        when(cuentas.asegurar(any())).thenReturn(new ResultadoCuentaIdentidad(EstadoCuentaAprovisionada.ACTIVA, false));

        final AdmisionEstudianteResultadoDomain resultado = useCase.execute(domain("ana@externo.com", null));

        assertFalse(resultado.esUsuarioNuevo());
        final ArgumentCaptor<AdmisionEstudianteCommand> orden = ArgumentCaptor.forClass(AdmisionEstudianteCommand.class);
        verify(comando).admitir(orden.capture());
        assertEquals(null, orden.getValue().passwordHash());
        verify(codificador, never()).encode(any());
        final ArgumentCaptor<SolicitudCuentaIdentidad> solicitud = ArgumentCaptor.forClass(SolicitudCuentaIdentidad.class);
        verify(cuentas).asegurar(solicitud.capture());
        assertFalse(solicitud.getValue().requiereVerificacionCorreo());
        verify(notificaciones, never()).notificar(any());
    }

    @Test
    void una_sola_clave_existente_deja_la_decision_al_sp_ADS_004() {
        when(usuarios.consultarUsuarioPorCorreo(any())).thenReturn(Optional.of(canonico()));
        when(comando.admitir(any())).thenThrow(new ConflictException(AdmisionEstudianteErrorCode.ADS_004));

        final ConflictException error = assertThrows(ConflictException.class, () -> useCase.execute(domain(CORREO, null)));

        assertEquals(AdmisionEstudianteErrorCode.ADS_004.code(), error.getCode());
        verifyNoInteractions(cuentas);
    }

    @Test
    void ya_admitido_ADS_002_repara_la_cuenta_faltante_y_responde_con_la_persona_existente() {
        when(usuarios.consultarUsuarioPorCorreo(any())).thenReturn(Optional.of(canonico()));
        when(usuarios.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.of(canonico()));
        when(comando.admitir(any())).thenThrow(new ConflictException(AdmisionEstudianteErrorCode.ADS_002));
        when(ambito.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));
        when(cuentas.repararOFallar(any())).thenReturn(new ResultadoCuentaIdentidad(EstadoCuentaAprovisionada.ACTIVA, true));

        final AdmisionEstudianteResultadoDomain resultado = useCase.execute(domain(CORREO, null));

        assertEquals(ESTUDIANTE, resultado.idEstudiante());
        assertEquals(USUARIO, resultado.idUsuario());
        assertFalse(resultado.esUsuarioNuevo());
        assertTrue(resultado.cuentaReparada());
        assertEquals(EstadoCuentaAprovisionada.ACTIVA, resultado.estadoCuenta());
        verify(cuentas, never()).asegurar(any());
        verify(notificaciones, never()).notificar(any());
    }

    @Test
    void ya_admitido_con_la_cuenta_existente_propaga_el_conflicto_del_servicio() {
        when(usuarios.consultarUsuarioPorCorreo(any())).thenReturn(Optional.of(canonico()));
        when(usuarios.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.of(canonico()));
        when(comando.admitir(any())).thenThrow(new ConflictException(AdmisionEstudianteErrorCode.ADS_002));
        when(ambito.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));
        final ConflictException conflicto = new ConflictException(AdmisionEstudianteErrorCode.ADS_002);
        when(cuentas.repararOFallar(any())).thenThrow(conflicto);

        assertSame(conflicto, assertThrows(ConflictException.class, () -> useCase.execute(domain(CORREO, null))));
    }

    @Test
    void ya_admitido_sin_poder_resolver_la_persona_es_un_error_interno() {
        when(usuarios.consultarUsuarioPorCorreo(any())).thenReturn(Optional.of(canonico()));
        when(usuarios.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.of(canonico()), Optional.empty());
        when(comando.admitir(any())).thenThrow(new ConflictException(AdmisionEstudianteErrorCode.ADS_002));

        assertThrows(InternalApplicationException.class, () -> useCase.execute(domain(CORREO, null)));

        when(usuarios.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.of(canonico()));
        when(ambito.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.empty());
        assertThrows(InternalApplicationException.class, () -> useCase.execute(domain(CORREO, null)));
    }

    @Test
    void un_conflicto_distinto_de_ADS_002_se_propaga_sin_reparar() {
        when(comando.admitir(any())).thenThrow(new ConflictException(UsuarioErrorCode.ERR_UNICIDAD_DOCUMENTO));

        assertThrows(ConflictException.class, () -> useCase.execute(domain(CORREO, CLAVE)));
        verifyNoInteractions(cuentas);
    }

    @Test
    void sin_la_identidad_canonica_despues_del_comando_es_un_error_interno() {
        when(comando.admitir(any())).thenReturn(new AdmisionEstudianteResultadoProjection(USUARIO, ESTUDIANTE, true, true));
        when(usuarios.consultarUsuarioPorId(USUARIO)).thenReturn(Optional.empty());

        assertThrows(InternalApplicationException.class, () -> useCase.execute(domain(CORREO, CLAVE)));
        verifyNoInteractions(cuentas);
    }

    @Test
    void el_dominio_nulo_y_las_dependencias_nulas_se_rechazan() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
        final CorreoInstitucionalPolicy politica = new CorreoInstitucionalPolicy(parametros, mensajes);
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteUseCaseImpl(null, usuarios, codificador, politica, cuentas, ambito, notificaciones));
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteUseCaseImpl(comando, null, codificador, politica, cuentas, ambito, notificaciones));
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteUseCaseImpl(comando, usuarios, null, politica, cuentas, ambito, notificaciones));
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteUseCaseImpl(comando, usuarios, codificador, null, cuentas, ambito, notificaciones));
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteUseCaseImpl(comando, usuarios, codificador, politica, null, ambito, notificaciones));
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteUseCaseImpl(comando, usuarios, codificador, politica, cuentas, null, notificaciones));
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteUseCaseImpl(comando, usuarios, codificador, politica, cuentas, ambito, null));
    }

    /** OBS-12: la clave nunca aparece en la representacion de texto del dominio ni de la orden (logs, metadata). */
    @Test
    void la_clave_no_aparece_en_to_string() {
        assertFalse(domain(CORREO, CLAVE).toString().contains(CLAVE));
        assertFalse(new AdmisionEstudianteCommand(TIPO, 1, "Ana", null, "Perez", null, CORREO, HASH, PROGRAMA, ADMIN).toString().contains(HASH));
        assertNotNull(domain(CORREO, CLAVE).toString());
    }
}
