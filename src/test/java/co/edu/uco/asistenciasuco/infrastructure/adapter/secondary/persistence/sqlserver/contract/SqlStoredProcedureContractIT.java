package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.contract;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Certifica que cada stored procedure publico consumido por el backend coincide, parametro a
 * parametro, con el contrato real vigente en {@code gestionasistenciadb}. Cada procedimiento se
 * valida como un caso independiente (via {@link ParameterizedTest}) para que un mismatch en uno
 * no oculte el resultado de los demas.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class SqlStoredProcedureContractIT {

    private static final String SQL_PARAMETROS_POR_PROCEDIMIENTO = """
            SELECT
                p.name AS parameterName,
                TYPE_NAME(p.user_type_id) AS typeName,
                p.is_output AS output
            FROM sys.objects o
            INNER JOIN sys.parameters p ON p.object_id = o.object_id
            WHERE SCHEMA_NAME(o.schema_id) = 'dbo'
              AND o.type = 'P'
              AND o.name = ?
            ORDER BY p.parameter_id
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest(name = "{0}")
    @MethodSource("contratosEsperados")
    void stored_procedure_publico_coincide_con_el_contrato_real_de_la_db(
            final String procedureName,
            final List<SqlParameterContract> expectedParameters
    ) {
        final List<SqlParameterContract> actualParameters = jdbcTemplate.query(
                SQL_PARAMETROS_POR_PROCEDIMIENTO,
                (resultSet, rowNum) -> new SqlParameterContract(
                        resultSet.getString("parameterName"),
                        resultSet.getString("typeName"),
                        resultSet.getBoolean("output")
                ),
                procedureName
        );

        assertTrue(
                !actualParameters.isEmpty(),
                () -> "Contrato SQL incompatible:\n" + procedureName + "\nesperaba procedimiento almacenado existente"
        );

        for (final SqlParameterContract expectedParameter : expectedParameters) {
            if (!actualParameters.contains(expectedParameter)) {
                fail("Contrato SQL incompatible:\n"
                        + procedureName
                        + "\nesperaba parametro "
                        + expectedParameter.parameterName()
                        + "\nparametros reales: " + actualParameters);
            }
        }

        assertEquals(
                expectedParameters,
                actualParameters,
                () -> "Contrato SQL incompatible:\n" + procedureName + "\nparametros actuales no coinciden con el backend"
        );
    }

    /**
     * {@code sys.parameters.has_default_value} NO distingue de forma confiable "sin default" de
     * "default = NULL" (limitacion documentada de metadata de SQL Server: el propio
     * {@code default_value} queda NULL en ambos casos). Por eso la nulabilidad de
     * {@code @idUsuarioEjecutor} se verifica contra el texto fuente real via
     * {@code OBJECT_DEFINITION}, no contra {@code sys.parameters}.
     */
    @Test
    void idUsuarioEjecutor_de_registrar_asistencias_sesion_permite_null_segun_texto_fuente_real() {
        final String definition = jdbcTemplate.queryForObject(
                "SELECT OBJECT_DEFINITION(OBJECT_ID('dbo.usp_registrar_asistencias_sesion'))",
                String.class
        );

        assertTrue(definition != null && definition.replaceAll("\\s+", " ")
                        .contains("@idUsuarioEjecutor UNIQUEIDENTIFIER = NULL"),
                () -> "El contrato fuente de usp_registrar_asistencias_sesion cambio: "
                        + "@idUsuarioEjecutor ya no declara '= NULL' como valor por defecto.");
    }

    /**
     * Inventario completo de los 19 stored procedures publicos realmente invocados por los
     * adapters SQL Server del backend (ver auditoria de esta microfase). Cada entrada refleja el
     * contrato vigente en la DB real, incluyendo los procedimientos donde {@code @idUsuarioEjecutor}
     * fue agregado y, en {@code usp_crear_decano}, donde {@code @idTipoIdIdentificacion} fue retirado.
     */
    private static Stream<Arguments> contratosEsperados() {
        return Stream.of(
                Arguments.of("usp_sincronizar_usuario", List.of(
                        input("@idTipoIdIdentificacion", "uniqueidentifier"),
                        input("@numeroIdentificacion", "int"),
                        input("@primerApellido", "nvarchar"),
                        input("@segundoApellido", "nvarchar"),
                        input("@primerNombre", "nvarchar"),
                        input("@segundoNombre", "nvarchar"),
                        input("@correo", "nvarchar"),
                        input("@password", "nvarchar"),
                        input("@idCorrelacion", "uniqueidentifier")
                )),
                Arguments.of("usp_crear_decano", List.of(
                        input("@idDecano", "uniqueidentifier"),
                        input("@numeroIdentificacion", "int"),
                        input("@primerNombre", "nvarchar"),
                        input("@segundoNombre", "nvarchar"),
                        input("@primerApellido", "nvarchar"),
                        input("@segundoApellido", "nvarchar"),
                        input("@correo", "nvarchar"),
                        input("@idFacultad", "uniqueidentifier"),
                        input("@nombreFacultad", "nvarchar"),
                        input("@password", "nvarchar"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_crear_coordinador", List.of(
                        input("@idCoordinador", "uniqueidentifier"),
                        input("@numeroIdentificacion", "int"),
                        input("@primerNombre", "nvarchar"),
                        input("@segundoNombre", "nvarchar"),
                        input("@primerApellido", "nvarchar"),
                        input("@segundoApellido", "nvarchar"),
                        input("@correo", "nvarchar"),
                        input("@idPrograma", "uniqueidentifier"),
                        input("@idFacultad", "uniqueidentifier"),
                        input("@password", "nvarchar"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_crear_asignatura", List.of(
                        input("@idAsignatura", "uniqueidentifier"),
                        input("@codigo", "nvarchar"),
                        input("@nombre", "nvarchar"),
                        input("@creditos", "int"),
                        input("@idPlanEstudio", "uniqueidentifier"),
                        input("@semestreNumero", "int"),
                        input("@nombreArea", "nvarchar"),
                        input("@nombreComponente", "nvarchar"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_actualizar_asignatura", List.of(
                        input("@idAsignatura", "uniqueidentifier"),
                        input("@codigo", "nvarchar"),
                        input("@nombre", "nvarchar"),
                        input("@creditos", "int"),
                        input("@idPlanEstudio", "uniqueidentifier"),
                        input("@semestreNumero", "int"),
                        input("@nombreArea", "nvarchar"),
                        input("@nombreComponente", "nvarchar"),
                        input("@idCorrelacion", "uniqueidentifier")
                )),
                Arguments.of("usp_toggle_estado_asignatura", List.of(
                        input("@idAsignatura", "uniqueidentifier"),
                        input("@idCorrelacion", "uniqueidentifier")
                )),
                Arguments.of("usp_registrar_o_actualizar_plan_estudio", List.of(
                        input("@idPlanEstudio", "uniqueidentifier"),
                        input("@idPrograma", "uniqueidentifier"),
                        input("@inp", "int"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_ejecutar_cierre_masivo_periodo", List.of(
                        input("@codigoPeriodo", "nvarchar"),
                        input("@idActor", "nvarchar"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_crear_grupo", List.of(
                        input("@idGrupo", "uniqueidentifier"),
                        input("@idAsignatura", "uniqueidentifier"),
                        input("@idPeriodoAcademico", "uniqueidentifier"),
                        input("@codigo", "int"),
                        input("@nombre", "nvarchar"),
                        input("@idDocente", "uniqueidentifier"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_actualizar_grupo", List.of(
                        input("@idGrupo", "uniqueidentifier"),
                        input("@codigo", "int"),
                        input("@nombre", "nvarchar"),
                        input("@idDocente", "uniqueidentifier"),
                        input("@cupoMaximo", "int"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_generar_sesiones_grupo", List.of(
                        input("@idGrupo", "uniqueidentifier"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_registrar_estudiante_en_grupo", List.of(
                        input("@idGrupo", "uniqueidentifier"),
                        input("@numeroIdentificacion", "int"),
                        input("@primerNombre", "nvarchar"),
                        input("@segundoNombre", "nvarchar"),
                        input("@primerApellido", "nvarchar"),
                        input("@segundoApellido", "nvarchar"),
                        input("@correo", "nvarchar"),
                        input("@password", "nvarchar"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier"),
                        input("@idTipoIdIdentificacion", "uniqueidentifier")
                )),
                Arguments.of("usp_crear_sesion", List.of(
                        input("@idGrupo", "uniqueidentifier"),
                        input("@nombre", "nvarchar"),
                        input("@fechaHoraInicio", "datetime2"),
                        input("@fechaHoraFin", "datetime2"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_actualizar_sesion", List.of(
                        input("@idSesion", "uniqueidentifier"),
                        input("@nombre", "nvarchar"),
                        input("@fechaHoraInicio", "datetime2"),
                        input("@fechaHoraFin", "datetime2"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_cerrar_sesion", List.of(
                        input("@idSesion", "uniqueidentifier"),
                        input("@idDocente", "uniqueidentifier"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_registrar_asistencias_sesion", List.of(
                        input("@idSesion", "uniqueidentifier"),
                        input("@asistenciaJSON", "nvarchar"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_registrar_asistencia_estudiante_autonomo", List.of(
                        input("@idEstudiante", "uniqueidentifier"),
                        input("@idSesion", "uniqueidentifier"),
                        input("@codigoVerificacion", "nvarchar"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_radicar_solicitud_revision_asistencia", List.of(
                        input("@idEstudiante", "uniqueidentifier"),
                        input("@idSesion", "uniqueidentifier"),
                        input("@categoria", "nvarchar"),
                        input("@justificacion", "nvarchar"),
                        input("@soporteNombre", "nvarchar"),
                        input("@soporteUrl", "nvarchar"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                )),
                Arguments.of("usp_resolver_solicitud_revision_asistencia", List.of(
                        input("@idSolicitud", "uniqueidentifier"),
                        input("@idDocente", "uniqueidentifier"),
                        input("@accion", "nvarchar"),
                        input("@respuestaDocente", "nvarchar"),
                        input("@idCorrelacion", "uniqueidentifier"),
                        input("@idUsuarioEjecutor", "uniqueidentifier")
                ))
        );
    }

    private static SqlParameterContract input(final String parameterName, final String typeName) {
        return new SqlParameterContract(parameterName, typeName, false);
    }

    private record SqlParameterContract(String parameterName, String typeName, boolean output) {
    }
}
