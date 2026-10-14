package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.securitybaseline;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Extractor de claims para tokens emitidos por la suite Security Baseline (Keycloak + PDP).
 * Extrae el subject/userId como UUID y mapea roles institucionales reconocidos.
 */
public final class SecurityBaselineJwtClaimsExtractor implements JwtClaimsExtractor {

    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String REALM_ACCESS_CLAIM = "realm_access";

    private static final Set<String> VALID_INSTITUTIONAL_ROLE_NAMES = Arrays.stream(InstitutionalRole.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    private final String apiClientId;
    private final String userIdClaim;

    public SecurityBaselineJwtClaimsExtractor(final String apiClientId, final String userIdClaim) {
        this.apiClientId = Objects.requireNonNull(apiClientId, "apiClientId es obligatorio.");
        this.userIdClaim = Objects.requireNonNull(userIdClaim, "userIdClaim es obligatorio.");
    }

    @Override
    public UUID requireUserId(final Jwt jwt) {
        Object rawValue = jwt.getClaim(userIdClaim);
        if (rawValue == null && !"sub".equalsIgnoreCase(userIdClaim)) {
            rawValue = jwt.getSubject();
        }
        if (!(rawValue instanceof String value) || value.isBlank()) {
            throw invalidToken("El token no contiene un claim '" + userIdClaim + "' o subject valido.");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw invalidToken("El claim '" + userIdClaim + "' no tiene formato UUID valido.");
        }
    }

    @Override
    public Set<InstitutionalRole> extractRoles(final Jwt jwt) {
        // 1. resource_access[apiClientId].roles
        final Object resourceAccessRaw = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (resourceAccessRaw instanceof Map<?, ?> resourceAccess) {
            final Object clientEntryRaw = resourceAccess.get(apiClientId);
            if (clientEntryRaw instanceof Map<?, ?> clientEntry) {
                final Object rolesRaw = clientEntry.get(ROLES_CLAIM);
                if (rolesRaw instanceof Collection<?> roles) {
                    final Set<InstitutionalRole> extracted = parseRoles(roles);
                    if (!extracted.isEmpty()) {
                        return extracted;
                    }
                }
            }
        }

        // 2. roles directo
        final Object directRolesRaw = jwt.getClaim(ROLES_CLAIM);
        if (directRolesRaw instanceof Collection<?> directRoles) {
            final Set<InstitutionalRole> extracted = parseRoles(directRoles);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }

        // 3. realm_access.roles
        final Object realmAccessRaw = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccessRaw instanceof Map<?, ?> realmAccess) {
            final Object rolesRaw = realmAccess.get(ROLES_CLAIM);
            if (rolesRaw instanceof Collection<?> roles) {
                return parseRoles(roles);
            }
        }

        return Set.of();
    }

    private static Set<InstitutionalRole> parseRoles(final Collection<?> roles) {
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(SecurityBaselineJwtClaimsExtractor::toInstitutionalRole)
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Optional<InstitutionalRole> toInstitutionalRole(final String rawRole) {
        if (rawRole == null || !VALID_INSTITUTIONAL_ROLE_NAMES.contains(rawRole)) {
            return Optional.empty();
        }
        return Optional.of(InstitutionalRole.valueOf(rawRole));
    }

    private static OAuth2AuthenticationException invalidToken(final String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null));
    }
}
