package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.RegistrarEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity.RegistrarEstudianteResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteDetalleRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteResumenRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrarEstudianteInteractorTest {

    private static final UUID GRUPO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USUARIO_EJECUTOR = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TIPO_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ESTUDIANTE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private RegistrarEstudianteUseCase useCase;
    private UsuarioRepositoryPort usuarioRepositoryPort;
    private EstudianteRepositoryPort estudianteRepositoryPort;
    private RegistrarEstudianteInteractor interactor;

    @BeforeEach
    void setUp() {
        useCase = mock(RegistrarEstudianteUseCase.class);
        usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        estudianteRepositoryPort = mock(EstudianteRepositoryPort.class);
        interactor = new RegistrarEstudianteInteractor(useCase, usuarioRepositoryPort, estudianteRepositoryPort);
    }

    @Test
    void execute_con_datos_completos_delega_directamente_al_caso_de_uso() {
        final RegistrarEstudianteDTO dto = new RegistrarEstudianteDTO(
                TIPO_ID,
                1017123456,
                "Perez",
                "Gomez",
                "Carlos",
                "Andres",
                "carlos.perez@uco.edu.co",
                "Temporal2026*",
                GRUPO_ID,
                USUARIO_EJECUTOR
        );
        when(useCase.execute(any(RegistrarEstudianteDomain.class)))
                .thenReturn(new RegistrarEstudianteResultadoEntity(true, "Estudiante registrado."));

        final RegistrarEstudianteResultadoDTO resultado = interactor.execute(dto);

        assertNotNull(resultado);
        assertTrue(resultado.isExitoso());
        assertEquals("Estudiante registrado.", resultado.getMensajeUsuario());

        final ArgumentCaptor<RegistrarEstudianteDomain> captor = ArgumentCaptor.forClass(RegistrarEstudianteDomain.class);
        verify(useCase).execute(captor.capture());
        final RegistrarEstudianteDomain domain = captor.getValue();
        assertEquals(TIPO_ID, domain.getTipoIdentificacionId());
        assertEquals(1017123456, domain.getNumeroIdentificacion());
        assertEquals("CARLOS", domain.getPrimerNombre());
        assertEquals("PEREZ", domain.getPrimerApellido());
        assertEquals("carlos.perez@uco.edu.co", domain.getCorreo());
    }

    @Test
    void execute_con_estudianteId_resuelve_datos_personales_y_delega_al_caso_de_uso() {
        final RegistrarEstudianteDTO dto = new RegistrarEstudianteDTO();
        dto.setEstudianteId(ESTUDIANTE_ID);
        dto.setGrupoId(GRUPO_ID);
        dto.setUsuarioEjecutor(USUARIO_EJECUTOR);

        final EstudianteResumenRepositoryProjection resumen = new EstudianteResumenRepositoryProjection(
                ESTUDIANTE_ID,
                UUID.randomUUID(),
                TIPO_ID,
                1017123456,
                "Perez",
                "Gomez",
                "Carlos",
                "Andres",
                "Carlos Andres Perez Gomez",
                "carlos.perez@uco.edu.co",
                true
        );
        final EstudianteDetalleRepositoryProjection detalle = new EstudianteDetalleRepositoryProjection(
                resumen,
                Collections.emptyList()
        );
        when(estudianteRepositoryPort.consultarEstudiantePorId(ESTUDIANTE_ID)).thenReturn(Optional.of(detalle));
        when(useCase.execute(any(RegistrarEstudianteDomain.class)))
                .thenReturn(new RegistrarEstudianteResultadoEntity(true, "Estudiante matriculado."));

        final RegistrarEstudianteResultadoDTO resultado = interactor.execute(dto);

        assertNotNull(resultado);
        assertTrue(resultado.isExitoso());
        assertEquals("Estudiante matriculado.", resultado.getMensajeUsuario());

        final ArgumentCaptor<RegistrarEstudianteDomain> captor = ArgumentCaptor.forClass(RegistrarEstudianteDomain.class);
        verify(useCase).execute(captor.capture());
        final RegistrarEstudianteDomain domain = captor.getValue();
        assertEquals(TIPO_ID, domain.getTipoIdentificacionId());
        assertEquals(1017123456, domain.getNumeroIdentificacion());
        assertEquals("CARLOS", domain.getPrimerNombre());
        assertEquals("PEREZ", domain.getPrimerApellido());
    }

    @Test
    void execute_con_estudianteId_inexistente_lanza_ResourceNotFoundException() {
        final RegistrarEstudianteDTO dto = new RegistrarEstudianteDTO();
        dto.setEstudianteId(ESTUDIANTE_ID);
        dto.setGrupoId(GRUPO_ID);
        dto.setUsuarioEjecutor(USUARIO_EJECUTOR);

        when(estudianteRepositoryPort.consultarEstudiantePorId(ESTUDIANTE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> interactor.execute(dto));
    }

    @Test
    void execute_con_solo_identificacion_resuelve_usuario_existente_y_delega_al_caso_de_uso() {
        final RegistrarEstudianteDTO dto = new RegistrarEstudianteDTO();
        dto.setTipoIdentificacionId(TIPO_ID);
        dto.setNumeroIdentificacion(1017123456);
        dto.setGrupoId(GRUPO_ID);
        dto.setUsuarioEjecutor(USUARIO_EJECUTOR);

        final UsuarioIdentidadRepositoryProjection usuario = new UsuarioIdentidadRepositoryProjection(
                UUID.randomUUID(),
                TIPO_ID,
                1017123456,
                "Maria",
                "Rodriguez",
                "maria.rodriguez@uco.edu.co"
        );
        when(usuarioRepositoryPort.consultarUsuarioPorIdentificacion(TIPO_ID, 1017123456))
                .thenReturn(Optional.of(usuario));
        when(useCase.execute(any(RegistrarEstudianteDomain.class)))
                .thenReturn(new RegistrarEstudianteResultadoEntity(true, "Estudiante matriculado."));

        final RegistrarEstudianteResultadoDTO resultado = interactor.execute(dto);

        assertNotNull(resultado);
        assertTrue(resultado.isExitoso());

        final ArgumentCaptor<RegistrarEstudianteDomain> captor = ArgumentCaptor.forClass(RegistrarEstudianteDomain.class);
        verify(useCase).execute(captor.capture());
        final RegistrarEstudianteDomain domain = captor.getValue();
        assertEquals("MARIA", domain.getPrimerNombre());
        assertEquals("RODRIGUEZ", domain.getPrimerApellido());
        assertEquals("maria.rodriguez@uco.edu.co", domain.getCorreo());
    }

    @Test
    void execute_con_solo_identificacion_no_encontrada_lanza_ValidationException() {
        final RegistrarEstudianteDTO dto = new RegistrarEstudianteDTO();
        dto.setTipoIdentificacionId(TIPO_ID);
        dto.setNumeroIdentificacion(1017999999);
        dto.setGrupoId(GRUPO_ID);
        dto.setUsuarioEjecutor(USUARIO_EJECUTOR);

        when(usuarioRepositoryPort.consultarUsuarioPorIdentificacion(TIPO_ID, 1017999999))
                .thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> interactor.execute(dto));
    }
}
