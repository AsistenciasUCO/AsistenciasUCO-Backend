package co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano;

import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.KpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.mapper.ConsultarKpisInstitucionMapper;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain.ConsultarKpisInstitucionDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain.KpisInstitucionDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.impl.ConsultarKpisInstitucionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.KpisDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.mapper.ConsultarKpisDecanoMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.ConsultarKpisDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.KpisDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.impl.ConsultarKpisDecanoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdminIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisDecanoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisInstitucionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CULM-001 B.2 (CONTRACT 2.9, HU102/HU133): los KPIs de decano e institucion exponen las columnas de horas que la DB agrega
 * al final (tomasDictadas, horasDictadas, horasFalta) sin recalcularlas; la forma previa queda con cero y nulos.
 */
class KpisHorasDecanoInstitucionTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final BigDecimal DIEZ = new BigDecimal("10.00");

    @Test
    void decano_propaga_las_horas_de_la_db_hasta_el_dto() {
        final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
        when(scope.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        final DecanoIndicadoresQueryPort port = mock(DecanoIndicadoresQueryPort.class);
        when(port.consultarKpis(ACTOR, null)).thenReturn(new KpisDecanoProjection(UUID.randomUUID(), "F", null, null, 1, 2, 1, 1, 0,
                DIEZ, 3, 1, DIEZ, DIEZ, 1, 40, 2, DIEZ, DIEZ, DIEZ, 7, new BigDecimal("14.00"), new BigDecimal("2.00")));

        final KpisDecanoDomain domain = new ConsultarKpisDecanoUseCaseImpl(scope, port).execute(new ConsultarKpisDecanoDomain(ACTOR, null));
        final KpisDecanoDTO dto = ConsultarKpisDecanoMapper.toDto(domain);

        assertEquals(7, dto.tomasDictadas());
        assertEquals(new BigDecimal("14.00"), dto.horasDictadas());
        assertEquals(new BigDecimal("2.00"), dto.horasFalta());
        final KpisDecanoDTO previo = new KpisDecanoDTO(null, null, null, null, 0, 0, 0, 0, 0, null, 0, 0, null, null, 0, 0, 0, null, null, null);
        assertEquals(0, previo.tomasDictadas());
        assertNull(previo.horasDictadas());
    }

    @Test
    void institucion_propaga_las_horas_de_la_db_hasta_el_dto() {
        final AdminIndicadoresQueryPort port = mock(AdminIndicadoresQueryPort.class);
        when(port.consultarKpis(ACTOR, null)).thenReturn(new KpisInstitucionProjection(UUID.randomUUID(), "I", null, null, 1, 2, 3, 1, 1, 1,
                DIEZ, 4, 1, DIEZ, DIEZ, 4, 4, DIEZ, 3, 3, 1, 1, DIEZ, DIEZ, 9, new BigDecimal("18.00"), new BigDecimal("3.00")));

        final KpisInstitucionDomain domain = new ConsultarKpisInstitucionUseCaseImpl(port).execute(new ConsultarKpisInstitucionDomain(ACTOR, null));
        final KpisInstitucionDTO dto = ConsultarKpisInstitucionMapper.toDto(domain);

        assertEquals(9, dto.tomasDictadas());
        assertEquals(new BigDecimal("18.00"), dto.horasDictadas());
        assertEquals(new BigDecimal("3.00"), dto.horasFalta());
        assertEquals(0, new KpisInstitucionDomain(null, null, null, null, 0, 0, 0, 0, 0, 0, null, 0, 0, null, null, 0, 0, null, 0, 0, 0, 0,
                null, null).tomasDictadas());
    }
}
