package co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.usecase.domain.TipoProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.TipoProgramaProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarTiposProgramaUseCaseImplTest {

    private final TipoProgramaQueryPort queryPort = mock(TipoProgramaQueryPort.class);
    private final ConsultarTiposProgramaUseCaseImpl useCase = new ConsultarTiposProgramaUseCaseImpl(queryPort);

    @Test
    void execute_retorna_catalogo_de_tipos_de_programa() {
        final UUID idTipo = UUID.randomUUID();
        when(queryPort.consultarTiposPrograma()).thenReturn(List.of(
                new TipoProgramaProjection(idTipo, "pregrado", true, "SI")
        ));

        final List<TipoProgramaDomain> result = useCase.execute();
        assertEquals(1, result.size());
        assertEquals("pregrado", result.get(0).nombre());
    }
}
