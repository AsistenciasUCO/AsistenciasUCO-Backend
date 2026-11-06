# HU176B — Diseño: acceso del estudiante en su primer ingreso (perfil en el PDP)

> **Estado:** PROPUESTA, sin implementar. No modifica `CONTRACT.md` (congelado con `CONTRACT.sha256`); si se aprueba, entra como
> addendum formal con nuevo hash.
> **Fecha:** 2026-11-06 · **Rama:** `feature/HU176B-matricula-qr`
> **Origen:** prueba manual de la matrícula por QR (estudiante `1001478944`, `sergio@uco.edu.co`).

## 1. Síntoma

Un estudiante que se registra por QR/PIN, verifica su correo e inicia sesión recibe en la pantalla de login:
«La sesión fue autenticada, pero no tiene permiso para acceder (403). Contacta al administrador.»
Keycloak lo autentica bien; el rechazo viene del PEP/PDP. Aplica a **toda cuenta nueva** creada por el backend (registro por QR y
registro manual del docente), no solo a HU176B.

## 2. Causa raíz (verificada)

| Hecho | Evidencia |
|---|---|
| La persona existe en el PDP tras el primer login, pero sin perfil «Estudiantes» ni rol directo. | Lectura del PDP con la sesión del administrador del laboratorio: `perfil Estudiantes asignado: False`, `rol directo Estudiante: False`. |
| El backend intenta asignar el perfil al crear la cuenta (`ProfileSyncingIdentityProvider.crearCuenta`), pero en ese momento la persona **aún no existe** en el PDP. | `SecurityBaselinePdpProfileClient`: resultado `PENDIENTE_PERSONA_NO_REGISTRADA` («se registra en su primer acceso»). |
| Después del primer acceso nadie reintenta la asignación. | No hay job ni hook; solo existe `POST /api/v1/identidad/cuentas/acceso/reconciliaciones`, reservado a ADMINISTRADOR/DECANO/COORDINADOR. |
| La sincronización privilegiada está **apagada por defecto**. | `application.yml`: `app.providers.security-baseline-pdp-admin.enabled=${SECURITY_BASELINE_PDP_PROFILE_SYNC_ENABLED:false}`. |
| El token de servicio del backend no lleva la audiencia del PDP. | Claim `aud` vacío en el token de `asistencias-provisioner`; `provision_asistencias.py keycloak` solo la concede con `--enable-pdp-profile-sync` (opt-in explícito del responsable). |

**No verificado:** si el PDP permite crear una persona por adelantado, o si tiene una opción de perfil por defecto en el primer
acceso (su API no se pudo leer en el laboratorio).

## 3. Qué es de quién

Hay dos Keycloak distintos:

- **Nuestro:** puerto 8081, realm `asistencias-uco` (modo `legacy`, scripts en `infra/keycloak`).
- **Del equipo de seguridad:** stack Security Baseline (`asistencias-security-lab-*`): Keycloak `:19090` realm `security-baseline`,
  PDP `:18080`, PEP `:18081` y sesión BFF.

En modo `baseline` (el que se ejecuta hoy) el Keycloak, el PDP y el PEP **son componente del equipo de seguridad**. El código de
`KeycloakIdentityProviderAdapter`, `SecurityBaselinePdpProfileClient`, `SecurityPepEnforcementFilter` y el catálogo
`access-catalog.json` es del backend y se puede cambiar sin ellos.

### 3.1 Cambio ya aplicado en su componente (informar)

Durante la prueba se declaró el atributo `idUsuario` en el **perfil de usuario del realm `security-baseline`** (visible y editable
solo por el administrador), con la cuenta administradora del laboratorio. Sin esa declaración Keycloak 26 descarta en silencio el
atributo y la verificación de correo responde 500 («No existe la cuenta de identidad del usuario que verifica su correo»), porque
`KeycloakIdentityProviderAdapter.buscarCuentaPorIdUsuario` busca por `idUsuario`. El paso 4 de
`infra/security-baseline/scripts/provision_asistencias.py` (commit `67a006c3`) lo repite de forma idempotente en el laboratorio.
**Su provisioning real de realms debe incluir el mismo atributo.**

## 4. Diseño propuesto (backend + frontend)

### 4.1 Endpoint «sincronizar mi acceso»

`POST /api/v1/identidad/mi-acceso/sincronizacion` (nombre a confirmar), sin cuerpo.

- **Autenticación:** exige sesión/JWT válido. **Queda fuera del PEP** (se agrega a `SecurityPublicEndpoints.pepExempt()` y a la
  sección `exempt` de `access-catalog.json`, con su razón) porque en este punto el usuario aún no tiene perfil y el PEP lo
  rechazaría por diseño. Fuera del PEP no significa público: `SecurityConfig` lo marca `authenticated()`.
- **Identidad y roles:** el usuario sale del token (`AuthenticatedUserResolver`); los roles salen de la base de datos
  (`InstitutionalScopePort.findRolesActivosByUsuario`), que es la fuente de verdad. **El cliente no envía correo ni rol**, así que
  no puede pedir más acceso del que le corresponde.
- **Acción:** para cada rol activo llama a `AccessProfilePort.sincronizar(correo, rol)`, la misma operación idempotente que usa
  `AccesoCuentaService.reconciliar`. Se implementa como método propio de ese servicio (`reconciliarPropio`) para no duplicar lógica.
- **Casos:** usuario inactivo → no asigna nada; PDP caído o pendiente → responde el estado y registra el desfase existente
  (`UsuarioSincronizacionIdentidadPort`); nunca concede acceso por defecto (fail-closed).
- **Salvaguardas:** límite de peticiones por usuario, auditoría del evento y correlationId propagado.
- **Respuesta:** `ApiDataResponse<...>` con el estado (`ASIGNADO`, `PENDIENTE_*`), para que la SPA sepa si reintentar.

### 4.2 Frontend (presentación pura)

Tras el login, ante el primer 403, la SPA llama al endpoint y reintenta **una sola vez**. No interpreta textos de error; decide por el
estado que devuelve el backend. Si sigue sin acceso, muestra el mensaje institucional que entregue el backend.

### 4.3 Configuración

`SECURITY_BASELINE_PDP_PROFILE_SYNC_ENABLED=true` y la credencial de administración del PDP. **Depende del punto 5.1.** El endpoint
debe quedar detrás de ese interruptor: apagado, responde un estado «no disponible» sin efectos.

### 4.4 Contrato y pruebas (SDD/TDD)

- El contrato HU176B está congelado: el endpoint nuevo requiere **addendum A5**, nuevo `CONTRACT.sha256` y actualización de
  `openapi-golden-path.yaml` en `AsistenciasUCO-Infra` con verificación cross-repo (`infra.ps1 verify`).
- Pruebas RED primero: caso de uso (roles desde BD, sin datos del cliente, usuario inactivo, PDP pendiente/caído), controlador
  (401 sin sesión, 200 con sesión), exención del PEP en `SecurityPepEnforcementFilterTest` y `RbacSecurityFilterChainTest`,
  `access-catalog` y ArchUnit; spec del frontend para el reintento único.

## 5. Lo que requiere al equipo de seguridad

1. **(Bloqueante) Permiso para asignar perfiles en el PDP.** El PDP 0.1.7 no distingue administradores entre los tokens con la
   audiencia `security-baseline-pdp`. Se necesita una de estas dos cosas: (a) un cliente de servicio de **privilegio mínimo**,
   limitado a asignar perfiles a personas; o (b) su aprobación explícita para concederle esa audiencia a `asistencias-provisioner`.
2. **(Alternativa preferible, en su componente)** que el PDP asigne un **perfil por defecto en el primer acceso**, según el client
   role de Keycloak (`ESTUDIANTE` → perfil «Estudiantes»). Con eso no hace falta el endpoint de §4.1 ni el permiso de §5.1.
3. **(Pregunta)** ¿El PDP permite **crear una persona por adelantado**? Si sí, el backend asignaría el perfil al registrar y no
   dependería del primer login.
4. **(Informativo)** Incluir `idUsuario` en el perfil de usuario de sus realms (ver §3.1).

### Mensaje sugerido para el equipo

> Estamos integrando el registro de estudiantes por QR (HU176B). Las cuentas nuevas reciben 403 en su primer login: el PDP crea la
> persona en ese momento sin perfil, y nuestra sincronización de perfiles no puede asignarlo antes porque la persona no existe ni
> tiene permiso para hacerlo (el token de servicio no lleva la audiencia del PDP y el PDP 0.1.7 no distingue administradores).
> Necesitamos que nos indiquen cuál de estas opciones prefieren: (1) un perfil por defecto en el primer acceso según el client role
> de Keycloak; (2) un cliente de privilegio mínimo para asignar perfiles; (3) otra vía, p. ej. crear la persona por adelantado.
> Además: declaramos el atributo `idUsuario` en el perfil de usuario del realm `security-baseline` del laboratorio; sus realms
> reales necesitan lo mismo o la verificación de correo falla.

## 6. Mitigación mientras tanto (solo laboratorio)

Para completar una prueba manual, asignar a mano el perfil «Estudiantes» a la persona en el PDP del laboratorio (con la sesión del
administrador del laboratorio y el script `provision_asistencias.py` / su API). No sustituye el arreglo: cada estudiante nuevo
necesitaría lo mismo.

## 7. Otros hallazgos de la prueba (fuera de este diseño)

- La cuenta de Keycloak se crea con contraseña **temporal** (`UPDATE_PASSWORD`): el primer login pide cambiarla. Comportamiento
  previo a HU176B (TD-079 en la guía de IDENT-001).
- En el entorno local no sale correo salvo que se configure SMTP (`APP_ADAPTERS_NOTIFICATION_PROVIDER=outbox`,
  `OUTBOX_NOTIFICATION_SENDER=smtp`, `SMTP_NOTIFICATION_*`). `AsistenciasUCO-Infra/scripts/orchestration/init-env.ps1` borraba esas
  variables del `.env` del backend al regenerarlo; el arreglo está **sin commit** en `AsistenciasUCO-Infra`.
