---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-26
---

# Arquitectura de Runtime Security (independiente del proveedor)

## 1. Alcance

Este documento describe la seguridad **runtime** de AsistenciasUCO:

- validación de JWT;
- resolución del usuario institucional;
- extracción de roles;
- autorización HTTP por rol;
- autorización contextual pendiente o implementada a nivel Application;
- CSRF/CORS;
- implicaciones de seguridad del canal realtime/SSE.

No describe el provisioning administrativo de cuentas en Keycloak. Ese flujo usa
`IdentityProviderPort` y está documentado en:

```text
docs/security/keycloak-identity-provider.md
```

Runtime Security e Identity Provisioning son capabilities distintas aunque hoy compartan
Keycloak.

---

## 2. Modelo de autenticación

La API es un OAuth2 Resource Server.

Flujo:

```text
HTTP Request
Authorization: Bearer <jwt>
        |
        v
BearerTokenAuthenticationFilter
        |
        v
JwtDecoder
  - firma
  - issuer
  - audience
  - claim institucional requerido
        |
        v
InstitutionalJwtAuthenticationConverter
        |
        v
JwtClaimsExtractor
        |
        v
KeycloakJwtClaimsExtractor
        |
        v
JwtAuthenticationToken
principal = idUsuario UUID
authorities = ROLE_*
        |
        v
SecurityContextHolder
```

La aplicación no autentica usuarios mediante sesión de servidor.

Configuración:

```text
SessionCreationPolicy.STATELESS
```

---

## 3. Separación de responsabilidades

| Capability | Contrato | Provider actual | Selector |
|---|---|---|---|
| Runtime authentication | `JwtClaimsExtractor`, `JwtDecoder` | Keycloak | `app.adapters.security.provider=keycloak` |
| Identity provisioning | `IdentityProviderPort` | Keycloak Admin API | `app.adapters.identity.provider=keycloak` |

`SecurityConfig` es neutral respecto a Keycloak. Depende de:

- `JwtDecoder`;
- `InstitutionalJwtAuthenticationConverter`;
- `AuthenticationEntryPoint`;
- `AccessDeniedHandler`;
- configuración CORS.

La interpretación específica de claims Keycloak está aislada en `KeycloakJwtClaimsExtractor`.

---

## 4. Selección de provider

Configuración:

```yaml
app:
  adapters:
    security:
      provider: ${APP_ADAPTERS_SECURITY_PROVIDER:keycloak}

  providers:
    keycloak-security:
      issuer-uri: ${KEYCLOAK_ISSUER_URI:http://127.0.0.1:8081/realms/asistencias-uco}
      api-client-id: ${KEYCLOAK_API_CLIENT_ID:asistencias-api}
      expected-audience: ${KEYCLOAK_EXPECTED_AUDIENCE:asistencias-api}
      user-id-claim: ${KEYCLOAK_USER_ID_CLAIM:idUsuario}
```

Composition Root:

```text
infrastructure.config.adapters.security.keycloak
└── KeycloakSecurityAdapterConfiguration
```

El provider se selecciona con `@ConditionalOnProperty`; nunca con `@Profile`, service locator o
un `if` dentro de Application.

---

## 5. Validación JWT

El `JwtDecoder` productivo de Keycloak aplica:

```text
issuer exacto
+ audience esperada
+ claim institucional UUID requerido
```

Validadores:

```text
JwtValidators.createDefaultWithIssuer(...)
AudienceValidator
RequiredUuidClaimValidator
```

### 5.1 Issuer

El claim `iss` debe coincidir exactamente con `KEYCLOAK_ISSUER_URI`.

No se usa:

- `contains`;
- `endsWith`;
- regex parcial;
- fallback a otro issuer.

### 5.2 Audience

`aud` debe contener la audiencia esperada:

```text
asistencias-api
```

Una audiencia como `account` o el client de frontend no sustituye la audiencia de la API.

### 5.3 Usuario institucional

El claim configurado, por defecto:

```text
idUsuario
```

debe existir y ser UUID válido.

No existe fallback a:

```text
sub
email
preferred_username
```

---

## 6. Roles

`KeycloakJwtClaimsExtractor` lee exclusivamente:

```text
resource_access[asistencias-api].roles
```

No utiliza `realm_access.roles` para autorización de la API.

Roles institucionales reconocidos:

```text
ADMINISTRADOR
DECANO
COORDINADOR
DOCENTE
ESTUDIANTE
```

La comparación es exacta. Roles técnicos o desconocidos se ignoran.

`InstitutionalJwtAuthenticationConverter` transforma:

```text
DOCENTE -> ROLE_DOCENTE
```

La convención `ROLE_` pertenece a Spring Security / Infrastructure; no a Domain ni Application.

---

## 7. Principal autenticado

El principal runtime es siempre:

```text
idUsuario.toString()
```

`AuthenticatedUserResolver` resuelve:

```text
SecurityContextHolder
 -> Authentication.getName()
 -> UUID.fromString(...)
```

No vuelve a interpretar claims del JWT.

Esto evita que diferentes capas discutan si el principal es `sub`, email, username o UUID
institucional.

---

## 8. CSRF

### 8.1 Modelo

`/api/v1/**` es una API REST **stateless** (`SessionCreationPolicy.STATELESS`). La API de negocio
se autentica exclusivamente con una credencial explícita:

```http
Authorization: Bearer <jwt>
```

El webhook `/api/v1/internal/azure-events` se autentica con el header `aeg-sas-token`, validado
por `AzureEventGridAuthFilter` (fail-closed). Su `permitAll` en `authorizeHttpRequests` solo
significa que no exige JWT de negocio; no es un endpoint sin autenticación.

Ninguna ruta `/api/v1/**` autentica usuarios mediante cookies ni `HttpSession`. Un navegador no
adjunta automáticamente un bearer token elegido por la aplicación a un request cross-site, a
diferencia de credenciales basadas en cookies; por tanto no existe una credencial ambiente que
un sitio ajeno pueda hacer valer y el token CSRF no protege nada en este modelo.

`SecurityConfig` aplica una política explícita, no dependiente del contenido del request:

```java
.csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/**"))
```

Consecuencias:

```text
request inseguro a /api/v1/** sin credencial válida
  -> llega al mecanismo de autenticación -> 401 (no 403 de CSRF)

request con Authorization: Bearer válido y rol insuficiente
  -> 403 de autorización

cookie (p. ej. JSESSIONID) sin Bearer
  -> no autentica -> 401
```

Fuera de `/api/v1/**` la protección CSRF de Spring Security permanece activa.

### 8.2 Invariantes que sustentan la exclusión

- API stateless y Bearer explícito (`SessionCreationPolicy.STATELESS`);
- el cliente Angular no envía peticiones con credenciales cross-origin (`withCredentials` no se
  usa; los `fetch` directos al backend usan `credentials: 'omit'`);
- realtime/SSE usa `credentials: 'omit'`;
- CORS del backend no permite credenciales de navegador (`allowCredentials=false`);
- el webhook usa un header secreto explícito validado por un filtro fail-closed.

### 8.3 Validación

La suite de seguridad (`RbacSecurityFilterChainTest`, `AzureWebhookSecurityChainTest`,
`SecurityConfigTest`) cubre, entre otros:

- POST/PUT/PATCH/DELETE protegido sin Bearer -> 401 (no 403 de CSRF);
- POST/PUT/PATCH/DELETE con Bearer válido -> no requiere token CSRF y no crea `HttpSession`;
- rol incorrecto -> 403;
- cookie sin Bearer -> no concede acceso;
- Bearer inválido, vacío o malformado -> 401;
- webhook sin `aeg-sas-token`, con token inválido o sin token configurado -> 401; con token
  válido atraviesa el filtro;
- CORS: origen autorizado permitido, `Authorization` permitido, sin
  `Access-Control-Allow-Credentials`.

### 8.4 Sonar java:S4502 (Security Hotspot)

`java:S4502` («Make sure disabling Spring Security's CSRF protection is safe here») es un Security
Hotspot y se revisa en SonarCloud como **Reviewed / Safe**; no se oculta con `NOSONAR`,
`@SuppressWarnings`, exclusiones ni cambios al quality profile.

Justificación:

> This backend exposes a stateless REST API. Business endpoints authenticate exclusively through
> an explicit Authorization Bearer JWT and do not use cookie- or session-based authentication.
> Spring Security is configured with SessionCreationPolicy.STATELESS. The Angular API client does
> not send credentialed cross-origin requests, realtime explicitly uses credentials=omit, and
> backend CORS does not allow browser credentials. The Azure Event Grid webhook uses an explicit
> aeg-sas-token header validated by a fail-closed authentication filter. Therefore no
> browser-managed authentication credential is automatically attached to unsafe API requests, so
> the API CSRF exclusion is safe for the current architecture. This decision must be revisited
> before introducing cookie/BFF/session authentication.

### 8.5 Cuándo reabrir esta decisión

Si en el futuro la API adopta autenticación mediante cookies, BFF con cookie `HttpOnly` o sesión
de navegador, esta exclusión **debe revisarse** y CSRF **deberá volver a habilitarse** para ese
flujo. Migrar a PKCE/BFF es deuda futura documentada y queda fuera de este cambio.

---

## 9. CORS

`SecurityConfig` mantiene CORS neutral respecto al proveedor de identidad.

Origen local por defecto:

```text
http://localhost:4200
```

Headers relevantes:

```text
Authorization
Content-Type
X-Correlation-Id
Accept
```

Header expuesto:

```text
X-Correlation-Id
```

`allowCredentials=false`: la API es Bearer-only y `Authorization` es un header explícito que no
requiere credenciales de navegador. Nunca debe combinarse `allowedOrigins("*")` con credenciales.

Si se cambian los clientes frontend permitidos, debe hacerse por configuración de entorno y no
hardcodeando nuevos origins en lógica de negocio.

---

## 10. Capas de autorización

La autorización se divide deliberadamente:

### Layer 1 — RBAC HTTP

Spring Security responde:

> ¿Este rol puede llegar a esta familia de rutas/método HTTP?

Ejemplo:

```text
POST /api/v1/grupos/** -> COORDINADOR o ADMINISTRADOR
```

### Layer 2 — scope contextual

Application responde:

> ¿Este usuario concreto puede operar sobre este grupo/sesión/estudiante concreto?

Se implementa mediante casos de uso y `InstitutionalScopePort` cuando el flujo lo requiere.

`SecurityConfig` no debe consultar DB ni resolver ownership contextual.

---

## 11. Matriz RBAC relevante

Estado vigente de las familias revisadas:

| Ruta | Regla |
|---|---|
| `/api/v1/admin/**` | `ADMINISTRADOR` |
| `/api/v1/decano/**` | `DECANO` o `ADMINISTRADOR` |
| `/api/v1/coordinador/**` | `COORDINADOR` o `ADMINISTRADOR` |
| `/api/v1/docente/**` | `DOCENTE` |
| `/api/v1/docentes/**` | `COORDINADOR` o `ADMINISTRADOR` |
| `/api/v1/estudiante/**` | `ESTUDIANTE` |
| `/api/v1/estudiantes/**` | `DOCENTE`, `COORDINADOR` o `ADMINISTRADOR` |
| `GET /api/v1/grupos/**` | `DOCENTE`, `COORDINADOR` o `ADMINISTRADOR` |
| `POST/PUT/DELETE /api/v1/grupos/**` | `COORDINADOR` o `ADMINISTRADOR` |
| `POST/PUT/PATCH /api/v1/sesiones/**` | `DOCENTE` |
| `GET /api/v1/sesiones/*/qr-token` | `DOCENTE` |
| `POST /api/v1/asistencias` | `DOCENTE` |
| `POST /api/v1/asistencias/lote` | `DOCENTE` |
| `POST /api/v1/asistencias/revisiones` | `ESTUDIANTE` |
| `POST /api/v1/asistencias/consultas/grupo` | `DOCENTE`, `COORDINADOR` o `ADMINISTRADOR` |
| `POST /api/v1/usuarios` | cualquier usuario autenticado; deuda funcional pendiente |
| `/api/v1/archivos/**` | cualquier usuario autenticado; deuda Storage/ownership pendiente |
| `GET /api/v1/realtime/stream` | cualquier usuario autenticado |
| `GET /api/v1/realtime/status` | cualquier usuario autenticado |
| `POST /api/v1/realtime/emit` | `ADMINISTRADOR` |

---

## 12. Realtime / SSE

La Fase 3.1 ya reemplazó el antiguo `RealtimeEventHub` por:

```text
RealtimePublisherPort
 -> ReactorRealtimeAdapter
 -> RealtimeStreamGateway
 -> RealtimeEventsController
 -> SSE
```

Provider:

```text
app.adapters.realtime.provider=local-sse
```

### 12.1 Autenticación

Además de autenticación HTTP, `/stream` exige `grupoId` y `LocalSseRealtimeStreamGateway` verifica titularidad docente al suscribirse; no basta un JWT válido.


El stream continúa protegido por Bearer. No se permite:

```text
?token=<jwt>
```

y no se hace público para facilitar el consumo desde navegador.

El frontend deberá consumir SSE con una solución que permita enviar headers, como `fetch`
streaming o un cliente SSE compatible con `Authorization`.

### 12.2 `/emit`

`POST /api/v1/realtime/emit` es una utilidad diagnóstica, no un caso de uso de negocio.

Permanece restringida a:

```text
ADMINISTRADOR
```

Antes de producción debe decidirse si:

- se conserva;
- se condiciona mediante property de diagnóstico;
- o se elimina.

### 12.3 Información sensible

Los payloads realtime no deben transportar:

- JWT;
- refresh tokens;
- cookies;
- contraseñas;
- secretos.

Más detalle:

```text
docs/architecture/reactive-realtime.md
```

---

## 13. Autorización contextual y seguimiento

`GET /api/v1/sesiones/grupo/{grupoId}` ya verifica titularidad en `ConsultarSesionesPorGrupoUseCaseImpl`; no se mantiene como falta de implementación. El seguimiento de validación está en [TD-008](../baseline/TECHNICAL_DEBT.md#td-008).


La lista única de endpoints pendientes y condición de cierre vive en [TD-016](../baseline/TECHNICAL_DEBT.md#td-016). No resolver ownership contextual en el Controller ni mezclarlo con Layer 1.

## 14. Storage security

La regla AS-IS de `/api/v1/archivos/**` es `authenticated()`. La abstracción y autorización funcional pendiente se registran en [TD-004](../baseline/TECHNICAL_DEBT.md#td-004); un matcher de roles no resuelve por sí solo ownership.

## 15. Provisioning security

`POST /api/v1/usuarios` permanece bajo `authenticated()`. Política funcional/roles pendientes: [TD-013](../baseline/TECHNICAL_DEBT.md#td-013). Contrato y semántica DB-first: [keycloak-identity-provider](keycloak-identity-provider.md).

## 16. Cómo agregar otro runtime IdP

Para incorporar otro proveedor, por ejemplo Auth0:

1. crear `Auth0JwtClaimsExtractor implements JwtClaimsExtractor`;
2. crear sus properties tipadas;
3. crear `Auth0SecurityAdapterConfiguration`;
4. condicionarla a `app.adapters.security.provider=auth0`;
5. registrar su `JwtDecoder`;
6. agregar el enum/provider correspondiente;
7. cubrir issuer/audience/user claim/roles con tests.

No deben cambiar:

```text
SecurityConfig
InstitutionalJwtAuthenticationConverter
AuthenticatedUserResolver
Application UseCases
Controllers de negocio
```

salvo que el nuevo contrato funcional exija una capacidad distinta, no por el simple cambio de
vendor.

---

## 17. Reglas de calidad y seguridad

Los objetivos Sonar mencionados aquí requieren evidencia/configuración remota; no son umbrales comprobables en pom.xml. Los gates efectivos locales/CI se distinguen en [TESTING_STANDARD](../testing/TESTING_STANDARD.md).


Cualquier cambio en seguridad debe conservar:

```text
CodeQL: sin nuevas alertas
Sonar Security Rating: A
Sonar Security Hotspots nuevos: 0 o revisados explícitamente
Dependency Review: sin nuevas vulnerabilidades High/Critical
ArchUnit: PASS
JaCoCo: gates globales PASS
```

No se permite “arreglar” scanners mediante:

- desactivar reglas;
- excluir paquetes productivos;
- ocultar warnings con código equivalente;
- relajar RBAC;
- hacer endpoints públicos;
- pasar tokens por query string.

## 17.1 Webhooks externos

La autenticación de `POST /api/v1/internal/azure-events` es una frontera operacional externa y **no equivale** a OAuth2 Bearer JWT de la API de negocio. No cambia la matriz RBAC institucional.

Las credenciales de webhook no pueden tener defaults funcionales inseguros, aceptarse por query parameter ni aparecer en logs/evidencia. El AS-IS y los hallazgos se documentan en [Azure Runtime Integration](../integration/azure-runtime-integration.md) y TD-051/TD-052; TD-051/TD-052 quedaron resueltas en LB-001D.2: sin default funcional, fail-closed sin credencial y header único `aeg-sas-token`. `permitAll` de autorización no equivale a endpoint sin autenticación.

---

## 18. Checklist de cambio de seguridad

Antes de integrar un cambio:

```text
[ ] issuer validado exactamente
[ ] audience validada
[ ] idUsuario UUID requerido
[ ] roles solo desde el client de API
[ ] principal = idUsuario
[ ] Bearer inválido -> 401
[ ] rol insuficiente -> 403
[ ] CSRF solo se excluye en /api/v1/** (stateless, Bearer-only); revisar si aparecen cookies/sesión
[ ] cookies no autentican accidentalmente
[ ] CORS no se amplía sin necesidad
[ ] Layer 1 y Layer 2 no se mezclan
[ ] no hay secretos en logs/eventos
[ ] tests RBAC pasan
[ ] CodeQL pasa
[ ] Sonar Security pasa
```

Este documento representa el estado vigente después de la baseline de seguridad y la Fase 3.1
Realtime. Si cambia el mecanismo de autenticación, la estrategia CSRF o la matriz RBAC, debe
actualizarse en el mismo PR.
