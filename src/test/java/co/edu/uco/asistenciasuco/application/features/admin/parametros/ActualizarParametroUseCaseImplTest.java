package co.edu.uco.asistenciasuco.application.features.admin.parametros;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarparametro.primaryports.ActualizarParametroInputPort.Comando;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarparametro.primaryports.ActualizarParametroInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarparametro.usecase.impl.ActualizarParametroUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.exception.ParametroErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParametroEdicionCapacidadPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroAdminRepositoryPort.ParametroActualizado;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** CULM-001 D.3 (CONTRACT v2 §4.3, HU168): caso de uso de edicion de parametros; 100 % de ramas. */
class ActualizarParametroUseCaseImplTest {

    private static final UUID ADMIN = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID PARAMETRO = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final Instant ESPERADA = Instant.parse("2026-11-07T15:04:05.007Z");
    private static final LocalDateTime ESPERADA_UTC = LocalDateTime.of(2026, 11, 7, 15, 4, 5, 7_000_000);

    private final ParametroAdminRepositoryPort repositorio = mock(ParametroAdminRepositoryPort.class);
    private final ParametroEdicionCapacidadPort capacidad = mock(ParametroEdicionCapacidadPort.class);
    private final CatalogInvalidationPort invalidacion = mock(CatalogInvalidationPort.class);
    private final ActualizarParametroUseCaseImpl useCase = new ActualizarParametroUseCaseImpl(repositorio, capacidad, invalidacion);

    private static Comando comando() {
        return new Comando(PARAMETRO, "20", ESPERADA, "Ajuste de prueba", ADMIN);
    }

    private static ParametroActualizado actualizado(final String anterior, final String nuevo) {
        return new ParametroActualizado(PARAMETRO, "ASISTENCIA", "TOLERANCIA_MINUTOS", anterior, nuevo,
                LocalDateTime.of(2026, 11, 7, 16, 0, 0, 3_000_000));
    }

    @Test
    void con_un_proveedor_de_parametros_que_no_es_sqlserver_responde_prm_010_sin_tocar_nada() {
        when(capacidad.admiteEdicion()).thenReturn(false);

        final ConflictException error = assertThrows(ConflictException.class, () -> useCase.execute(comando()));

        assertEquals("PRM_010", error.getCode());
        assertEquals(ParametroErrorCode.PRM_010.defaultMessage(), error.getMessage());
        verifyNoInteractions(repositorio, invalidacion);
    }

    @Test
    void un_cambio_exitoso_invalida_el_parametro_una_sola_vez_y_devuelve_anterior_nuevo_y_version() {
        when(capacidad.admiteEdicion()).thenReturn(true);
        when(repositorio.actualizar(PARAMETRO, "20", ESPERADA_UTC, "Ajuste de prueba", ADMIN)).thenReturn(actualizado("15", "20"));

        final Resultado resultado = useCase.execute(comando());

        assertEquals(new Resultado(PARAMETRO, "ASISTENCIA", "TOLERANCIA_MINUTOS", "15", "20",
                Instant.parse("2026-11-07T16:00:00.003Z"), false), resultado);
        verify(invalidacion, times(1)).invalidateParameter("ASISTENCIA", "TOLERANCIA_MINUTOS");
    }

    @Test
    void el_mismo_valor_es_exito_sin_cambios_y_no_invalida_la_cache() {
        when(capacidad.admiteEdicion()).thenReturn(true);
        when(repositorio.actualizar(any(), anyString(), any(), anyString(), any())).thenReturn(actualizado("20", "20"));

        final Resultado resultado = useCase.execute(comando());

        assertTrue(resultado.sinCambios());
        verify(invalidacion, never()).invalidateParameter(anyString(), anyString());
    }

    @Test
    void un_fallo_al_invalidar_la_cache_no_revierte_el_cambio() {
        when(capacidad.admiteEdicion()).thenReturn(true);
        when(repositorio.actualizar(any(), anyString(), any(), anyString(), any())).thenReturn(actualizado("15", "20"));
        doThrow(new IllegalStateException("cache caida")).when(invalidacion).invalidateParameter(anyString(), anyString());

        final Resultado resultado = useCase.execute(comando());

        assertFalse(resultado.sinCambios());
        assertEquals("20", resultado.valorNuevo());
    }

    @Test
    void un_rechazo_de_la_base_de_datos_se_propaga_y_no_invalida() {
        when(capacidad.admiteEdicion()).thenReturn(true);
        when(repositorio.actualizar(any(), anyString(), any(), anyString(), any()))
                .thenThrow(new ConflictException(ParametroErrorCode.PRM_008));

        assertEquals("PRM_008", assertThrows(ConflictException.class, () -> useCase.execute(comando())).getCode());
        verifyNoInteractions(invalidacion);
    }

    @Test
    void la_forma_del_comando_se_valida_antes_de_consultar_la_capacidad() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(ValidationException.class, () -> useCase.execute(new Comando(null, "20", ESPERADA, "Ajuste de prueba", ADMIN)));
        assertThrows(ValidationException.class, () -> useCase.execute(new Comando(PARAMETRO, null, ESPERADA, "Ajuste de prueba", ADMIN)));
        assertThrows(ValidationException.class, () -> useCase.execute(new Comando(PARAMETRO, "20", null, "Ajuste de prueba", ADMIN)));
        assertThrows(ValidationException.class, () -> useCase.execute(new Comando(PARAMETRO, "20", ESPERADA, null, ADMIN)));
        assertThrows(ValidationException.class, () -> useCase.execute(new Comando(PARAMETRO, "20", ESPERADA, "Ajuste de prueba", null)));
        verifyNoInteractions(capacidad, repositorio, invalidacion);
    }

    @Test
    void las_dependencias_son_obligatorias() {
        assertThrows(NullPointerException.class, () -> new ActualizarParametroUseCaseImpl(null, capacidad, invalidacion));
        assertThrows(NullPointerException.class, () -> new ActualizarParametroUseCaseImpl(repositorio, null, invalidacion));
        assertThrows(NullPointerException.class, () -> new ActualizarParametroUseCaseImpl(repositorio, capacidad, null));
    }
}
