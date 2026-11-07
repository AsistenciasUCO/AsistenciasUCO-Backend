package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.ResumenAsistenciaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarresumenasistencia.primaryports.ConsultarResumenAsistenciaGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.abrirqrtoma.primaryports.AbrirQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.cerrarqrtoma.primaryports.CerrarQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.QrTomaResumen;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.primaryports.RegistrarAsistenciaPorTokenInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotarqrtoma.primaryports.RotarQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.abrirtoma.primaryports.AbrirTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartoma.primaryports.CerrarTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.FaltanteTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarfaltantestoma.primaryports.ConsultarFaltantesTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultartomassesion.primaryports.ConsultarTomasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.registrarasistenciastoma.primaryports.RegistrarAsistenciasTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.ConsultarMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.TomaQrCargaFixture;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CULM-001 B.2 (CONTRACT 3.11, contrato JPA/SP con SQL Server real): flujo completo por los puertos de entrada reales
 * (casos de uso + adaptadores JPA + SP): abrir toma MANUAL, lote por toma, abrir toma QR, QR (abrir, rotar, cerrar),
 * registro por PIN y por token (idempotente), faltantes, cierre con ausentes, resumen por horas del docente y del
 * estudiante y consolidado por sesion de GET /grupos/{id}/asistencias con dos tomas.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class TomaAsistenciaQrSqlServerIT {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AbrirTomaAsistenciaInputPort abrirToma;
    @Autowired private ConsultarTomasSesionInputPort consultarTomas;
    @Autowired private RegistrarAsistenciasTomaInputPort registrarLote;
    @Autowired private CerrarTomaAsistenciaInputPort cerrarToma;
    @Autowired private ConsultarFaltantesTomaInputPort faltantes;
    @Autowired private AbrirQrTomaInputPort abrirQr;
    @Autowired private RotarQrTomaInputPort rotarQr;
    @Autowired private CerrarQrTomaInputPort cerrarQr;
    @Autowired private RegistrarAsistenciaPorTokenInputPort registrarPorToken;
    @Autowired private ConsultarResumenAsistenciaGrupoInputPort resumenGrupo;
    @Autowired private ConsultarMateriasEstudianteInputPort materias;
    @Autowired private AsistenciaRepositoryPort asistencias;

    private TomaQrCargaFixture fixture;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new TomaQrCargaFixture(jdbcTemplate).crear(1, 3);
    }

    @AfterEach
    void limpiar() {
        try {
            fixture.limpiar();
        } finally {
            CorrelationIdContext.clear();
        }
    }

    @Test
    void flujo_completo_de_tomas_qr_faltantes_y_resumen_por_horas() {
        final TomaQrCargaFixture.GrupoCarga grupo = fixture.grupos().getFirst();
        final UUID docente = fixture.docenteUsuario();
        final TomaQrCargaFixture.Estudiante e1 = grupo.estudiantes().get(0);
        final TomaQrCargaFixture.Estudiante e2 = grupo.estudiantes().get(1);
        final TomaQrCargaFixture.Estudiante e3 = grupo.estudiantes().get(2);

        // Toma 1 MANUAL con el lote por toma.
        final TomaAsistenciaResumen manual = abrirToma.execute(new AbrirTomaAsistenciaInputPort.Comando(grupo.sesion(), "MANUAL", docente));
        assertEquals(1, manual.numero());
        assertEquals("MANUAL", manual.modo());
        assertTrue(manual.estaAbierta());
        assertEquals("TOM_003", assertThrows(ConflictException.class,
                () -> abrirToma.execute(new AbrirTomaAsistenciaInputPort.Comando(grupo.sesion(), "QR", docente))).getCode());
        registrarLote.execute(new RegistrarAsistenciasTomaInputPort.Comando(manual.idToma(), List.of(
                new RegistrarAsistenciasTomaInputPort.Registro(e1.estudiante(), "AN"),
                new RegistrarAsistenciasTomaInputPort.Registro(e2.estudiante(), "AN"),
                new RegistrarAsistenciasTomaInputPort.Registro(e3.estudiante(), "SJC")), docente));
        final CerrarTomaAsistenciaInputPort.Resultado cierreManual = cerrarToma.execute(
                new CerrarTomaAsistenciaInputPort.Comando(manual.idToma(), null, docente));
        assertFalse(cierreManual.yaEstabaCerrada());
        assertFalse(cierreManual.toma().estaAbierta());
        assertTrue(cerrarToma.execute(new CerrarTomaAsistenciaInputPort.Comando(manual.idToma(), null, docente)).yaEstabaCerrada());

        // Toma 2 QR: abrir, rotar, registrar por PIN y por token (idempotente), cerrar marcando ausentes.
        final TomaAsistenciaResumen tomaQr = abrirToma.execute(new AbrirTomaAsistenciaInputPort.Comando(grupo.sesion(), "QR", docente));
        assertEquals(2, tomaQr.numero());
        final QrTomaResumen qr = abrirQr.execute(new AbrirQrTomaInputPort.Comando(tomaQr.idToma(), docente));
        assertEquals(6, qr.pin().length());
        assertTrue(qr.urlAsistencia().contains("/app/asistencia/qr?t="));
        final QrTomaResumen rotado = rotarQr.execute(new RotarQrTomaInputPort.Comando(tomaQr.idToma(), docente));
        final String token = rotado.urlAsistencia().substring(rotado.urlAsistencia().indexOf("?t=") + 3);

        final RegistrarAsistenciaPorTokenInputPort.Resultado porPin = registrarPorToken.execute(
                new RegistrarAsistenciaPorTokenInputPort.Comando(null, rotado.pin(), "127.0.0.1", e1.usuario()));
        assertFalse(porPin.yaRegistrado());
        assertEquals(2, porPin.numeroToma());
        assertNotNull(porPin.mensaje());
        assertTrue(registrarPorToken.execute(
                new RegistrarAsistenciaPorTokenInputPort.Comando(token, null, "127.0.0.1", e1.usuario())).yaRegistrado());
        assertEquals("AQR_001", assertThrows(ResourceNotFoundException.class, () -> registrarPorToken.execute(
                new RegistrarAsistenciaPorTokenInputPort.Comando(null, "000000".equals(rotado.pin()) ? "111111" : "000000", "127.0.0.1",
                        e2.usuario()))).getCode());

        final PaginaResultado<FaltanteTomaFila> antes = faltantes.execute(new ConsultarFaltantesTomaInputPort.Consulta(tomaQr.idToma(), 1, 50, docente));
        assertEquals(2, antes.totalElementos());

        cerrarQr.execute(new CerrarQrTomaInputPort.Comando(tomaQr.idToma(), docente));
        assertEquals("AQR_001", assertThrows(ApplicationException.class, () -> registrarPorToken.execute(
                new RegistrarAsistenciaPorTokenInputPort.Comando(token, null, "127.0.0.1", e2.usuario()))).getCode());

        final CerrarTomaAsistenciaInputPort.Resultado cierreQr = cerrarToma.execute(
                new CerrarTomaAsistenciaInputPort.Comando(tomaQr.idToma(), null, docente));
        assertEquals(2, cierreQr.totalAusentesMarcados());
        assertEquals(3, cierreQr.toma().totalRegistros());

        final ConsultarTomasSesionInputPort.Resultado tomas = consultarTomas.execute(new ConsultarTomasSesionInputPort.Consulta(grupo.sesion(), docente));
        assertEquals(2, tomas.tomas().tomas().size());
        assertTrue(tomas.tomas().ventanaAbierta());
        assertEquals(4, tomas.tomas().maximoTomas());

        // GET /grupos/{id}/asistencias: una fila por estudiante y sesion con el estado consolidado (e1: AN+AN, e2: AN+SJC, e3: SJC+SJC).
        final List<AsistenciaRepositoryProjection> filas = asistencias.consultarAsistenciasPorGrupo(
                new ConsultarAsistenciasPorGrupoRepositoryDTO(grupo.grupo(), null, docente));
        assertEquals(3, filas.size());
        assertEquals("AN", estadoDe(filas, e1.estudiante()));
        assertEquals("SJC", estadoDe(filas, e2.estudiante()));
        assertEquals("SJC", estadoDe(filas, e3.estudiante()));

        // Resumenes por horas: la sesion aun no vencio, el dictado lo decide la vista (DEC-024); la forma es completa.
        final PaginaResultado<ResumenAsistenciaEstudianteDTO> sabana = resumenGrupo.execute(
                new ConsultarResumenAsistenciaGrupoInputPort.Consulta(grupo.grupo(), null, null, docente));
        assertEquals(3, sabana.totalElementos());
        assertNotNull(sabana.elementos().getFirst().indicadores().horasDictadas());
        final List<MateriaEstudianteDTO> materiasE1 = materias.execute(e1.usuario());
        assertEquals(1, materiasE1.size());
        assertNotNull(materiasE1.getFirst().indicadores());
    }

    private static String estadoDe(final List<AsistenciaRepositoryProjection> filas, final UUID estudiante) {
        return filas.stream().filter(f -> f.getEstudiante().equals(estudiante)).findFirst().orElseThrow().getEstado();
    }
}
