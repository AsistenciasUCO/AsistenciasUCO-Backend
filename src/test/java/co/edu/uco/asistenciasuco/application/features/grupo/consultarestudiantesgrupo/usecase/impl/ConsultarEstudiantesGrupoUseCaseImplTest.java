package co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.domain.ConsultarEstudiantesGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.entity.EstudianteGrupoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarEstudiantesGrupoUseCaseImplTest {

    private static final UUID GRUPO_PROPIO = UUID.randomUUID();
    private static final UUID GRUPO_AJENO = UUID.randomUUID();
    private static final UUID DOCENTE_A = UUID.randomUUID();
    private static final UUID DOCENTE_B = UUID.randomUUID();
    private static final UUID COORDINADOR = UUID.randomUUID();

    private final GrupoRepositoryPort grupoRepositoryPort = mock(GrupoRepositoryPort.class);
    private final InstitutionalScopePort institutionalScopePort = mock(InstitutionalScopePort.class);
    private final ConsultarEstudiantesGrupoUseCaseImpl useCase =
            new ConsultarEstudiantesGrupoUseCaseImpl(grupoRepositoryPort, institutionalScopePort);

    @Test
    void docente_propietario_del_grupo_consulta_sus_estudiantes() {
        when(institutionalScopePort.findDocenteIdByUsuario(DOCENTE_A)).thenReturn(Optional.of(UUID.randomUUID()));
        when(institutionalScopePort.canDocenteAccessGrupo(DOCENTE_A, GRUPO_PROPIO)).thenReturn(true);
        when(grupoRepositoryPort.consultarEstudiantesGrupo(GRUPO_PROPIO, DOCENTE_A)).thenReturn(List.of());

        final List<EstudianteGrupoEntity> resultado =
                useCase.execute(new ConsultarEstudiantesGrupoDomain(GRUPO_PROPIO, DOCENTE_A));

        assertEquals(0, resultado.size());
    }

    @Test
    void docente_ajeno_al_grupo_es_rechazado_con_forbidden() {
        when(institutionalScopePort.findDocenteIdByUsuario(DOCENTE_B)).thenReturn(Optional.of(UUID.randomUUID()));
        when(institutionalScopePort.canDocenteAccessGrupo(DOCENTE_B, GRUPO_AJENO)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(new ConsultarEstudiantesGrupoDomain(GRUPO_AJENO, DOCENTE_B)));
    }

    @Test
    void coordinador_sin_identidad_docente_conserva_visibilidad_institucional_amplia() {
        when(institutionalScopePort.findDocenteIdByUsuario(COORDINADOR)).thenReturn(Optional.empty());
        when(grupoRepositoryPort.consultarEstudiantesGrupo(GRUPO_AJENO, COORDINADOR))
                .thenReturn(List.of(new EstudianteGrupoRepositoryProjection(
                        UUID.randomUUID(), UUID.randomUUID(), "123", "Ana Perez", "ana@uco.edu.co", "ACTIVO", "Activo")));

        final List<EstudianteGrupoEntity> resultado =
                useCase.execute(new ConsultarEstudiantesGrupoDomain(GRUPO_AJENO, COORDINADOR));

        assertEquals(1, resultado.size());
    }
}
