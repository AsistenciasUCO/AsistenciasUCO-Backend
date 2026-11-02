---
status: active
type: security-finding
scope: backend
owner: backend-team
last-reviewed: 2026-11-02
---

# Hallazgo: la API administrativa del PDP autentica pero no autoriza del todo (0.1.7 → 0.1.8)

**Clasificación:** `SECURITY_FINDING` en el componente externo Security Baseline (Seguridad UCO). **Estado:**
`BLOCKED_BY_EXTERNAL_COMPONENT` (0.1.7); con 0.1.8 `PARTIALLY_RESOLVED_BY_EXTERNAL_COMPONENT`: las rutas por aplicación ya
autorizan, pero el listado global de usuarios/aplicaciones sigue abierto (ver "Re-evaluación con PDP 0.1.8"). **Consecuencia en este repositorio:** `SECURITY_BASELINE_PDP_PROFILE_SYNC_ENABLED=false`.

## Evidencia reproducible original (2026-10-26, laboratorio local PDP 0.1.7, solo lectura)

Sonda `GET` contra el PDP (`127.0.0.1:18080`) con el Bearer de cada cuenta de laboratorio. No se imprimen tokens ni cuerpos.

| Identidad | `GET /api/v1/applications` | `GET /api/v1/users` |
|---|---|---|
| ADMINISTRADOR | 200 | 200 |
| DOCENTE | 200 | 200 |
| ESTUDIANTE | 200 | 200 |
| Sin Bearer | 401 | — |

Cualquier token válido del realm lee el directorio administrativo del PDP: la API exige *autenticación* pero no
*autorización administrativa*. No se probaron escrituras (acción privilegiada sobre un recurso compartido no autorizada).

## Re-evaluación con PDP 0.1.8 (2026-11-02, laboratorio local, solo lectura)

**Estado: `PARTIALLY_RESOLVED_BY_EXTERNAL_COMPONENT`.** Misma técnica: `GET` con el Bearer PKCE real de cada cuenta de laboratorio
(`asistencias-uco-spa`, audiencia `security-baseline-pdp`); sin tokens ni cuerpos en la evidencia. Aplicación `asistencias-uco`.

| Identidad | `GET /applications` | `GET /users` | `GET /applications/{app}/security/{summary,roles,resources,administrators}` |
|---|---|---|---|
| ADMINISTRADOR (administrador de la aplicación) | 200 | 200 | 200 |
| DECANO | 200 | 200 | **403 `NOT_AUTHORIZED_TO_ADMINISTER`** |
| COORDINADOR | 200 | 200 | **403 `NOT_AUTHORIZED_TO_ADMINISTER`** |
| DOCENTE | 200 | 200 | **403 `NOT_AUTHORIZED_TO_ADMINISTER`** |
| ESTUDIANTE | 200 | 200 | **403 `NOT_AUTHORIZED_TO_ADMINISTER`** |

- **Corregido en 0.1.8:** las rutas administrativas **por aplicación** ya exigen ser administrador de esa aplicación (condición 1 de
  "Decisiones", cumplida para ese alcance). La consola de Seguridad de la SPA (`/app/admin/seguridad`) sigue funcionando con la sesión
  del administrador.
- **Residual (sigue abierto):** `GET /api/v1/users` y `GET /api/v1/applications` responden 200 a **cualquier** Bearer con la audiencia del
  PDP. Con el token de un ESTUDIANTE se obtienen las 2 aplicaciones (`id`, `name`, `description`, `baseUrl`, `enabledEnvironments`,
  `tenantId`) y los 28 usuarios del inquilino (`id`, `name`, `email`, `lastLoginAt`, `provider`, `tenantId`): exposición de datos
  personales entre roles. No se imprimieron valores en esta evidencia.
- **Sin probar:** escrituras (asignación/revocación de perfiles) y la identidad de servicio del backend: no se concedió ni elevó nada.
  `SECURITY_BASELINE_PDP_PROFILE_SYNC_ENABLED` permanece `false`.
- **Pregunta para Seguridad UCO:** si el acceso global a `users`/`applications` es intencional (directorio del inquilino) o debe restringirse
  al administrador de al menos una aplicación, como ya hace ADR-036 con el catálogo de roles y perfiles. Si se restringe, la cuenta de
  servicio del backend necesitaría ser administradora de `asistencias-uco` (decisión de Seguridad, no de este repositorio).
- Evidencia completa y contexto del laboratorio: `docs/work-items/SEC-BASELINE-018-upgrade-login/VALIDATION.md` (secciones N y O.4).

## Por qué no se habilita la sincronización

El backend necesita escribir asignaciones de perfil (`POST /api/v1/profiles/{id}/assignments`). Con esta API, una
credencial de servicio con privilegio mínimo no se puede distinguir de la de un docente; habilitarla exigiría
conceder o asumir más privilegio del aprobado y reutilizar secretos que no corresponden. Se mantiene:

- `SECURITY_BASELINE_PDP_PROFILE_SYNC_ENABLED=false` (valor por defecto en `application.yml`).
- Secreto propio `SECURITY_BASELINE_PDP_ADMIN_CLIENT_SECRET`, nunca el de administración del proveedor de identidad.
- Las cuentas creadas quedan `PENDIENTE_SINCRONIZACION`; la SPA lo muestra y nunca declara `ACTIVA` sin perfil confirmado.

## Decisiones que debe tomar Seguridad UCO

1. Que la API administrativa del PDP aplique autorización efectiva (rol/scope/audiencia administrativos) y responda 403
   a tokens de docente y estudiante.
2. Definir la identidad de servicio del backend (client credentials), su audiencia y el scope mínimo
   (`profiles:assign`/`profiles:revoke` sobre la aplicación `asistencias-uco`, sin lectura global de usuarios).
3. Decidir el mecanismo oficial de aprovisionamiento durante el desarrollo (por ejemplo `provision_asistencias.py users`
   ejecutado por el responsable) mientras 1 y 2 no estén aprobados.

## Condiciones para declarar `FULL_PDP_SYNC_READY`

Sonda con 403 para DOCENTE/ESTUDIANTE; cuenta de servicio con scope mínimo comprobado; reconciliación real de alta,
cambio y revocación de rol; sin acceso administrativo indebido. Hasta entonces el desarrollo ordinario continúa con las
cuentas de laboratorio ya migradas a perfiles y el alta de roles por el procedimiento del responsable.
