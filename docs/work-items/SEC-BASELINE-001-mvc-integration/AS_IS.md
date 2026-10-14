# AS-IS comprobado

## Backend

- Aplicación Spring Boot servlet/Spring MVC; `src/main/resources/application.yml` fija `spring.main.web-application-type: servlet`.
- `pom.xml` usa `spring-boot-starter-webmvc`, `spring-boot-starter-security` y resource server OAuth2. No se agrega WebFlux.
- `SecurityConfig` mantiene CORS sin credenciales de navegador, sesión stateless, JWT Bearer, reglas de rol y el filtro de Azure antes del Bearer filter.
- `KeycloakSecurityAdapterConfiguration` valida issuer, timestamp, audience y el claim UUID configurable `userIdClaim`, cuyo valor local actual es `idUsuario`.
- `InstitutionalJwtAuthenticationConverter` toma el principal institucional del claim `idUsuario`; no usa `sub` como sustituto.
- `CorrelationIdFilter` establece o sanea `X-Correlation-Id` y el contexto de correlación.
- Los errores de autenticación/autorización existentes usan el envelope `ApiErrorResponse` con códigos `UNAUTHORIZED` y `FORBIDDEN`.

## Security Baseline v0.1.6

- Tag verificado: `v0.1.6` → `272c8e3168aeee6934194e2278b3537dc3c5dd78`.
- El starter oficial inspeccionado está basado en WebFlux y describe el enforcement como filtro web reactivo.
- El endpoint implementado por el PEP embebido es `POST /internal/v1/embedded-access-decisions`.
- El request raw del PEP contiene `applicationName`, `environment`, `path` y `method`; la credencial de aplicación viaja por `X-Application-Credential`.
- La respuesta operacional del PEP usa `2xx` para permitir, `401` para token no válido, `403` para denegación y otros estados/fallos para indisponibilidad.
- El starter oficial no exige que este backend implemente el endpoint interno PEP→PDP; hacerlo aquí sería mezclar responsabilidades.

## Discrepancias y evidencia faltante

- El README del starter describe una protección embebida; otra guía del release describe PEP externo y registro. Se toma el código/endpoint como AS-IS operativo y se conserva la discrepancia para decisión posterior.
- El release clonado no contiene `Manual-integracion-aplicaciones.pdf` ni `Manual-funcional-plataforma-seguridad.pdf`; fueron buscados en los archivos versionados y en los assets publicados del release. Estado: `BLOCKED_BY_MISSING_EVIDENCE` para validar esas reglas documentales.
- El compose oficial contiene una credencial literal. Se reporta como `SECURITY_FINDING` por archivo/tipo; la copia del laboratorio usa placeholders y no copia el valor.
