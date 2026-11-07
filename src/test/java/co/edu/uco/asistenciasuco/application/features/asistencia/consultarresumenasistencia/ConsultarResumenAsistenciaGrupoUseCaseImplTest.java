package co.edu.uco.asistenciasuco.application.features.asistencia.consultarresumenasistencia;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.ResumenAsistenciaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarresumenasistencia.primaryports.ConsultarResumenAsistenciaGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarresumenasistencia.usecase.impl.ConsultarResumenAsistenciaGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 B.2 (F-DOC-004, CONTRACT 3.2.8): sabana del docente por horas desde el SP paginado. */
class ConsultarResumenAsistenciaGrupoUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final ResumenAsistenciaQueryPort resumen = mock(ResumenAsistenciaQueryPort.class);
    private final ConsultarResumenAsistenciaGrupoUseCaseImpl useCase = new ConsultarResumenAsistenciaGrupoUseCaseImpl(scope, resumen);

    static IndicadoresAsistenciaDTO indicadores() {
        return new IndicadoresAsistenciaDTO(10, new BigDecimal("20.00"), new BigDecimal("16.00"), new BigDecimal("3.00"),
                new BigDecimal("1.00"), BigDecimal.ZERO, new BigDecimal("15.00"), new BigDecimal("80.00"), "RIESGO",
                new BigDecimal("15.00"), new BigDecimal("20.00"));
    }

    @Test
    void el_titular_obtiene_la_pagina_con_los_indicadores_de_la_db_y_valores_por_defecto() {
        final UUID estudiante = UUID.randomUUID();
        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(true);
        when(resumen.consultarResumenGrupoDocente(GRUPO, 1, 50, USUARIO)).thenReturn(new PaginaResultado<>(List.of(
                new ResumenAsistenciaQueryPort.FilaResumen(GRUPO, estudiante, "Ana Perez", "A", indicadores())), 1, 50, 31));

        final PaginaResultado<ResumenAsistenciaEstudianteDTO> pagina = useCase.execute(
                new ConsultarResumenAsistenciaGrupoInputPort.Consulta(GRUPO, null, null, USUARIO));

        assertEquals(31, pagina.totalElementos());
        assertEquals(new ResumenAsistenciaEstudianteDTO(estudiante, "Ana Perez", "A", indicadores()), pagina.elementos().getFirst());
    }

    @Test
    void un_docente_no_titular_recibe_403_sin_consultar() {
        assertThrows(ForbiddenException.class, () -> useCase.execute(
                new ConsultarResumenAsistenciaGrupoInputPort.Consulta(GRUPO, 2, 10, USUARIO)));
        verify(resumen, never()).consultarResumenGrupoDocente(any(), anyInt(), anyInt(), any());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(NullPointerException.class, () -> new ConsultarResumenAsistenciaGrupoUseCaseImpl(null, resumen));
        assertThrows(NullPointerException.class, () -> new ConsultarResumenAsistenciaGrupoUseCaseImpl(scope, null));
    }

    @Test
    void indicadores_sin_sesiones_son_ceros_con_los_umbrales_dados() {
        final IndicadoresAsistenciaDTO vacio = IndicadoresAsistenciaDTO.sinSesiones(new BigDecimal("15.00"), null);

        assertEquals(0, vacio.sesionesEjecutadas());
        assertEquals(BigDecimal.ZERO, vacio.horasDictadas());
        assertEquals(null, vacio.codigoPermanencia());
        assertEquals(new BigDecimal("15.00"), vacio.umbralAdvertencia());
    }
}
