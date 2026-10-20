package co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain.CrearDecanoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearDecanoUseCaseImplTest {

    private static final UUID FACULTAD_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID TIPO_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    private static final UUID USUARIO_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID USUARIO_EJECUTOR = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final String CORREO = "nuevo@uco.edu.co";
    private static final String RAW = "Clave123!";
    private static final String HASH = "HASH_CONTROLADO";

    private FacultadQueryPort facultad;
    private DecanoCommandPort command;
    private UsuarioRepositoryPort usuarios;
    private PasswordEncoderPort encoder;
    private IdentityProviderPort identity;
    private CrearDecanoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        facultad = mock(FacultadQueryPort.class);
        command = mock(DecanoCommandPort.class);
        usuarios = mock(UsuarioRepositoryPort.class);
        encoder = mock(PasswordEncoderPort.class);
        identity = mock(IdentityProviderPort.class);
        when(facultad.consultarFacultadPorId(FACULTAD_ID)).thenReturn(Optional.of(new FacultadProjection(
                FACULTAD_ID, "Facultad", null, null, null, null, true, "Activa"
        )));
        when(identity.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-1", true));
        useCase = new CrearDecanoUseCaseImpl(facultad, command, usuarios, encoder, identity);
    }

    @Test
    void usuario_nuevo_es_db_first_db_recibe_hash_identity_raw_y_role_decano() {
        final UsuarioIdentidadRepositoryProjection canonico = usuario(USUARIO_ID, 123456789, CORREO);
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789))
                .thenReturn(Optional.empty(), Optional.of(canonico));
        when(encoder.encode(RAW)).thenReturn(HASH);

        useCase.execute(domain(123456789, CORREO));

        final ArgumentCaptor<DecanoCommandPort.CrearDecanoCommand> db =
                ArgumentCaptor.forClass(DecanoCommandPort.CrearDecanoCommand.class);
        final ArgumentCaptor<CrearCuentaIdentidadDTO> idp = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        final InOrder orden = inOrder(command, usuarios, identity);
        orden.verify(command).crearDecano(db.capture());
        orden.verify(usuarios).consultarUsuarioPorIdentificacion(TIPO_ID, 123456789);
        orden.verify(identity).crearCuenta(idp.capture());
        assertEquals(HASH, db.getValue().password());
        assertEquals(TIPO_ID, db.getValue().tipoIdentificacionId());
        assertEquals(USUARIO_EJECUTOR, db.getValue().usuarioEjecutor());
        assertNotEquals(RAW, db.getValue().password());
        assertEquals(RAW, idp.getValue().passwordInicial());
        assertNotEquals(HASH, idp.getValue().passwordInicial());
        assertEquals(USUARIO_ID, idp.getValue().idUsuario());
        assertEquals("123456789", idp.getValue().username());
        assertEquals(CORREO, idp.getValue().correo());
        assertEquals(InstitutionalRole.DECANO, idp.getValue().rolInstitucional());
    }

    @Test
    void fallo_db_impide_identity() {
        when(encoder.encode(RAW)).thenReturn(HASH);
        doThrow(new IllegalStateException("Fallo DB")).when(command).crearDecano(any());

        assertThrows(IllegalStateException.class, () -> useCase.execute(domain(123456789, CORREO)));

        verifyNoInteractions(identity);
        verify(usuarios, never()).consultarUsuarioPorId(any());
    }

    @Test
    void correo_y_numero_de_usuarios_distintos_fallan_antes_del_command() {
        when(usuarios.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.of(usuario(USUARIO_ID, 111111111, CORREO)));
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789)).thenReturn(Optional.of(
                usuario(UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff"), 123456789, "otro@uco.edu.co")
        ));

        final ConflictException exception = assertThrows(ConflictException.class,
                () -> useCase.execute(domain(123456789, CORREO)));

        assertEquals("ERR_IDENTIDAD_USUARIO_CONFLICTO", exception.getCode());
        verifyNoInteractions(command, encoder, identity);
    }

    @Test
    void solo_correo_existente_es_conflicto_antes_de_db() {
        when(usuarios.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.of(usuario(USUARIO_ID, 111111111, CORREO)));

        final ConflictException exception = assertThrows(ConflictException.class,
                () -> useCase.execute(domain(123456789, CORREO)));

        assertEquals("ERR_IDENTIDAD_USUARIO_CONFLICTO", exception.getCode());
        verifyNoInteractions(command, encoder, identity);
    }

    @Test
    void solo_documento_existente_es_conflicto_antes_de_db() {
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789)).thenReturn(Optional.of(
                usuario(USUARIO_ID, 123456789, "viejo@uco.edu.co")
        ));

        final ConflictException exception = assertThrows(ConflictException.class,
                () -> useCase.execute(domain(123456789, CORREO)));

        assertEquals("ERR_IDENTIDAD_USUARIO_CONFLICTO", exception.getCode());
        verifyNoInteractions(command, encoder, identity);
    }

    @Test
    void existente_exacto_usa_perfil_canonico_y_password_db_null() {
        final UsuarioIdentidadRepositoryProjection previo = usuario(USUARIO_ID, 123456789, CORREO);
        final UsuarioIdentidadRepositoryProjection canonico = new UsuarioIdentidadRepositoryProjection(
                USUARIO_ID, TIPO_ID, 111111111, "JOSE", "PEREZ", "  Usuario@Test.com  "
        );
        when(usuarios.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.of(previo));
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789)).thenReturn(Optional.of(previo));
        when(usuarios.consultarUsuarioPorId(USUARIO_ID)).thenReturn(Optional.of(canonico));

        useCase.execute(domain(123456789, CORREO));

        final ArgumentCaptor<DecanoCommandPort.CrearDecanoCommand> db =
                ArgumentCaptor.forClass(DecanoCommandPort.CrearDecanoCommand.class);
        final ArgumentCaptor<CrearCuentaIdentidadDTO> idp = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        verify(command).crearDecano(db.capture());
        verify(identity).crearCuenta(idp.capture());
        assertEquals(null, db.getValue().password());
        assertEquals("111111111", idp.getValue().username());
        assertEquals("JOSE", idp.getValue().primerNombre());
        assertEquals("PEREZ", idp.getValue().primerApellido());
        assertEquals("  Usuario@Test.com  ", idp.getValue().correo());
        assertEquals(RAW, idp.getValue().passwordInicial());
        assertEquals(InstitutionalRole.DECANO, idp.getValue().rolInstitucional());
        verify(usuarios).consultarUsuarioPorId(USUARIO_ID);
        verifyNoInteractions(encoder);
    }

    @Test
    void usuario_post_command_ausente_falla_sin_identity() {
        final UsuarioIdentidadRepositoryProjection previo = usuario(USUARIO_ID, 123456789, CORREO);
        when(usuarios.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.of(previo));
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789)).thenReturn(Optional.of(previo));

        assertThrows(InternalApplicationException.class, () -> useCase.execute(domain(123456789, CORREO)));

        verify(command).crearDecano(any());
        verify(usuarios).consultarUsuarioPorId(USUARIO_ID);
        verifyNoInteractions(identity);
    }

    @Test
    void fallo_identity_despues_de_db_preserva_cause_sin_rollback_ni_eliminarCuenta() {
        final IdentityProviderPort.IdentityProviderException fallo =
                new IdentityProviderPort.IdentityProviderException("Fallo IdP");
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789)).thenReturn(
                Optional.empty(), Optional.of(usuario(USUARIO_ID, 123456789, CORREO))
        );
        when(encoder.encode(RAW)).thenReturn(HASH);
        when(identity.crearCuenta(any())).thenThrow(fallo);

        final InternalApplicationException exception = assertThrows(InternalApplicationException.class,
                () -> useCase.execute(domain(123456789, CORREO)));

        assertSame(fallo, exception.getCause());
        verify(command).crearDecano(any());
        verify(identity, never()).eliminarCuenta(any());
    }

    @Test
    void usuario_nuevo_sin_proyeccion_post_command_falla_sin_identity() {
        when(encoder.encode(RAW)).thenReturn(HASH);

        assertThrows(InternalApplicationException.class, () -> useCase.execute(domain(123456789, CORREO)));

        verify(command).crearDecano(any());
        verifyNoInteractions(identity);
    }

    @Test
    void usuario_nuevo_con_correo_canonico_distinto_falla_sin_identity() {
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789)).thenReturn(
                Optional.empty(), Optional.of(usuario(USUARIO_ID, 123456789, "otro@uco.edu.co"))
        );
        when(encoder.encode(RAW)).thenReturn(HASH);

        assertThrows(InternalApplicationException.class, () -> useCase.execute(domain(123456789, CORREO)));

        verify(command).crearDecano(any());
        verifyNoInteractions(identity);
    }

    @Test
    void usuario_post_command_incompleto_falla_sin_identity() {
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789)).thenReturn(
                Optional.empty(), Optional.of(new UsuarioIdentidadRepositoryProjection(
                        USUARIO_ID, TIPO_ID, 123456789, null, "PEREZ", CORREO
                ))
        );
        when(encoder.encode(RAW)).thenReturn(HASH);

        assertThrows(InternalApplicationException.class, () -> useCase.execute(domain(123456789, CORREO)));

        verify(command).crearDecano(any());
        verifyNoInteractions(identity);
    }

    @Test
    void tipo_identificacion_requerido_antes_de_cualquier_dependencia() {
        final CrearDecanoDomain sinTipo = new CrearDecanoDomain(
                null, 123456789, "Ana", "Maria", "Perez", "Gomez", CORREO, RAW, FACULTAD_ID, USUARIO_EJECUTOR
        );

        assertThrows(ValidationException.class, () -> useCase.execute(sinTipo));

        verifyNoInteractions(command, usuarios, encoder, identity);
    }

    @Test
    void crear_decano_sin_facultad_no_consulta_facultad_y_persiste_con_idFacultad_null() {
        final UsuarioIdentidadRepositoryProjection canonico = usuario(USUARIO_ID, 123456789, CORREO);
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789))
                .thenReturn(Optional.empty(), Optional.of(canonico));
        when(encoder.encode(RAW)).thenReturn(HASH);

        final CrearDecanoDomain sinFacultad = new CrearDecanoDomain(
                TIPO_ID, 123456789, "Ana", "Maria", "Perez", "Gomez", CORREO, RAW, null, USUARIO_EJECUTOR
        );

        useCase.execute(sinFacultad);

        final ArgumentCaptor<DecanoCommandPort.CrearDecanoCommand> db =
                ArgumentCaptor.forClass(DecanoCommandPort.CrearDecanoCommand.class);
        verify(command).crearDecano(db.capture());
        verify(facultad, never()).consultarFacultadPorId(any());
        assertNull(db.getValue().idFacultad());
        assertNull(db.getValue().nombreFacultad());
        verify(identity).crearCuenta(any());
    }

    @Test
    void crear_decano_con_facultad_inexistente_falla_con_resource_not_found() {
        final UUID facultadInexistente = UUID.randomUUID();
        when(facultad.consultarFacultadPorId(facultadInexistente)).thenReturn(Optional.empty());

        final CrearDecanoDomain conFacultadInexistente = new CrearDecanoDomain(
                TIPO_ID, 123456789, "Ana", "Maria", "Perez", "Gomez", CORREO, RAW, facultadInexistente, USUARIO_EJECUTOR
        );

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(conFacultadInexistente));
        verifyNoInteractions(command, usuarios, encoder, identity);
    }

    private CrearDecanoDomain domain(final int numero, final String correo) {
        return new CrearDecanoDomain(
                TIPO_ID, numero, "Ana", "Maria", "Perez", "Gomez", correo, RAW, FACULTAD_ID, USUARIO_EJECUTOR
        );
    }

    private UsuarioIdentidadRepositoryProjection usuario(final UUID id, final int numero, final String correo) {
        return new UsuarioIdentidadRepositoryProjection(id, TIPO_ID, numero, "ANA", "PEREZ", correo);
    }
}
