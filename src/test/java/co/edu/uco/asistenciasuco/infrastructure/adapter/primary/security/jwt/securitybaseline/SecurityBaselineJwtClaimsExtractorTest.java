package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.securitybaseline;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityBaselineJwtClaimsExtractorTest {

    private static final String API_CLIENT_ID = "security-baseline-bff";
    private static final String USER_ID_CLAIM = "sub";
    private static final String VALID_USER_ID = "4fc64417-05ef-42f6-a458-a2ac168b2a29";

    private final SecurityBaselineJwtClaimsExtractor extractor =
            new SecurityBaselineJwtClaimsExtractor(API_CLIENT_ID, USER_ID_CLAIM);

    @Test
    void sub_valido_se_retorna_como_uuid() {
        final Jwt jwt = jwtWithClaims(Map.of(USER_ID_CLAIM, VALID_USER_ID));
        assertEquals(VALID_USER_ID, extractor.requireUserId(jwt).toString());
    }

    @Test
    void sub_ausente_lanza_excepcion() {
        final Jwt jwt = jwtWithClaims(Map.of("otroClaim", "otroValor"));
        assertThrows(OAuth2AuthenticationException.class, () -> extractor.requireUserId(jwt));
    }

    @Test
    void sub_no_uuid_lanza_excepcion() {
        final Jwt jwt = jwtWithClaims(Map.of(USER_ID_CLAIM, "not-a-uuid"));
        assertThrows(OAuth2AuthenticationException.class, () -> extractor.requireUserId(jwt));
    }

    @Test
    void resource_access_roles_extrae_roles_institucionales() {
        final Jwt jwt = jwtWithClaims(Map.of(
                USER_ID_CLAIM, VALID_USER_ID,
                "resource_access", Map.of(API_CLIENT_ID, Map.of("roles", List.of("DOCENTE", "ADMINISTRADOR")))
        ));
        assertEquals(Set.of(InstitutionalRole.DOCENTE, InstitutionalRole.ADMINISTRADOR), extractor.extractRoles(jwt));
    }

    @Test
    void direct_roles_extrae_roles_institucionales() {
        final Jwt jwt = jwtWithClaims(Map.of(
                USER_ID_CLAIM, VALID_USER_ID,
                "roles", List.of("ESTUDIANTE")
        ));
        assertEquals(Set.of(InstitutionalRole.ESTUDIANTE), extractor.extractRoles(jwt));
    }

    @Test
    void realm_access_extrae_roles_si_otros_no_estan() {
        final Jwt jwt = jwtWithClaims(Map.of(
                USER_ID_CLAIM, VALID_USER_ID,
                "realm_access", Map.of("roles", List.of("COORDINADOR"))
        ));
        assertEquals(Set.of(InstitutionalRole.COORDINADOR), extractor.extractRoles(jwt));
    }

    @Test
    void roles_desconocidos_se_ignoran() {
        final Jwt jwt = jwtWithClaims(Map.of(
                USER_ID_CLAIM, VALID_USER_ID,
                "roles", List.of("UNKNOWN_ROLE", "DOCENTE")
        ));
        assertEquals(Set.of(InstitutionalRole.DOCENTE), extractor.extractRoles(jwt));
    }

    private static Jwt jwtWithClaims(final Map<String, Object> claims) {
        return new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                claims
        );
    }
}
