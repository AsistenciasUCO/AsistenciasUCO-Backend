package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.abrirqrtoma.primaryports.AbrirQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.QrTomaResumen;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.primaryports.RegistrarAsistenciaPorTokenInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotartokensasistenciaautomatico.primaryports.RotarTokensAsistenciaAutomaticoInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.abrirtoma.primaryports.AbrirTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.TomaQrCargaFixture;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.AsistenciaQrRateLimitFilter;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CULM-001 B.2 (CONTRACT 3.11, B-9, NB-4): concurrencia del registro por QR sobre SQL Server real. Ronda 1: 5 grupos x 40
 * estudiantes registrando por token a la vez (200 hilos) a traves del filtro de IP real, el caso de uso real y el
 * adaptador JPA, todos desde 127.0.0.1, mientras el job de rotacion corre cada segundo; se repite la ronda (200
 * {@code yaRegistrado}). Ronda 2: 20 grupos x 40 (800 registros) desde la misma IP. Criterio: todos AN, 0 duplicados, 0
 * errores al cliente y 0 respuestas 429. Se registran p50, p95 y maximo por grupo.
 */
@Tag("integration")
@SpringBootTest(properties = "app.asistencia-qr.rotacion.tick=PT1H")
@MockitoBean(types = JwtDecoder.class)
class AsistenciaQrConcurrenciaIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsistenciaQrConcurrenciaIT.class);
    private static final String IP = "127.0.0.1";
    private static final int POR_GRUPO = 40;

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AbrirTomaAsistenciaInputPort abrirToma;
    @Autowired private AbrirQrTomaInputPort abrirQr;
    @Autowired private RegistrarAsistenciaPorTokenInputPort registrar;
    @Autowired private RotarTokensAsistenciaAutomaticoInputPort rotarAutomatico;
    @Autowired private FilterRegistrationBean<AsistenciaQrRateLimitFilter> filtroIp;

    private final List<TomaQrCargaFixture> fixtures = new ArrayList<>();

    @AfterEach
    void limpiar() {
        for (final TomaQrCargaFixture fixture : fixtures) {
            fixture.limpiar();
        }
    }

    private record Resultado(int status, String codigo, long milis, UUID grupo) {
    }

    @Test
    void ronda_1_cinco_grupos_por_40_y_repeticion_idempotente_sin_429() throws Exception {
        final TomaQrCargaFixture fixture = preparar(5);
        final Map<UUID, String> tokens = abrirTomasQr(fixture);

        final List<Resultado> ronda = ejecutarRonda(fixture, tokens, true);
        verificar(fixture, ronda, 201, "ronda 1 (5x40)");

        final List<Resultado> repeticion = ejecutarRonda(fixture, tokens, true);
        verificar(fixture, repeticion, 200, "repeticion ronda 1 (yaRegistrado)");
    }

    @Test
    void ronda_2_veinte_grupos_por_40_desde_la_misma_ip_sin_429() throws Exception {
        final TomaQrCargaFixture fixture = preparar(20);
        final Map<UUID, String> tokens = abrirTomasQr(fixture);

        verificar(fixture, ejecutarRonda(fixture, tokens, false), 201, "ronda 2 (20x40)");
    }

    private TomaQrCargaFixture preparar(final int grupos) {
        final TomaQrCargaFixture fixture = new TomaQrCargaFixture(jdbcTemplate).crear(grupos, POR_GRUPO);
        fixtures.add(fixture);
        return fixture;
    }

    private Map<UUID, String> abrirTomasQr(final TomaQrCargaFixture fixture) {
        final Map<UUID, String> tokens = new ConcurrentHashMap<>();
        CorrelationIdContext.set(UUID.randomUUID());
        try {
            for (final TomaQrCargaFixture.GrupoCarga grupo : fixture.grupos()) {
                final TomaAsistenciaResumen toma = abrirToma.execute(new AbrirTomaAsistenciaInputPort.Comando(grupo.sesion(), "QR",
                        fixture.docenteUsuario()));
                final QrTomaResumen qr = abrirQr.execute(new AbrirQrTomaInputPort.Comando(toma.idToma(), fixture.docenteUsuario()));
                tokens.put(grupo.grupo(), qr.urlAsistencia().substring(qr.urlAsistencia().indexOf("?t=") + 3));
            }
        } finally {
            CorrelationIdContext.clear();
        }
        return tokens;
    }

    private List<Resultado> ejecutarRonda(final TomaQrCargaFixture fixture, final Map<UUID, String> tokens, final boolean conJob)
            throws Exception {
        final int total = fixture.grupos().size() * POR_GRUPO;
        final ExecutorService hilos = Executors.newFixedThreadPool(Math.min(total, 200));
        final CountDownLatch salida = new CountDownLatch(1);
        final AtomicBoolean corriendo = new AtomicBoolean(true);
        final AtomicInteger pasadasJob = new AtomicInteger();
        final Thread job = new Thread(() -> {
            while (corriendo.get()) {
                CorrelationIdContext.set(UUID.randomUUID());
                try {
                    rotarAutomatico.execute();
                    pasadasJob.incrementAndGet();
                    Thread.sleep(1000);
                } catch (final InterruptedException _) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (final RuntimeException exception) {
                    LOGGER.warn("Pasada del job fallida durante la ronda: {}", exception.getClass().getSimpleName());
                } finally {
                    CorrelationIdContext.clear();
                }
            }
        }, "it-rotacion-asistencia");
        if (conJob) {
            job.start();
        }
        final List<Future<Resultado>> futuros = new ArrayList<>(total);
        try {
            for (final TomaQrCargaFixture.GrupoCarga grupo : fixture.grupos()) {
                for (final TomaQrCargaFixture.Estudiante estudiante : grupo.estudiantes()) {
                    futuros.add(hilos.submit(() -> {
                        salida.await();
                        return registrarPorHttp(tokens.get(grupo.grupo()), estudiante.usuario(), grupo.grupo());
                    }));
                }
            }
            salida.countDown();
            final List<Resultado> resultados = new ArrayList<>(total);
            for (final Future<Resultado> futuro : futuros) {
                resultados.add(futuro.get(5, TimeUnit.MINUTES));
            }
            return resultados;
        } finally {
            corriendo.set(false);
            job.interrupt();
            job.join(5000);
            hilos.shutdownNow();
            if (conJob) {
                LOGGER.info("Pasadas del job de rotacion durante la ronda: {}", pasadasJob.get());
            }
        }
    }

    /** Pasa por el filtro de IP real (solo cuenta rechazos) y por el caso de uso real; el status sigue la regla del controller. */
    private Resultado registrarPorHttp(final String token, final UUID usuario, final UUID grupo) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", AsistenciaQrRateLimitFilter.RUTA);
        request.setRemoteAddr(IP);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final String[] codigo = {null};
        final long inicio = System.nanoTime();
        final FilterChain cadena = (req, res) -> {
            CorrelationIdContext.set(UUID.randomUUID());
            try {
                final RegistrarAsistenciaPorTokenInputPort.Resultado resultado = registrar.execute(
                        new RegistrarAsistenciaPorTokenInputPort.Comando(token, null, IP, usuario));
                ((MockHttpServletResponse) res).setStatus(resultado.yaRegistrado() ? 200 : 201);
            } catch (final ApplicationException exception) {
                codigo[0] = exception.getCode();
                ((MockHttpServletResponse) res).setStatus("AQR_011".equals(exception.getCode()) ? 429 : 409);
            } catch (final RuntimeException exception) {
                codigo[0] = exception.getClass().getSimpleName();
                ((MockHttpServletResponse) res).setStatus(500);
            } finally {
                CorrelationIdContext.clear();
            }
        };
        filtroIp.getFilter().doFilter(request, response, cadena);
        return new Resultado(response.getStatus(), codigo[0], (System.nanoTime() - inicio) / 1_000_000, grupo);
    }

    private void verificar(final TomaQrCargaFixture fixture, final List<Resultado> resultados, final int estadoEsperado, final String ronda) {
        final List<Resultado> errores = resultados.stream().filter(r -> r.status() != estadoEsperado).toList();
        for (final TomaQrCargaFixture.GrupoCarga grupo : fixture.grupos()) {
            final List<Long> tiempos = new ArrayList<>(resultados.stream().filter(r -> r.grupo().equals(grupo.grupo())).map(Resultado::milis).toList());
            Collections.sort(tiempos);
            LOGGER.info("{} grupo {}: p50={} ms p95={} ms max={} ms", ronda, grupo.grupo(), percentil(tiempos, 50), percentil(tiempos, 95),
                    tiempos.getLast());
            assertEquals(POR_GRUPO, fixture.detallesPresentes(grupo.sesion()), ronda + ": detalles AN del grupo " + grupo.grupo());
            assertEquals(0, fixture.duplicados(grupo.sesion()), ronda + ": duplicados del grupo " + grupo.grupo());
        }
        assertEquals(0, resultados.stream().filter(r -> r.status() == 429).count(), ronda + ": respuestas 429");
        assertTrue(errores.isEmpty(), ronda + ": " + errores.size() + " errores al cliente, p. ej. " + errores.stream().limit(3).toList());
    }

    private static long percentil(final List<Long> ordenados, final int percentil) {
        final int indice = (int) Math.ceil(percentil / 100.0 * ordenados.size()) - 1;
        return ordenados.get(Math.max(0, Math.min(indice, ordenados.size() - 1)));
    }
}
