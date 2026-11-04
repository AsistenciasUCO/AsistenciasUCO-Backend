package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistrarAsistenciasSesionDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistroAsistenciaSesionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegistrarAsistenciasSesionUseCaseImplTest {

    private static final UUID SESION = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID USUARIO_EJECUTOR = UUID.randomUUID();

    /** Sesion institucional 08:00-10:00 America/Bogota (UTC-5) = 13:00-15:00 UTC, instantes persistidos en UTC. */
    private static final LocalDateTime INICIO_UTC = LocalDateTime.of(2026, 11, 3, 13, 0);
    private static final LocalDateTime FIN_UTC = LocalDateTime.of(2026, 11, 3, 15, 0);
    private static final Instant DURANTE = Instant.parse("2026-11-03T14:00:00Z");

    private final AsistenciaRepositoryPort asistenciaRepositoryPort = mock(AsistenciaRepositoryPort.class);
    private final SesionProcedenciaQueryPort sesionProcedenciaQueryPort = mock(SesionProcedenciaQueryPort.class);
    private final InstitutionalScopePort institutionalScopePort = mock(InstitutionalScopePort.class);
    private final RealtimePublisherPort realtimePublisherPort = mock(RealtimePublisherPort.class);

    @Test
    void ejecucion_exitosa_persiste_y_luego_publica_en_ese_orden_exacto() {
        darTitularidadAlDocente();

        useCaseEn(DURANTE).execute(dominioValido());

        final InOrder orden = inOrder(asistenciaRepositoryPort, realtimePublisherPort);
        orden.verify(asistenciaRepositoryPort).registrarAsistenciasSesion(any(RegistrarAsistenciasSesionRepositoryDTO.class));
        orden.verify(realtimePublisherPort).publish(any(RealtimeEvent.class));
    }

    @Test
    void evento_publicado_tiene_el_type_y_payload_canonicos() {
        darTitularidadAlDocente();

        useCaseEn(DURANTE).execute(dominioValido());

        final ArgumentCaptor<RealtimeEvent> captor = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtimePublisherPort).publish(captor.capture());
        final RealtimeEvent event = captor.getValue();
        assertEquals("ASISTENCIAS_SESION_ACTUALIZADAS", event.type());
        assertEquals(GRUPO.toString(), event.payload().get("grupo"));
        assertEquals(SESION.toString(), event.payload().get("sesion"));
        assertEquals(1, event.payload().get("totalRegistros"));
    }

    @Test
    void si_la_persistencia_falla_no_se_publica_evento_alguno() {
        darTitularidadAlDocente();
        doThrow(new CrosscuttingException("fallo de persistencia"))
                .when(asistenciaRepositoryPort).registrarAsistenciasSesion(any());

        assertThrows(CrosscuttingException.class, () -> useCaseEn(DURANTE).execute(dominioValido()));

        verify(realtimePublisherPort, never()).publish(any());
    }

    @Test
    void docente_ajeno_al_grupo_de_la_sesion_es_rechazado_sin_persistir_ni_publicar() {
        when(sesionProcedenciaQueryPort.consultarSesion(SESION)).thenReturn(Optional.of(sesionConfirmada()));
        when(institutionalScopePort.canDocenteAccessGrupo(USUARIO_EJECUTOR, GRUPO)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCaseEn(DURANTE).execute(dominioValido()));

        verifyNoInteractions(asistenciaRepositoryPort);
        verify(realtimePublisherPort, never()).publish(any());
    }

    @Test
    void docente_ajeno_recibe_forbidden_y_no_conoce_el_estado_temporal_de_una_sesion_cerrada() {
        when(sesionProcedenciaQueryPort.consultarSesion(SESION)).thenReturn(Optional.of(sesionConfirmada()));
        when(institutionalScopePort.canDocenteAccessGrupo(USUARIO_EJECUTOR, GRUPO)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> useCaseEn(Instant.parse("2026-11-03T17:00:00Z")).execute(dominioValido()));

        verifyNoInteractions(asistenciaRepositoryPort);
    }

    @Test
    void sesion_inexistente_es_rechazada_con_notFound_sin_persistir_ni_publicar() {
        when(sesionProcedenciaQueryPort.consultarSesion(SESION)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCaseEn(DURANTE).execute(dominioValido()));

        verifyNoInteractions(asistenciaRepositoryPort);
        verify(realtimePublisherPort, never()).publish(any());
    }

    @Test
    void propaga_exactamente_el_usuarioEjecutor_del_dominio_al_repositoryDTO() {
        darTitularidadAlDocente();

        useCaseEn(DURANTE).execute(dominioValido());

        final ArgumentCaptor<RegistrarAsistenciasSesionRepositoryDTO> captor =
                ArgumentCaptor.forClass(RegistrarAsistenciasSesionRepositoryDTO.class);
        verify(asistenciaRepositoryPort).registrarAsistenciasSesion(captor.capture());
        assertEquals(USUARIO_EJECUTOR, captor.getValue().usuarioEjecutor());
    }

    // SES-01: ventana [inicio, fin) contra el reloj del servidor en UTC, sin tolerancia posterior al cierre.

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-03T13:00:00Z", "2026-11-03T14:00:00Z", "2026-11-03T14:59:59.999999999Z"})
    void dentro_de_la_ventana_inicio_inclusivo_registra(final String instante) {
        darTitularidadAlDocente();

        useCaseEn(Instant.parse(instante)).execute(dominioValido());

        verify(asistenciaRepositoryPort).registrarAsistenciasSesion(any(RegistrarAsistenciasSesionRepositoryDTO.class));
    }

    @Test
    void antes_del_inicio_rechaza_con_conflicto_sesion_no_iniciada_sin_persistir() {
        darTitularidadAlDocente();

        final ConflictException exception = assertThrows(ConflictException.class,
                () -> useCaseEn(Instant.parse("2026-11-03T12:59:59Z")).execute(dominioValido()));

        assertEquals(AsistenciaErrorCode.ERR_SESION_NO_INICIADA.code(), exception.getCode());
        verifyNoInteractions(asistenciaRepositoryPort);
        verify(realtimePublisherPort, never()).publish(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-03T15:00:00Z", "2026-11-03T15:00:00.000000001Z", "2026-11-03T17:00:00Z", "2026-11-04T13:30:00Z"})
    void en_el_fin_exacto_o_despues_rechaza_con_conflicto_sesion_cerrada_sin_persistir(final String instante) {
        darTitularidadAlDocente();

        final ConflictException exception = assertThrows(ConflictException.class,
                () -> useCaseEn(Instant.parse(instante)).execute(dominioValido()));

        assertEquals(AsistenciaErrorCode.ERR_SESION_CERRADA.code(), exception.getCode());
        verifyNoInteractions(asistenciaRepositoryPort);
        verify(realtimePublisherPort, never()).publish(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Europe/London", "Europe/Berlin", "America/Bogota", "Asia/Tokyo"})
    void la_zona_del_reloj_del_servidor_no_altera_la_decision_que_depende_solo_del_instante(final String zona) {
        darTitularidadAlDocente();
        final Clock cerradoEnOtraZona = Clock.fixed(Instant.parse("2026-11-03T15:00:00Z"), ZoneId.of(zona));
        final RegistrarAsistenciasSesionUseCaseImpl useCase = new RegistrarAsistenciasSesionUseCaseImpl(
                asistenciaRepositoryPort, sesionProcedenciaQueryPort, institutionalScopePort, realtimePublisherPort,
                cerradoEnOtraZona);

        final ConflictException exception = assertThrows(ConflictException.class, () -> useCase.execute(dominioValido()));

        assertEquals(AsistenciaErrorCode.ERR_SESION_CERRADA.code(), exception.getCode());
        verifyNoInteractions(asistenciaRepositoryPort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "LOCAL", "utc_v2"})
    void procedencia_indeterminada_o_desconocida_rechaza_sin_suponer_zona(final String procedencia) {
        darTitularidadAlDocente(sesionConProcedencia(procedencia.isEmpty() ? null : procedencia));

        final ConflictException exception = assertThrows(ConflictException.class,
                () -> useCaseEn(DURANTE).execute(dominioValido()));

        assertEquals(AsistenciaErrorCode.ERR_SESION_HORARIO_INDETERMINADO.code(), exception.getCode());
        verifyNoInteractions(asistenciaRepositoryPort);
        verify(realtimePublisherPort, never()).publish(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTC_V2", "UTC_GENERADOR", "UTC_OWNER"})
    void toda_procedencia_confirmada_permite_registrar_dentro_de_la_ventana(final String procedencia) {
        darTitularidadAlDocente(sesionConProcedencia(procedencia));

        useCaseEn(DURANTE).execute(dominioValido());

        verify(asistenciaRepositoryPort).registrarAsistenciasSesion(any(RegistrarAsistenciasSesionRepositoryDTO.class));
    }

    private RegistrarAsistenciasSesionUseCaseImpl useCaseEn(final Instant instante) {
        return new RegistrarAsistenciasSesionUseCaseImpl(
                asistenciaRepositoryPort, sesionProcedenciaQueryPort, institutionalScopePort, realtimePublisherPort,
                Clock.fixed(instante, ZoneOffset.UTC));
    }

    private void darTitularidadAlDocente() {
        darTitularidadAlDocente(sesionConfirmada());
    }

    private void darTitularidadAlDocente(final SesionProcedenciaRepositoryProjection sesion) {
        when(sesionProcedenciaQueryPort.consultarSesion(SESION)).thenReturn(Optional.of(sesion));
        when(institutionalScopePort.canDocenteAccessGrupo(USUARIO_EJECUTOR, GRUPO)).thenReturn(true);
    }

    private SesionProcedenciaRepositoryProjection sesionConfirmada() {
        return sesionConProcedencia("UTC_V2");
    }

    private SesionProcedenciaRepositoryProjection sesionConProcedencia(final String procedencia) {
        return new SesionProcedenciaRepositoryProjection(
                SESION, GRUPO, "Sesion 1", 1, "S01", 1, "G01", "Grupo 1", INICIO_UTC, FIN_UTC, procedencia);
    }

    private RegistrarAsistenciasSesionDomain dominioValido() {
        return new RegistrarAsistenciasSesionDomain(
                SESION,
                List.of(new RegistroAsistenciaSesionDomain(ESTUDIANTE, "AN")),
                USUARIO_EJECUTOR
        );
    }
}
