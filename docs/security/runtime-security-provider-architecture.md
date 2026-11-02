---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-10-26
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

## 1.1 Autorización explícita de la migración Security Baseline 0.1.7

El responsable del proyecto autorizó el 2026-10-26 la implementación directa del switch de
runtime `legacy|baseline` y de la integración con Security Baseline 0.1.7 en los checkouts
oficiales existentes, sin crear work item, worktree ni clon nuevo, y sin cambiar Domain,
Application ni contratos académicos. Esta autorización permite modificar únicamente el wiring
de Infrastructure, los adaptadores de seguridad, las pruebas observables y el frontend oficial
necesario para OIDC/BFF.

La evidencia AS-IS consultada es el checkout oficial del baseline con tag `v0.1.6` (fuente) y las imágenes
`davidalzate/*:0.1.7` en ejecución, el frontend oficial y este backend. Hallazgos contrastados contra la instalación real:

- `POST /internal/v1/embedded-access-decisions` responde `204 + X-Decision-Id` (allow), `403` (deny) o `401`; **no entrega
  identidad**. La cookie BFF `SECURITY_BASELINE_SESSION` solo la resuelve el PEP→PDP por el canal técnico
  `/internal/v1/bff-session-token`, reservado a la identidad técnica del PEP; el PEP proxy no inyecta el Bearer
  resuelto al backend y no soporta SSE. Por tanto **no existe un mecanismo oficial para que un backend consumidor
  obtenga una identidad verificable desde la cookie BFF**, y una cookie de otro origen tampoco llegaría al backend.
- Decisión: la API es **Bearer-only** en ambos modos. La SPA obtiene el access token del realm central con un cliente
  público (Authorization Code + PKCE S256, token solo en memoria, sin refresh token) y el login interactivo lo hace el
  BFF del PDP (aprovisiona la identidad). No se aceptan cookies, `X-User-Id` ni un `204` del PEP como identidad.
- El ambiente del PEP debe ser `dev|lab|prod` en 0.1.7 (`local` falla la validación de credencial de aplicación).
- Las rutas con variables (`/api/v1/grupos/{grupoId}/estudiantes`) se registran como recurso con la plantilla y el PEP
  las hace coincidir con rutas concretas (verificado con PEP/PDP reales).
- El binding `issuer+subject → Usuario.id` queda **resuelto** con persistencia propia (ver §5.4): migración
  SEC-BASELINE-017 en `gestion-asistencia-db` (tabla `IdentidadExterna`, vista y dos SP canónicos).

`SECURITY_FINDING` (resuelto en el entorno local): el Compose del baseline del repositorio de Seguridad trae credenciales
estáticas en la configuración de PDP/PEP. La instalación local única (`infra/security-baseline/compose.yaml`) no contiene
ningún secreto literal: todos llegan de `%USERPROFILE%\.asistencias-security-lab\lab.env` (fuera de Git) y los secretos de
los clientes técnicos del realm se rotaron fuera del repositorio. Una instalación compartida debe usar su gestor de secretos.

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
| Runtime authentication | `JwtClaimsExtractor`, `JwtDecoder` | Keycloak legacy / Security Baseline 0.1.7 | `app.adapters.security.provider=legacy|baseline` (`keycloak` es alias legacy) |
| Identity provisioning | `IdentityProviderPort` | Keycloak Admin API (realm legacy o realm central) | `app.adapters.identity.provider=legacy|baseline` (`keycloak` es alias legacy); **debe coincidir con el selector de seguridad** o el arranque falla |

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
      provider: ${APP_ADAPTERS_SECURITY_PROVIDER:baseline}

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

`app.adapters.identity.provider` toma por defecto el valor de `APP_ADAPTERS_SECURITY_PROVIDER`; si se define explícitamente
y no coincide, el contexto no arranca (`SecurityProviderCoherence`). Un valor desconocido de cualquiera de los dos selectores
falla el binding del enum: no se convierte en `legacy`. En IntelliJ basta con definir
`APP_ADAPTERS_SECURITY_PROVIDER=baseline` (variables del modo baseline: ver `infra/security-baseline/README.md`).

No existe una segunda bandera operativa para seleccionar seguridad. `APP_ADAPTERS_SECURITY_PROVIDER`
es la única fuente del modo; `legacy` conserva el issuer y roles históricos, mientras `baseline`
usa el issuer central, el PEP 0.1.7 y su autorización por decisión. No hay fallback entre ambos.

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

### 5.4 Modo baseline: vínculo persistente `issuer + subject → Usuario.id`

`ExternalIdentityResolver` resuelve la identidad ya validada por el `JwtDecoder` (firma, issuer exacto, audiencia
`asistencias-api`, exp/nbf):

1. Si `issuer + subject` ya está vinculado y **vigente**, se devuelve ese `Usuario.id` (siempre que el usuario siga
   activo) **sin mirar el correo**: un cambio de correo en el IdP no reasigna ni apropia cuentas.
2. Si no hay vínculo, se exige `email_verified=true`, se normaliza el correo, debe haber exactamente **un** usuario
   institucional activo y se registra el vínculo de forma atómica con `usp_vincular_identidad_externa`.
3. Se rechazan (401) vínculo revocado, usuario inactivo, correo ambiguo/no verificado y conflictos: identidad ya
   ocupada por otra cuenta, cuenta con otra identidad vigente del mismo emisor, identidad revocada. Nunca se elige una
   fila al azar ni se crea usuario automáticamente.
4. El claim `idUsuario` (atributo del IdP usado por la UI) es solo coherencia: si viene y difiere del usuario vinculado
   se rechaza. El `sub` nunca se convierte en UUID interno.

Persistencia (DB personal de John, rama `feat/security-baseline-017-identidad-externa`): `IdentidadExterna`
(`UNIQUE(emisor, sujeto)`, único filtrado `(usuario, emisor) WHERE estado=1`), vista `uv_identidad_externa`,
`usp_vincular_identidad_externa` y `usp_revocar_identidad_externa`. Al arrancar en modo baseline,
`ExternalIdentitySchemaCompatibilityVerifier` comprueba que existan; si falta algo el arranque falla (sin degradar a
resolución por correo). Trazabilidad: el alta y cada rechazo se registran con motivo, `subject` e `issuer`, nunca con
tokens ni correos; la DB conserva `idCorrelacion`, fecha de vinculación y de revocación.

La cookie de sesión BFF no es una credencial de este backend: `SecurityBaselinePepClient` solo evalúa con un Bearer
único y bien formado ya autenticado por Spring Security; sin Bearer responde 401 sin consultar al PEP.

### 5.5 Identidad institucional para la SPA: `GET /api/v1/auth/me`

La primera vinculación por correo ocurre en el backend, al autenticar el Bearer (§5.4), así que la SPA **no** puede
exigir el claim `idUsuario` en el JWT: una persona con el mismo correo institucional verificado en el realm central y en
SQL Server todavía no tiene ese atributo. Tras el intercambio PKCE, Angular llama a `GET /api/v1/auth/me`
(`AuthController`, solo Bearer; `credentials: 'omit'`) y establece la sesión con la respuesta:

```json
{ "exitoso": true, "datos": { "idUsuario": "<UUID de Usuario.id>", "roles": ["DOCENTE"] } }
```

- Sin parámetros: ni UUID ni correo del cliente participan; el UUID es el principal ya resuelto por
  `InstitutionalJwtAuthenticationConverter` (vínculo persistente) y los roles son las authorities `ROLE_*` del mismo token
  (`resource_access["asistencias-api"].roles`). Se publican solo estos dos campos (sin tokens, correo ni datos personales).
- Sin Bearer válido responde 401; un token rechazado por el vínculo (revocado, inactivo, ambiguo, conflicto, `idUsuario`
  contradictorio) responde 401 antes de llegar al controlador. Un 5xx o un fallo de red no concede sesión.
- Es `authenticated()` en `SecurityConfig` y recurso con los cinco roles en el PDP (`access: AUTHENTICATED`): cualquier rol
  institucional puede conocer su propia identidad; una cuenta sin rol institucional queda denegada por el PEP.
- La SPA toma de `datos.roles` el rol principal por jerarquía (`ADMINISTRADOR > DECANO > COORDINADOR > DOCENTE >
  ESTUDIANTE`) y conserva todos en `User.roles`; guards, directiva y menú aceptan cualquier rol efectivo. El claim
  `idUsuario` del token ya no es obligatorio en modo baseline y, si viene, nunca sustituye al UUID del backend
  (el backend lo rechaza si contradice el vínculo). Modo `legacy` sin cambios: exige el claim.
- Todo lo que la SPA muestre por rol es ergonomía: la autoridad sigue siendo PEP/PDP, `SecurityConfig` y Application/DB.

### 5.6 Starter oficial `security-pep-integration-spring-boot-starter` 0.1.3: no se incorpora

Seguridad UCO comunicó la dependencia `co.edu.uco:security-pep-integration-spring-boot-starter:0.1.3`. Evaluada el
2026-10-26 contra el tag `v0.1.3` del repositorio `Seguridad-UCO/securityBaseline` (idéntico en `v0.1.7`: solo cambia su
README):

| Criterio | Hallazgo |
|---|---|
| Disponibilidad | No está en Maven Central (404); se publica en GitHub Packages (`maven.pkg.github.com/Seguridad-UCO/securityBaseline`), que exige credencial. No se resolvió el POM publicado: el análisis es del código fuente del tag. |
| Plataforma | Padre `spring-boot-starter-parent` 4.1.0, Java 25; dependencias `spring-boot-starter-webflux` y `spring-boot-starter-webclient`. Este backend es Spring Boot 4.0.8 + Spring MVC (Servlet) con JPA bloqueante. |
| Enforcement | `PepEnforcementWebFilter` implementa `org.springframework.web.server.WebFilter` (reactivo); no existe un `Filter`/`OncePerRequestFilter` Servlet. Su README lo define como "starter Spring Boot WebFlux". |
| Autoconfiguración | `PepEnforcementAutoConfiguration` se activa por defecto (`security.enabled` y `security.pep.enforcement.enabled` con `matchIfMissing=true`) y no condiciona por tipo de aplicación; añadiría WebFlux/WebClient al classpath MVC y registraría un filtro que el contenedor Servlet nunca ejecuta, dejando la ilusión de un segundo control. |
| Contrato | Idéntico al de `SecurityPepEnforcementFilter` + `SecurityBaselinePepClient`: `POST /internal/v1/embedded-access-decisions` con `applicationName`, `environment`, `path`, `method`; cabeceras `Authorization`, `X-Application-Credential`, `X-Request-Id`, `X-Correlation-Id` (`[A-Za-z0-9_.:-]{1,128}`); 2xx = ALLOW (`X-Decision-Id`), 401/403 tal cual, cualquier otro estado o error = 503 fail-closed. Diferencias deliberadas: el adaptador MVC exige un único Bearer (el starter también acepta la cookie `SECURITY_BASELINE_SESSION`, que este backend no acepta, ver §1.1) y solo acepta `204` como ALLOW. |

Decisión: **no agregar la dependencia** ni migrar a WebFlux. Se conserva el adaptador MVC, que es el único punto de
enforcement (registrado dentro de la `SecurityFilterChain`, `FilterRegistrationBean` servlet deshabilitado), de modo que no
hay doble filtro. Reabrir si Seguridad UCO publica una variante Servlet/MVC compatible con Boot 4.0.x o este backend sube a
Boot 4.1; en ese caso habría que verificar que no instale un segundo filtro.

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

`/api/v1/**` es una API REST **stateless** (`SessionCreationPolicy.STATELESS`). En legacy la API
de negocio se autentica exclusivamente con una credencial explícita:

```http
Authorization: Bearer <jwt>
```

El webhook `/api/v1/internal/azure-events` se autentica con el header `aeg-sas-token`, validado
por `AzureEventGridAuthFilter` (fail-closed). Su `permitAll` en `authorizeHttpRequests` solo
significa que no exige JWT de negocio; no es un endpoint sin autenticación.

En baseline el frontend consulta el PDP central con `credentials: include`, pero este backend no
acepta una cookie BFF como identidad institucional hasta cerrar el binding descrito en §5.4. Un
navegador no
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
flujo. El modo baseline NO introduce cookie hacia la API: la SPA usa PKCE y envía Bearer; la cookie BFF solo existe entre el navegador y el PDP, por lo que CSRF sigue sin aplicar a `/api/v1/**`.

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

`allowCredentials=false` es definitivo en ambos modos: la cookie BFF no es una credencial de este backend y el
frontend envía `credentials: omit` a la API (Bearer solo hacia el origen de la API). Nunca debe combinarse
`allowedOrigins("*")` con credenciales.

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

La definición compartida y exacta, **un registro por endpoint**, es `infra/security-baseline/access-catalog.json`
(método, ruta con `{variables}`, roles, `access`, ownership contextual, perfiles del PDP y exenciones). La usan
`provision_asistencias.py` para el PDP y `AccessCatalogConsistencyTest` para comprobarla contra Spring MVC
(`RequestMappingHandlerMapping`: qué endpoints existen) y contra el `SecurityFilterChain` real (qué rol llega a cada uno).
La tabla siguiente resume las familias; ante cualquier diferencia manda `SecurityConfig` y el catálogo debe corregirse.

Flujo para una HU nueva (sin workflows ni sistemas adicionales):

1. Implementar el controlador y su regla en `SecurityConfig` (sin `/**` ni acceso a todos los roles por comodidad).
2. `python -X utf8 -I infra/security-baseline/scripts/provision_asistencias.py catalog` (offline) lista los endpoints sin
   regla y el esqueleto a completar; `AccessCatalogConsistencyTest` falla igual, así que un endpoint sin regla bloquea la
   validación y nunca recibe acceso por defecto.
3. Completar la fila (`roles`, `access`, `ownership`) en `access-catalog.json`; la prueba exige que coincida con `SecurityConfig`.
4. `provision_asistencias.py pdp` registra solo lo que falta (recursos, concesiones, perfiles); `verify` compara el PDP con el
   catálogo y `verify-decisions` pide la decisión real al PEP (ALLOW/DENY) para cada endpoint y rol. Las concesiones que el
   catálogo no autoriza se informan como deriva y solo se revocan con `pdp --prune`.
5. Documentar la HU en su work item y, si cambia una familia, actualizar esta tabla.

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
| `POST /api/v1/grupos` | `DOCENTE` (solo crea a su nombre, lo exige la DB), `COORDINADOR` o `ADMINISTRADOR` |
| `POST/PUT/DELETE /api/v1/grupos/**` (resto) | `COORDINADOR` o `ADMINISTRADOR` |
| `POST/PUT/PATCH /api/v1/sesiones/**` | `DOCENTE` |
| `GET /api/v1/sesiones/*/qr-token` | `DOCENTE` |
| `POST /api/v1/asistencias` | `DOCENTE` |
| `POST /api/v1/asistencias/lote` | `DOCENTE` |
| `POST /api/v1/asistencias/revisiones` | `ESTUDIANTE` |
| `POST /api/v1/asistencias/consultas/grupo` | `DOCENTE`, `COORDINADOR` o `ADMINISTRADOR` |
| `GET /api/v1/auth/me` | cualquier usuario autenticado (identidad institucional propia, §5.5) |
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

En modo baseline `IdentityProviderPort` opera sobre el realm central (`app.providers.security-baseline-identity`) con el
cliente de servicio `asistencias-provisioner` (roles mínimos de `realm-management`; secreto solo por entorno) y asigna
los cinco roles como client roles de `asistencias-api`; nunca contacta el Keycloak legacy (8081). El PDP mantiene sus
propias asignaciones por aplicación (`provision_asistencias.py`); ambas fuentes se aprovisionan con la misma matriz que
`SecurityConfig`. Verificado: `POST /api/v1/usuarios` crea la cuenta en `security-baseline` con `idUsuario`.

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
[ ] idUsuario UUID requerido (legacy) / vínculo issuer+subject resuelto (baseline) y GET /api/v1/auth/me
[ ] endpoint nuevo con fila en infra/security-baseline/access-catalog.json (AccessCatalogConsistencyTest en verde)
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
