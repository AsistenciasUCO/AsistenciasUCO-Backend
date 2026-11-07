package co.edu.uco.asistenciasuco.infrastructure.audit;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoFacultadQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.AuditEventJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditActorType;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditEvent;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditOutcome;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifica auditoria HTTP durable contra SQL Server usando solo mocks explicitos para los
 * puertos de negocio que no forman parte del contrato de auditoria.
 */
@Tag("integration")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
class AuditHttpIT {

    private static final String API_CLIENT_ID = "asistencias-api";
    private static final UUID ESTUDIANTE_ID = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID COORDINADOR_ID = UUID.fromString("83641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID DOCENTE_ID = UUID.fromString("73641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID SESION_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID ESTUDIANTE_ACADEMICO_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @LocalServerPort
    private int port;

    @Autowired
    private AuditEventJpaRepository auditEventJpaRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private InstitutionalScopePort institutionalScopePort;

    @MockitoBean
    private AsistenciaRepositoryPort asistenciaRepositoryPort;

    @MockitoBean(extraInterfaces = GrupoFacultadQueryPort.class)
    private GrupoRepositoryPort grupoRepositoryPort;

    @MockitoBean
    private UsuarioRepositoryPort usuarioRepositoryPort;

    private final RestTemplate restTemplate = buildRestTemplate();

    @BeforeEach
    void configureJwtDecoder() {
        when(jwtDecoder.decode("audit-estudiante"))
                .thenReturn(institutionalJwt("audit-estudiante", ESTUDIANTE_ID, InstitutionalRole.ESTUDIANTE,
                        API_CLIENT_ID, API_CLIENT_ID));
        when(jwtDecoder.decode("audit-coordinador"))
                .thenReturn(institutionalJwt("audit-coordinador", COORDINADOR_ID, InstitutionalRole.COORDINADOR,
                        API_CLIENT_ID, API_CLIENT_ID));
        when(jwtDecoder.decode("audit-docente"))
                .thenReturn(institutionalJwt("audit-docente", DOCENTE_ID, InstitutionalRole.DOCENTE,
                        API_CLIENT_ID, API_CLIENT_ID));
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo(anyString())).thenReturn(Optional.empty());
        when(usuarioRepositoryPort.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.empty());
        when(grupoRepositoryPort.registrarEstudianteEnGrupo(any()))
                .thenThrow(new ConflictException(GrupoErrorCode.ERR_GRUPO_NO_HABILITADO));
    }

    @Test
    void operacion_exitosa_persiste_auditoria_success_recuperable_desde_db() {
        when(institutionalScopePort.findEstudianteIdByUsuario(ESTUDIANTE_ID))
                .thenReturn(Optional.of(ESTUDIANTE_ACADEMICO_ID));
        final String correlationId = UUID.randomUUID().toString();
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", correlationId);
        headers.setBearerAuth("audit-estudiante");

        final ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/asistencias/revisiones",
                HttpMethod.POST,
                new HttpEntity<>("""
                        {
                          "sesionId":"00000000-0000-0000-0000-000000000301",
                          "categoria":"INASISTENCIA",
                          "justificacion":"Validar asistencia."
                        }
                        """, headers),
                String.class
        );

        assertEquals(202, response.getStatusCode().value());
        assertEquals(correlationId, response.getHeaders().getFirst("X-Correlation-Id"));

        final AuditEvent event = auditEventJpaRepository.findLatestByCorrelationId(correlationId).orElseThrow();
        assertEquals(AuditOutcome.SUCCESS, event.outcome());
        assertEquals("SOLICITAR_REVISION_ASISTENCIA", event.action());
        assertEquals("SESION", event.resourceType());
        assertEquals(SESION_ID.toString(), event.resourceId());
        assertEquals(correlationId, event.correlationId());
        assertEquals(AuditActorType.USER, event.actorType());
        assertEquals(ESTUDIANTE_ID.toString(), event.actorId());
        assertTrue(event.errorCode() == null || event.errorCode().isBlank());
        assertFalse(event.traceId() == null || event.traceId().isBlank());
        assertEquals("HTTP", event.metadata().get("handlerType"));
    }

    @Test
    void operacion_fallida_persiste_auditoria_failure_con_errorCode() {
        final String correlationId = UUID.randomUUID().toString();
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", correlationId);
        // CULM-001 A.1 (F-COO-004): la matricula manual es solo del DOCENTE titular; el COORDINADOR recibe 403 en el filtro.
        headers.setBearerAuth("audit-docente");

        final String body = """
                {
                  "tipoIdentificacionId":"13641bab-e3cd-485c-b275-47e7b731e18c",
                  "numeroIdentificacion":789456123,
                  "primerApellido":"PEREZ",
                  "segundoApellido":"GOMEZ",
                  "primerNombre":"ANA",
                  "segundoNombre":"MARIA",
                  "correo":"audit.failure.%s@uco.edu.co",
                  "password":"Clave123!"
                }
                """.formatted(UUID.randomUUID().toString().replace("-", "").substring(0, 12));

        final UUID grupoId = UUID.randomUUID();
        final ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/grupos/" + grupoId + "/estudiantes",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        assertEquals(409, response.getStatusCode().value());
        assertEquals(correlationId, response.getHeaders().getFirst("X-Correlation-Id"));
        assertTrue(response.getBody().contains("\"code\":\"ERR_GRUPO_NO_HABILITADO\""));

        final AuditEvent event = auditEventJpaRepository.findLatestByCorrelationId(correlationId).orElseThrow();
        assertEquals(AuditOutcome.FAILURE, event.outcome());
        assertEquals("REGISTRAR_ESTUDIANTE_EN_GRUPO", event.action());
        assertEquals("GRUPO", event.resourceType());
        assertEquals(grupoId.toString(), event.resourceId());
        assertEquals("ERR_GRUPO_NO_HABILITADO", event.errorCode());
        assertEquals(409, event.httpStatus());
        assertEquals(AuditActorType.USER, event.actorType());
        assertEquals(DOCENTE_ID.toString(), event.actorId());
        assertFalse(event.traceId() == null || event.traceId().isBlank());
        assertFalse(event.spanId() == null || event.spanId().isBlank());
        assertEquals("HTTP", event.metadata().get("handlerType"));
    }

    private static Jwt institutionalJwt(
            final String token,
            final UUID idUsuario,
            final InstitutionalRole role,
            final String audience,
            final String apiClientId
    ) {
        final Instant now = Instant.now();
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .issuer("http://127.0.0.1:65534/realms/asistencias-uco")
                .subject("test-" + idUsuario)
                .audience(List.of(audience))
                .issuedAt(now.minusSeconds(60))
                .notBefore(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(300))
                .claim("idUsuario", idUsuario.toString())
                .claim("resource_access", Map.of(apiClientId, Map.of("roles", List.of(role.name()))))
                .build();
    }

    private RestTemplate buildRestTemplate() {
        final RestTemplate template = new RestTemplate();
        template.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(final ClientHttpResponse response) {
                return false;
            }
        });
        return template;
    }
}
