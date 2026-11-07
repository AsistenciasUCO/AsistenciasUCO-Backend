package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteBusquedaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CULM-001 A.2-15 (A2-B-19, REVIEW-3 B1): matricula manual de un estudiante encontrado, sin correo en la solicitud. El
 * estudiante se resuelve con la busqueda por documento (titularidad, fila Estudiante y misma institucion en el SP); el
 * correo real sale del idEstudiante devuelto. Sin resultado: 400 FIELD_REQUIRED (correo) sin llamar al SP de matricula.
 */
class RegistrarEstudianteSinCorreoUseCaseImplTest {

    private static final UUID TIPO = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID DOCENTE = UUID.fromString("53641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID ESTUDIANTE = UUID.fromString("63641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO = UUID.fromString("73641bab-e3cd-485c-b275-47e7b731e18c");
    private static final int DOCUMENTO = 1017123456;

    private final GrupoRepositoryPort grupos = mock(GrupoRepositoryPort.class);
    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final PasswordEncoderPort encoder = mock(PasswordEncoderPort.class);
    private final IdentityProviderPort identidad = mock(IdentityProviderPort.class);
    private final CorreoInstitucionalPolicy correoPolicy = mock(CorreoInstitucionalPolicy.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);
    private final RegistrarEstudianteUseCaseImpl useCase =
            new RegistrarEstudianteUseCaseImpl(grupos, usuarios, encoder, identidad, correoPolicy, notificaciones);

    private static RegistrarEstudianteDomain pendiente() {
        return RegistrarEstudianteDomain.pendienteDeResolverPorDocumento(TIPO, DOCUMENTO, null, GRUPO, DOCENTE);
    }

    private static EstudianteBusquedaRepositoryProjection encontrado() {
        return new EstudianteBusquedaRepositoryProjection(ESTUDIANTE, "Ana", "Maria", "Perez", "Gomez", "ANA MARIA PEREZ GOMEZ",
                DOCUMENTO, "a***@uco.edu.co", "I");
    }

    @Test
    void con_un_resultado_resuelve_el_correo_por_estudiante_y_matricula_con_la_regla_idn_001() {
        when(grupos.buscarEstudiantePorDocumento(GRUPO, TIPO, DOCUMENTO, DOCENTE)).thenReturn(Optional.of(encontrado()));
        when(usuarios.consultarCorreoPorEstudiante(ESTUDIANTE)).thenReturn(Optional.of("ana.perez@uco.edu.co"));
        final UsuarioIdentidadRepositoryProjection usuario =
                new UsuarioIdentidadRepositoryProjection(USUARIO, TIPO, DOCUMENTO, "ANA", "PEREZ", "ana.perez@uco.edu.co");
        when(usuarios.consultarUsuarioPorCorreo("ana.perez@uco.edu.co")).thenReturn(Optional.of(usuario));
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO, DOCUMENTO)).thenReturn(Optional.of(usuario));
        when(grupos.registrarEstudianteEnGrupo(any())).thenReturn(new RegistrarEstudianteRepositoryProjection("Estudiante matriculado."));
        when(identidad.crearCuenta(any())).thenReturn(mock(CuentaIdentidadDTO.class));

        assertEquals("Estudiante matriculado.", useCase.execute(pendiente()).getMensajeUsuario());

        final ArgumentCaptor<RegistrarEstudianteRepositoryDTO> dto = ArgumentCaptor.forClass(RegistrarEstudianteRepositoryDTO.class);
        verify(grupos).registrarEstudianteEnGrupo(dto.capture());
        assertEquals("ana.perez@uco.edu.co", dto.getValue().getCorreo());
        assertEquals("ANA", dto.getValue().getPrimerNombre());
        assertEquals("PEREZ", dto.getValue().getPrimerApellido());
        assertEquals(GRUPO, dto.getValue().getGrupoId());
        // Usuario existente: no se revalida el dominio, no se cifra clave nueva ni se envia verificacion.
        verify(correoPolicy, never()).exigir(any());
        verify(encoder, never()).encode(any());
        verify(notificaciones, never()).notificar(any());
    }

    @Test
    void sin_resultado_por_documento_inexistente_de_otra_institucion_o_sin_fila_estudiante_responde_400_sin_matricular() {
        // El SP devuelve 0 filas en los tres casos (documento inexistente, usuario de otra institucion, usuario sin fila
        // Estudiante): no se distinguen y ninguno llega al SP de matricula.
        when(grupos.buscarEstudiantePorDocumento(GRUPO, TIPO, DOCUMENTO, DOCENTE)).thenReturn(Optional.empty());

        final ValidationException error = assertThrows(ValidationException.class, () -> useCase.execute(pendiente()));

        assertEquals("FIELD_REQUIRED", error.getCode());
        verify(grupos, never()).registrarEstudianteEnGrupo(any());
        verify(usuarios, never()).consultarUsuarioPorIdentificacion(any(), any());
        verify(usuarios, never()).consultarCorreoPorEstudiante(any());
        verify(identidad, never()).crearCuenta(any());
    }

    @Test
    void sin_correo_registrado_para_el_estudiante_encontrado_tambien_responde_400_sin_matricular() {
        when(grupos.buscarEstudiantePorDocumento(GRUPO, TIPO, DOCUMENTO, DOCENTE)).thenReturn(Optional.of(encontrado()));
        when(usuarios.consultarCorreoPorEstudiante(ESTUDIANTE)).thenReturn(Optional.empty());

        assertEquals("FIELD_REQUIRED", assertThrows(ValidationException.class, () -> useCase.execute(pendiente())).getCode());
        verify(grupos, never()).registrarEstudianteEnGrupo(any());
        verify(usuarios, never()).consultarUsuarioPorIdentificacion(any(), any());
    }

    @Test
    void sin_documento_responde_400_sin_buscar() {
        final RegistrarEstudianteDomain sinDocumento =
                RegistrarEstudianteDomain.pendienteDeResolverPorDocumento(null, null, null, GRUPO, DOCENTE);

        assertEquals("FIELD_REQUIRED", assertThrows(ValidationException.class, () -> useCase.execute(sinDocumento)).getCode());
        verify(grupos, never()).buscarEstudiantePorDocumento(any(), any(), any(), any());
    }

    @Test
    void docente_no_titular_recibe_el_403_de_la_busqueda_sin_matricular() {
        final ForbiddenException noTitular = new ForbiddenException("SEC_002", "No tiene titularidad sobre el grupo.");
        when(grupos.buscarEstudiantePorDocumento(GRUPO, TIPO, DOCUMENTO, DOCENTE)).thenThrow(noTitular);

        assertSame(noTitular, assertThrows(ForbiddenException.class, () -> useCase.execute(pendiente())));
        verify(grupos, never()).registrarEstudianteEnGrupo(any());
    }
}
