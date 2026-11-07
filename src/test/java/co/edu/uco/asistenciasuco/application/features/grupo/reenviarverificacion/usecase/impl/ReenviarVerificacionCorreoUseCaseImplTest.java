package co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.VerificacionCorreoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CrearUsuarioRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** IDENT-001 CA-09: el reenvio valida en la DB y solo entonces registra un aviso sin token ni correo. */
class ReenviarVerificacionCorreoUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID USUARIO_ESTUDIANTE = UUID.randomUUID();

    private final List<Notificacion> avisos = new ArrayList<>();

    private VerificacionCorreoRepositoryPort repositorio(final RuntimeException rechazo) {
        return new VerificacionCorreoRepositoryPort() {
            @Override
            public Optional<UUID> consultarUsuarioPorToken(final String token) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void confirmar(final String token) {
                throw new UnsupportedOperationException();
            }

            @Override
            public UUID consultarReenvio(final UUID grupoId, final UUID estudianteId, final UUID actorUsuarioId) {
                assertEquals(GRUPO, grupoId);
                assertEquals(ESTUDIANTE, estudianteId);
                assertEquals(ACTOR, actorUsuarioId);
                if (rechazo != null) {
                    throw rechazo;
                }
                return USUARIO_ESTUDIANTE;
            }
        };
    }

    private static UsuarioRepositoryPort usuarios(final Optional<UsuarioIdentidadRepositoryProjection> usuario) {
        return new UsuarioRepositoryPort() {
            @Override
            public Optional<String> consultarCorreoPorEstudiante(final UUID idEstudiante) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CrearUsuarioRepositoryProjection crearUsuario(final CrearUsuarioRepositoryDTO dto) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorCorreo(final String correo) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorId(final UUID idUsuario) {
                return usuario;
            }

            @Override
            public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorIdentificacion(
                    final UUID tipoIdentificacionId, final Integer numeroIdentificacion) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static Optional<UsuarioIdentidadRepositoryProjection> ana() {
        return Optional.of(new UsuarioIdentidadRepositoryProjection(
                USUARIO_ESTUDIANTE, UUID.randomUUID(), 123456789, "ANA MARIA", "PEREZ", "ana.perez@uco.edu.co"));
    }

    private ReenviarVerificacionCorreoUseCaseImpl useCase(
            final RuntimeException rechazo,
            final Optional<UsuarioIdentidadRepositoryProjection> usuario,
            final NotificacionPort notificacionPort
    ) {
        return new ReenviarVerificacionCorreoUseCaseImpl(repositorio(rechazo), usuarios(usuario), notificacionPort);
    }

    @Test
    void reenvio_valido_registra_un_aviso_de_verificacion_sin_token_ni_correo() {
        useCase(null, ana(), avisos::add).execute(ACTOR, GRUPO, ESTUDIANTE);

        assertEquals(1, avisos.size());
        final Notificacion aviso = avisos.getFirst();
        assertEquals(Notificacion.TIPO_VERIFICACION_CORREO, aviso.tipo());
        assertEquals(1, aviso.destinatarios().size());
        assertEquals("ESTUDIANTE", aviso.destinatarios().getFirst().rol());
        assertEquals(USUARIO_ESTUDIANTE, aviso.destinatarios().getFirst().idUsuario());
        assertNull(aviso.destinatarios().getFirst().correo());
        assertEquals("Ana Maria", aviso.datos().get("nombre"));
        final String texto = aviso.toString().toLowerCase();
        assertFalse(texto.contains("ana.perez@uco.edu.co"));
        assertFalse(texto.contains("token"));
    }

    @Test
    void los_rechazos_de_la_db_se_propagan_sin_registrar_aviso() {
        for (final RuntimeException rechazo : List.of(
                new ResourceNotFoundException("VCE_004", "El estudiante no pertenece a un grupo a su cargo."),
                new ConflictException("VCE_005", "El correo de este estudiante ya fue verificado."),
                new ConflictException("VCE_003", "Debe esperar unos minutos."),
                new ValidationException("GEN_002", "Datos invalidos."))) {
            final ReenviarVerificacionCorreoUseCaseImpl useCase = useCase(rechazo, ana(), avisos::add);

            assertThrows(rechazo.getClass(), () -> useCase.execute(ACTOR, GRUPO, ESTUDIANTE));
        }
        assertTrue(avisos.isEmpty());
    }

    @Test
    void si_el_usuario_no_se_resuelve_se_registra_el_aviso_sin_nombre_y_no_se_propaga_ningun_error() {
        useCase(null, Optional.empty(), avisos::add).execute(ACTOR, GRUPO, ESTUDIANTE);

        assertEquals(1, avisos.size());
        assertEquals("", avisos.getFirst().datos().getOrDefault("nombre", ""));
    }

    @Test
    void un_fallo_de_la_cola_si_se_propaga_porque_el_reenvio_es_la_accion_solicitada() {
        final ReenviarVerificacionCorreoUseCaseImpl useCase = useCase(null, ana(), aviso -> {
            throw new IllegalStateException("Cola caida.");
        });

        assertThrows(IllegalStateException.class, () -> useCase.execute(ACTOR, GRUPO, ESTUDIANTE));
    }

    @Test
    void los_identificadores_son_obligatorios() {
        final ReenviarVerificacionCorreoUseCaseImpl useCase = useCase(null, ana(), avisos::add);

        assertThrows(ValidationException.class, () -> useCase.execute(null, GRUPO, ESTUDIANTE));
        assertThrows(ValidationException.class, () -> useCase.execute(ACTOR, null, ESTUDIANTE));
        assertThrows(ValidationException.class, () -> useCase.execute(ACTOR, GRUPO, null));
        assertTrue(avisos.isEmpty());
    }

    @Test
    void los_colaboradores_son_obligatorios() {
        final UsuarioRepositoryPort usuariosPort = usuarios(ana());
        final VerificacionCorreoRepositoryPort repoPort = repositorio(null);
        final NotificacionPort notifPort = avisos::add;

        assertThrows(NullPointerException.class, () -> new ReenviarVerificacionCorreoUseCaseImpl(null, usuariosPort, notifPort));
        assertThrows(NullPointerException.class, () -> new ReenviarVerificacionCorreoUseCaseImpl(repoPort, null, notifPort));
        assertThrows(NullPointerException.class, () -> new ReenviarVerificacionCorreoUseCaseImpl(repoPort, usuariosPort, null));
    }
}
