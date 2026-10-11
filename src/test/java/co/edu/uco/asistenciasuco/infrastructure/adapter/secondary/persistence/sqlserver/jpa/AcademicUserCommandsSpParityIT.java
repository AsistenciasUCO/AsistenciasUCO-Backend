package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CierrePeriodoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * LB-008 JPA-03 — paridad REAL sobre SQL Server de los commands academicos y de usuario, ejercitados SOLO a
 * traves de los puertos de Application (mismo mecanismo que {@code SesionGrupoCommandsSpParityIT}).
 *
 * <p>Cada escenario imprime una linea {@code PARITY_OUTCOME|escenario|excepcion|codigo|mensaje} normalizada
 * (UUID sustituidos). El log del baseline JDBC (BEFORE) y del candidato JPA (AFTER) debe coincidir linea a linea.
 * Escenarios: errores funcionales deterministas (ids inexistentes, validacion) sin efectos DB. Los providers
 * publicos {@code usp_sincronizar_usuario} y {@code usp_registrar_o_actualizar_plan_estudio} los entrega el work
 * item DB CC-003G-01 (no existian en la DB publicada hasta ese work item; ver MAINT-003H). Los escenarios de
 * PlanEstudio y Usuario ejercitan el provider real con una entrada invalida deterministica; el camino de exito con
 * efectos DB, los duplicados y la autorizacion se certifican en {@code UsuarioPlanEstudioProvidersSqlServerIT}.</p>
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class AcademicUserCommandsSpParityIT {

    private static final String PREFIJO = "IT-LB008-JPA03-";

    @Autowired
    private AsignaturaCommandPort asignaturaCommandPort;

    @Autowired
    private CierrePeriodoCommandPort cierrePeriodoCommandPort;

    @Autowired
    private CoordinadorCommandPort coordinadorCommandPort;

    @Autowired
    private DecanoCommandPort decanoCommandPort;

    @Autowired
    private PlanEstudioCommandPort planEstudioCommandPort;

    @Autowired
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @BeforeEach
    void prepararCorrelacion() {
        CorrelationIdContext.set(UUID.randomUUID());
    }

    @Test
    void asignatura_crear_con_plan_inexistente_produce_el_mismo_resultado() {
        registrar("ASG_01_CREAR_PLAN_INEXISTENTE", () -> {
            asignaturaCommandPort.crearAsignatura(UUID.randomUUID(), PREFIJO + "A1", "Asignatura prueba",
                    3, UUID.randomUUID(), 1, "AREA-NO-EXISTE", "COMPONENTE-NO-EXISTE", UUID.randomUUID());
            return "OK";
        });
    }

    @Test
    void asignatura_actualizar_inexistente_produce_el_mismo_resultado() {
        registrar("ASG_02_ACTUALIZAR_INEXISTENTE", () -> {
            asignaturaCommandPort.actualizarAsignatura(UUID.randomUUID(), PREFIJO + "A2", "Asignatura prueba",
                    3, UUID.randomUUID(), 1, "AREA-NO-EXISTE", "COMPONENTE-NO-EXISTE");
            return "OK";
        });
    }

    @Test
    void asignatura_toggle_inexistente_produce_el_mismo_resultado() {
        registrar("ASG_03_TOGGLE_INEXISTENTE", () -> {
            asignaturaCommandPort.toggleEstadoAsignatura(UUID.randomUUID());
            return "OK";
        });
    }

    @Test
    void cierre_periodo_inexistente_produce_el_mismo_resultado() {
        registrar("CIE_01_PERIODO_INEXISTENTE", () -> {
            cierrePeriodoCommandPort.ejecutarCierreMasivoPeriodo(PREFIJO + "NOEXISTE", "ACTOR-IT", UUID.randomUUID());
            return "OK";
        });
    }

    @Test
    void coordinador_crear_con_facultad_inexistente_produce_el_mismo_resultado() {
        registrar("COO_01_CREAR_FACULTAD_INEXISTENTE", () -> {
            coordinadorCommandPort.crearCoordinador(UUID.randomUUID(), PREFIJO + "NUM", "Nombre", null,
                    "Apellido", null, PREFIJO + "coo@example.test", UUID.randomUUID(), UUID.randomUUID(),
                    "Clave-IT-LB008", UUID.randomUUID());
            return "OK";
        });
    }

    @Test
    void decano_crear_con_facultad_inexistente_produce_el_mismo_resultado() {
        registrar("DEC_01_CREAR_FACULTAD_INEXISTENTE", () -> {
            decanoCommandPort.crearDecano(new DecanoCommandPort.CrearDecanoCommand(
                    UUID.randomUUID(), null, 7, "Nombre", null, "Apellido", null,
                    PREFIJO + "dec@example.test", UUID.randomUUID(), "FACULTAD-NO-EXISTE",
                    "Clave-IT-LB008", UUID.randomUUID()));
            return "OK";
        });
    }

    @Test
    void plan_estudio_registrar_con_programa_inexistente_produce_el_mismo_resultado() {
        registrar("PLA_01_PROGRAMA_INEXISTENTE", () -> {
            planEstudioCommandPort.registrarOActualizarPlanEstudio(UUID.randomUUID(), UUID.randomUUID(), 2026,
                    UUID.randomUUID());
            return "OK";
        });
    }

    @Test
    void usuario_sincronizar_sin_password_produce_el_mismo_resultado() {
        registrar("USU_01_SIN_PASSWORD", () -> {
            usuarioRepositoryPort.crearUsuario(new CrearUsuarioRepositoryDTO(
                    UUID.randomUUID(), 7, "Apellido", null, "Nombre", null,
                    PREFIJO + "usu@example.test", null));
            return "OK";
        });
    }

    private void registrar(final String escenario, final Supplier<String> accion) {
        String excepcion = "NONE";
        String codigo = "NONE";
        String mensaje = "NONE";
        try {
            accion.get();
        } catch (RuntimeException exception) {
            excepcion = exception.getClass().getSimpleName();
            codigo = exception.getClass().getName();
            mensaje = String.valueOf(exception.getMessage());
        }
        System.out.println("PARITY_OUTCOME|" + escenario + "|" + excepcion + "|" + codigo + "|"
                + normalizar(mensaje));
    }

    private static String normalizar(final String texto) {
        return texto.replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "<UUID>");
    }
}



