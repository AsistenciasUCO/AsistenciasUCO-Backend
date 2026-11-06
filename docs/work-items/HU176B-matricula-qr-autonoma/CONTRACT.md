---
status: draft
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# CONTRACT — HU176B: Matrícula autónoma por QR/PIN

> **Work Item:** `HU176B-matricula-qr-autonoma`
> **Estado:** `CONTRACT_FROZEN` el 2026-11-04 (hash en `CONTRACT.sha256`). El owner aprobó D1–D9 del [PLAN](PLAN.md). **La aprobación de seguridad sigue pendiente**: el owner pidió congelar sin ella; si seguridad exige cambios, el contrato se reabre con un addendum y un hash nuevo. Ningún cambio posterior se hace sin actualizar el hash.
> **Supuestos del borrador:**
> - D1(a): HU175 queda fuera.
> - **D2 (resuelta 2026-11-04): QR/PIN rotativo cada 60 s**, con tolerancia del token anterior, rotación en servidor y SSE dirigido al docente (ver PLAN §8.1).
> - D3(a): la matrícula queda activa (confirmada).
> - IAM-001 no bloquea: se usa el patrón de HU050 con la deuda TD-015.
> - D4–D9: aceptadas el 2026-11-04 con las recomendaciones del PLAN.
> - `DURACION_MAXIMA_MINUTOS` es una variable global administrable (siembra inicial 60); ver PLAN §8.1.
> - D4(a): QR generado en el backend con ZXing.
> - D5(a): solo se usan proxies confiables.
> - D6(a): se retira el SP autónomo.
> - D7(a): rate limit en memoria.
> - D8(b): se reutiliza el formulario de `register`.
> - D9(a): con interruptor.

---

## 1. Matriz AS-IS / TARGET

| Elemento | AS-IS | TARGET | Compatibilidad |
|---|---|---|---|
| `dbo.TokenMatriculaGrupo`, `dbo.uv_token_matricula_grupo` | — | Nuevos (§2.2) | Aditivo |
| `usp_abrir_ventana_matricula_grupo`, `usp_cerrar_ventana_matricula_grupo` | — | Nuevos (§2.3) | Aditivo |
| `usp_validar_token_matricula_publico` | — | Nuevo (§2.4) | Aditivo |
| `usp_matricular_estudiante_por_token` (+ `usp_internal_resolver_token_matricula`) | — | Nuevos (§2.5) | Aditivo |
| `usp_rotar_token_matricula_grupo`, `usp_consultar_ventanas_matricula_por_rotar` | — | Nuevos (§2.3b) | Aditivo |
| `POST /api/v1/grupos/{grupoId}/matricula-qr/rotacion` | — | Nuevo (§3.1b) | Aditivo |
| `RealtimePublisherPort.publishToUser` | — | Nuevo, entrega dirigida (§5) | Aditivo; `publish` no cambia |
| `usp_registrar_estudiante_en_grupo_autonomo` | Existe, sin RBAC | Se retira | Sin consumidores |
| `POST/DELETE /api/v1/grupos/{grupoId}/matricula-qr` | — | Nuevos (§3.1, §3.2) | Aditivo |
| `POST /api/v1/public/matricula-qr/validar`, `POST /api/v1/public/matricula-qr/registro` | — | Nuevos, **públicos** (§3.3, §3.4) | Aditivo |
| `POST /api/v1/estudiante/matricular-grupo` | 501 | Implementado (§3.5) | El body cambia (antes nadie lo recibía) |

---

## 2. Base de Datos

### 2.1 Reglas comunes

Son las reglas del contrato del grupo A, §2.1:
- `XACT_ABORT`, transacción y savepoint;
- evaluar `IS NULL`;
- lecturas sobre vistas y escrituras en `usp_internal_*`;
- mensajes por código y tupla canónica en los comandos.

**Excepción declarada:** `usp_validar_token_matricula_publico` y el modo público de `usp_matricular_estudiante_por_token` **no tienen ejecutor**. Su credencial es el **hash del token** (o el PIN) de una ventana activa. Las demás validaciones (grupo, cupo, cruce) se mantienen.

### 2.2 Tabla `dbo.TokenMatriculaGrupo` (migración + `schema/tables/`)

```sql
id UNIQUEIDENTIFIER PK,
grupo UNIQUEIDENTIFIER NOT NULL -> FK Grupo(id),
docenteEmisor UNIQUEIDENTIFIER NOT NULL -> FK Docente(id),
tokenHash VARBINARY(32) NOT NULL,           -- SHA-256 del token VIGENTE (el token en claro nunca se guarda)
pin NCHAR(6) NOT NULL,                       -- PIN vigente, solo dígitos
tokenHashAnterior VARBINARY(32) NULL,        -- (D2) token inmediatamente anterior, válido durante la tolerancia
pinAnterior NCHAR(6) NULL,                   -- (D2) PIN anterior
fechaRotacionUtc DATETIME2(0) NOT NULL,      -- (D2) instante en que el token vigente empezó a regir
estado NVARCHAR(10) NOT NULL,                -- ACTIVA / CERRADA / EXPIRADA, desde los parámetros MATRICULA_QR/ESTADO_*
fechaEmisionUtc DATETIME2(0) NOT NULL,       -- apertura de la ventana
fechaExpiracionUtc DATETIME2(0) NOT NULL,    -- (D2) fin de la ventana = emisión + DURACION_MAXIMA_MINUTOS
fechaCierreUtc DATETIME2(0) NULL,
totalMatriculas INT NOT NULL DEFAULT 0,
CONSTRAINT UQ_TokenMatriculaGrupo_tokenHash UNIQUE (tokenHash)
-- índice filtrado: UNIQUE (tokenHashAnterior) WHERE tokenHashAnterior IS NOT NULL
-- índice filtrado: UNIQUE (pin) WHERE estado = 'ACTIVA'  (el literal del índice es un valor del catálogo, documentado como la única excepción técnica)
-- índice filtrado: UNIQUE (grupo) WHERE estado = 'ACTIVA' (una ventana activa por grupo)
-- la unicidad del PIN frente a pinAnterior de OTRAS ventanas activas se garantiza en el SP (UPDLOCK, HOLDLOCK)
```

`uv_token_matricula_grupo` expone:
- todos los campos, salvo `tokenHash` y `tokenHashAnterior`;
- `estaVigente = IIF(estado = ACTIVA AND SYSUTCDATETIME() < fechaExpiracionUtc, 1, 0)`;
- (D2) `debeRotar = IIF(estaVigente = 1 AND SYSUTCDATETIME() >= DATEADD(SECOND, <ROTACION_SEGUNDOS>, fechaRotacionUtc), 1, 0)`;
- los datos del grupo para mostrar: `nombreAsignatura`, `nombreGrupo`, `nombreDocente`, `cupoDisponible`.

> Si el owner no acepta el literal en los índices filtrados, la unicidad se garantiza solo en el SP, con `UPDLOCK, HOLDLOCK`, y se pierde la garantía física (decisión técnica que se documenta en la fase 02).

### 2.3 Ventana del docente

**`usp_abrir_ventana_matricula_grupo`**:
- Parámetros:
  - `@idGrupo`, `@tokenHash VARBINARY(32)`, `@pin NCHAR(6)`, `@idCorrelacion`, `@idUsuarioEjecutor`.
  - El token y el PIN los genera el backend con `SecureRandom`; la DB solo recibe el hash.
- Validaciones:
  - RBAC `DOCENTE` + titularidad `GRUPO` (el docente debe ser titular);
  - el grupo está activo y tiene cupo (`usp_validar_cupo_disponible_grupo_interno`).
- Efectos:
  - **cierra** cualquier ventana `ACTIVA` del grupo;
  - inserta la nueva con `fechaRotacionUtc = SYSUTCDATETIME()`, sin token anterior, y `fechaExpiracionUtc = DATEADD(MINUTE, <MATRICULA_QR/DURACION_MAXIMA_MINUTOS>, SYSUTCDATETIME())`.
- Errores: PIN repetido entre las ventanas activas (vigente o anterior) → `MQR_006` (el backend reintenta con otro PIN).
- Resultset: la tupla canónica + `idVentana` + `fechaExpiracionUtc` (extensión documentada del resultset de comando).

**`usp_cerrar_ventana_matricula_grupo`**:
- Parámetros: `@idGrupo`, `@idCorrelacion`, `@idUsuarioEjecutor`.
- RBAC y titularidad iguales.
- Pasa la ventana activa a `CERRADA` y registra `fechaCierreUtc`.
- Es idempotente: sin ventana activa, responde éxito sin cambios.

### 2.3b Rotación (D2)

**`usp_rotar_token_matricula_grupo`**:
- Parámetros: `@idGrupo`, `@tokenHash VARBINARY(32)`, `@pin NCHAR(6)`, `@forzar BIT = 0`, `@idCorrelacion`, `@idUsuarioEjecutor UNIQUEIDENTIFIER = NULL`.
- **Modos:**
  - `@idUsuarioEjecutor` no es NULL (regeneración manual): RBAC `DOCENTE` + titularidad; rota siempre.
  - `@idUsuarioEjecutor` es NULL (**job del servidor**): sin RBAC humano, **excepción declarada** (§2.1). Solo rota si `debeRotar = 1`; si no, responde éxito sin cambios (idempotente, protege ante dos instancias).
- Efectos: bloquea la ventana (`UPDLOCK, HOLDLOCK`), mueve `tokenHash`/`pin` a `tokenHashAnterior`/`pinAnterior`, guarda los nuevos y fija `fechaRotacionUtc = SYSUTCDATETIME()`. Si la ventana venció, la marca `EXPIRADA` y responde `MQR_001`.
- Resultset: tupla canónica + `idVentana`, `idDocenteEmisor`, `fechaRotacionUtc`, `fechaExpiracionUtc`.

**`usp_consultar_ventanas_matricula_por_rotar`**: lee `uv_token_matricula_grupo` con `debeRotar = 1` (`idGrupo`, `idDocenteEmisor`). Sin ejecutor (uso exclusivo del job). Limitado por parámetro de lote.

**Regla de validez del token** (`usp_internal_resolver_token_matricula`):
- Coincide con `tokenHash` / `pin` → válido mientras la ventana siga `ACTIVA` y vigente.
- Coincide con `tokenHashAnterior` / `pinAnterior` → válido solo si `SYSUTCDATETIME() < DATEADD(SECOND, <TOLERANCIA_SEGUNDOS>, fechaRotacionUtc)`.
- Cualquier otro caso → `MQR_001` (mismo mensaje, anti-enumeración).

### 2.4 `usp_validar_token_matricula_publico` (consulta, sin ejecutor)

- Parámetros: `@tokenHash VARBINARY(32) = NULL` | `@pin NCHAR(6) = NULL` (exactamente uno), `@idCorrelacion`.
- Delega en `usp_internal_resolver_token_matricula`, que expira de forma perezosa las vencidas: si `estado = ACTIVA` y ya pasó la expiración, la marca `EXPIRADA` dentro de su propia transacción corta.
- **Resultset de lectura:** `idGrupo`, `nombreAsignatura`, `nombreGrupo`, `nombreDocente`, `fechaExpiracionUtc`, `cupoDisponible`.
- Si el token o el PIN no existe, venció o está cerrado: `THROW` con `MQR_001`, el **mismo mensaje** en todos los casos (anti-enumeración).

### 2.5 `usp_matricular_estudiante_por_token`

| Parámetro | Tipo | Modo | Regla |
|---|---|---|---|
| `@tokenHash` / `@pin` | | ambos | Exactamente uno; debe resolver una ventana vigente (`MQR_001`) |
| `@idUsuarioEjecutor` | `UNIQUEIDENTIFIER = NULL` | **autenticado** si no es NULL | RBAC `ESTUDIANTE`. El estudiante es el del ejecutor |
| `@idTipoIdentificacion`, `@numeroIdentificacion INT`, nombres, `@correo`, `@password` (hash) | | **público** si `@idUsuarioEjecutor` es NULL | Reglas de `usp_sincronizar_usuario_interno`. Si el documento o el correo ya existen → `MQR_005` ("Ya tienes una cuenta: inicia sesión para matricularte"), sin cambios |
| `@idCorrelacion` | | ambos | |

Flujo (una transacción):
1. Resuelve la ventana con `UPDLOCK` (`usp_internal_resolver_token_matricula`).
2. Solo en modo público: `usp_sincronizar_usuario_interno` (**activo**, `correoConfirmado = 0`) y luego `usp_sincronizar_estudiante_interno`.
3. `usp_registrar_estudiante_en_grupo_interno` con `@codigoEstado` = valor del parámetro `MATRICULA_QR / ESTADO_MATRICULA` (D3). Valida cupo, cruce, duplicado (si ya está → `MQR_004`) y `EstudiantePrograma`.
4. `UPDATE TokenMatriculaGrupo SET totalMatriculas += 1`.
5. Éxito: `GEN_004` ("Matricula").

**Resultset:** la tupla canónica + `idEstudiante`, `idUsuario` e `idGrupo` (extensión documentada) para aprovisionar la identidad y publicar el evento.

### 2.6 Parámetros (`03_parametro.sql` / App Configuration `asistencias:matricula-qr:*`) y mensajes (`04_mensaje.sql`)

**Parámetros:**
- `MATRICULA_QR/ROTACION_SEGUNDOS` = 60 (D2)
- `MATRICULA_QR/TOLERANCIA_SEGUNDOS` = 60 (D2)
- `MATRICULA_QR/DURACION_MAXIMA_MINUTOS` = 60 (D2; siembra inicial; **variable global administrable** vía App Configuration `asistencias:matricula-qr:duracion-maxima-minutos`; reemplaza a `TTL_MINUTOS`). El backend la lee por `ParameterCatalogPort` en cada apertura y la pasa al SP, que no la quema
- `MATRICULA_QR/ESTADO_MATRICULA` = `A`
- `MATRICULA_QR/ESTADO_ACTIVA` / `ESTADO_CERRADA` / `ESTADO_EXPIRADA`
- App Configuration (backend):
  - `asistencias:red:cidrs-campus` (lista CIDR)
  - `asistencias:red:proxies-confiables` (lista CIDR)
  - `asistencias:matricula-qr:habilitada` (bool)
  - `asistencias:matricula-qr:limite-ip-por-minuto` = 20
  - `asistencias:matricula-qr:limite-registros-token-por-minuto` = 5

**Mensajes:**

| Código | HTTP | Mensaje (propuesta) |
|---|:-:|---|
| `MQR_001` | 404 | El código de matrícula no es válido o ya no está disponible. Solicítale al docente uno nuevo. |
| `MQR_002` | 403 | La matrícula por código solo está disponible desde la red Wi-Fi del campus UCO. |
| `MQR_003` | 429 | Demasiados intentos. Espera un momento antes de volver a intentarlo. |
| `MQR_004` | 409 | Ya estás matriculado en este grupo. |
| `MQR_005` | 409 | Ya tienes una cuenta registrada. Inicia sesión para completar tu matrícula. |
| `MQR_006` | 409 | (técnico) PIN repetido entre ventanas activas; el backend reintenta. |
| `MQR_007` | 400 | Debes indicar el código QR o el PIN, pero no ambos. |

Los errores de cupo y cruce reutilizan los códigos actuales del interno de HU050.

---

## 3. HTTP

### 3.1 `POST /api/v1/grupos/{grupoId}/matricula-qr` (docente)

- **Rol:** `DOCENTE`. Necesita una regla nueva en `SecurityConfig`: `HttpMethod.POST, "/api/v1/grupos/*/matricula-qr"` → `DOCENTE`, **antes** del comodín `POST /api/v1/grupos/**` (COORDINADOR, ADMIN) de la línea 190.
- **Use case:** valida la titularidad con `InstitutionalScopePort.canDocenteAccessGrupo` (403) y genera:
  - un `token` de 32 bytes aleatorios en Base64URL;
  - un `pin` de 6 dígitos;
  - el `tokenHash` = SHA-256.

  Después llama a `TokenMatriculaCommandPort.abrir` (reintenta hasta 5 veces si recibe `MQR_006`) y genera el PNG con `CodigoQrGeneratorPort.generarPng(urlMatricula, 320)`.
- **URL del QR:** `urlMatricula = <asistencias:frontend:url-base>/matricula/qr?t=<token>`.
- **201** `ApiDataResponse<VentanaMatriculaResponse>`:
  ```json
  { "idVentana": "uuid", "pin": "482913", "urlMatricula": "https://…/matricula/qr?t=…", "qrPngBase64": "iVBORw0…", "fechaExpiracionUtc": "2026-10-21T15:19:00Z" }
  ```
  `fechaExpiracionUtc` es el fin de la **ventana** (D2); el QR vigente se reemplaza cada `ROTACION_SEGUNDOS` (se incluye `fechaRotacionUtc` en la respuesta). El token en claro **solo** sale en esta respuesta y en los eventos `MATRICULA_QR_ROTADO` dirigidos al docente; no se puede consultar después.
- **Errores:** 401, 403, 404 (grupo), 409 (sin cupo), 404 si el interruptor está apagado (D9).

### 3.1b `POST /api/v1/grupos/{grupoId}/matricula-qr/rotacion` (docente, D2)

- **Rol:** `DOCENTE` (regla nueva en `SecurityConfig`, antes del comodín de la línea 190).
- Fuerza una rotación inmediata de la ventana activa (botón "Generar nuevo código" y recuperación tras reconectar el SSE). Llama a `usp_rotar_token_matricula_grupo` con ejecutor.
- **200** `ApiDataResponse<VentanaMatriculaResponse>` (mismo cuerpo que §3.1, token y PIN nuevos).
- **Errores:** 401, 403 (no titular), 404 (sin ventana activa o interruptor apagado `D9`).

### 3.1c Rotación automática (D2, sin endpoint)

- `RotacionTokenMatriculaScheduler` (`@Scheduled`, tick configurable, p. ej. cada 5 s) consulta `usp_consultar_ventanas_matricula_por_rotar` y, por cada ventana, genera token y PIN (`TokenGeneratorPort`), llama a `usp_rotar_token_matricula_grupo` (modo job), genera el PNG y publica `MATRICULA_QR_ROTADO` con `publishToUser(idDocenteEmisor, …)`.
- Un fallo en una ventana no detiene a las demás: log `ERROR` y métrica `matricula_qr_rotacion_total{resultado=error}`.

### 3.2 `DELETE /api/v1/grupos/{grupoId}/matricula-qr` (docente)
- **200** `ApiDataResponse<Void>`. Errores: 401, 403.
- Regla en `SecurityConfig` para `DELETE` con `DOCENTE`, antes de la línea 192.

### 3.3 `POST /api/v1/public/matricula-qr/validar` (**público**)

- **Request:** `{ "token": "…" }` o `{ "pin": "482913" }`.
- **Orden de los filtros:**
  1. Interruptor (D9): si está apagado → 404.
  2. Rate limit por IP → 429 `MQR_003`.
  3. Red del campus (`RedInstitucionalPort.perteneceACampus(ip)`) → 403 `MQR_002`.
  4. Validator de forma → 400 `MQR_007`.
  5. Use case: hash SHA-256 del token y SP §2.4.
- **200** `ApiDataResponse<CursoMatriculaQrResponse>` = `{ nombreAsignatura, nombreGrupo, nombreDocente, fechaExpiracionUtc, cupoDisponible }`.
- **Errores:** 404 `MQR_001` (genérico), 403, 429, 400.

### 3.4 `POST /api/v1/public/matricula-qr/registro` (**público**, crea la cuenta)

- **Request:**
  ```json
  { "token": "…", "idTipoIdentificacion": "uuid", "numeroIdentificacion": "1017555444",
    "primerNombre": "Ana", "segundoNombre": null, "primerApellido": "Gómez", "segundoApellido": null,
    "correo": "ana.gomez@uco.edu.co", "password": "<inicial>" }
  ```
  Se acepta `pin` en lugar de `token`.
- **Filtros:** los mismos de §3.3, más el rate limit por token.
- **Validator:** documento en rango `INT` y contraseña según `PasswordRegistroRule`, sin contraseña por defecto.
- **Use case:**
  1. Codifica la contraseña.
  2. Llama a `usp_matricular_estudiante_por_token` en modo público.
  3. `crearCuenta(..., ESTUDIANTE)` con la contraseña en claro; si falla, aplica el desfase de IAM-001.
  4. Publica `ESTUDIANTE_MATRICULADO`.
- **201** `ApiDataResponse<MatriculaQrResultadoResponse>` = `{ nombreAsignatura, nombreGrupo, usuario: "<numeroIdentificacion>", mensajeUsuario }`. **No** devuelve un token de sesión: el front redirige a `/login` con el usuario precargado.
- **Errores:** 400, 403, 404 `MQR_001`, 409 (`MQR_004`, `MQR_005`, cupo, cruce), 429, 500 controlado (`IAM_001`).

### 3.5 `POST /api/v1/estudiante/matricular-grupo` (estudiante autenticado)

- **Rol:** `ESTUDIANTE` (ya cubierto por `/api/v1/estudiante/**`, línea 142).
- **Request:** `{ "token": "…" }` o `{ "pin": "…" }`.
- **Filtros:** red del campus (403 `MQR_002`) + validator. Sin rate limit público, porque ya hay JWT.
- **Use case:** SP §2.5 en modo autenticado (`@idUsuarioEjecutor` = actor) y luego el evento.
- **201** `ApiDataResponse<MatriculaQrResultadoResponse>`.
- **Errores:** 400, 401, 403, 404, 409.

### 3.6 Seguridad y filtros

- `SecurityConfig`: `.requestMatchers(HttpMethod.POST, "/api/v1/public/matricula-qr/validar", "/api/v1/public/matricula-qr/registro").permitAll()`. Es la **única** apertura pública de negocio; se declara **explícita**, sin comodín `/public/**`.
- CSRF: hoy los endpoints son stateless con Bearer. Los públicos no usan cookies de sesión, así que no aplica token CSRF. Se verifica en el RED que la configuración actual no lo exija.
- `PublicEndpointRateLimitFilter` (`OncePerRequestFilter`) solo actúa sobre `/api/v1/public/**`. Lleva un mapa de ventanas deslizantes con expiración, de tamaño acotado (protección de memoria). Las claves son el **hash** de la IP y el hash del token.
- `ClientIpResolver.resolveClientIp(request)` aplica D5. **El valor para el log sigue saneado**: el valor de red y el de log son métodos distintos.
- `RedInstitucionalPort.perteneceACampus(String ip)` (aplicación) → `CidrRedInstitucionalAdapter`:
  - soporta IPv4 e IPv6 y CIDR;
  - si no hay rangos configurados, **niega** (fail-closed);
  - recarga los rangos ante un cambio en App Configuration (patrón `uco-azure` / `uco-catalogos`).

### 3.7 Capas

- **Entrada:** `MatriculaQrPublicaController` / `GrupoMatriculaQrController` / `EstudiantePortalController` → `{AbrirVentana|CerrarVentana|ValidarToken|RegistrarPorMatriculaQr|MatricularEstudianteAutenticado}InputPort` → `*Interactor` → `*UseCaseImpl`.
- **Salida:**
  - `TokenMatriculaCommandPort` / `TokenMatriculaQueryPort` → `TokenMatriculaJpaRepository` (`JpaProcedureExecutor` / native `EXEC`);
  - `RedInstitucionalPort`, `CodigoQrGeneratorPort` (ZXing), `TokenGeneratorPort` (`SecureRandom` + SHA-256), `PasswordEncoderPort`, `IdentityProviderPort`, `RealtimePublisherPort`.
- **Wiring:** `MatriculaQrWiringConfiguration`.

---

## 4. Frontend

| Elemento | Contrato |
|---|---|
| `app.routes.ts` | Rutas **sin** `authGuard`: `matricula/qr` y `matricula/pin` → `MatriculaQrComponent` (lazy). Se elimina la ruta `register` (D8) |
| `core/models/matricula-qr.model.ts` | `VentanaMatricula`, `CursoMatriculaQr`, `RegistroMatriculaQrPayload`, `MatriculaQrResultado` |
| `core/services/matricula-qr.service.ts` | Métodos `abrirVentana(grupoId)`, `cerrarVentana(grupoId)`, `validar({token}\|{pin})`, `registrar(payload)` y `matricularAutenticado({token}\|{pin})`. Signals: `ventanaActiva = signal<VentanaMatricula \| null>(null)` |
| `features/public/matricula-qr/matricula-qr.component.ts` | Standalone y OnPush. Lee `t` de la query y luego borra el token de la URL (`history.replaceState`, R5). Tiene tres estados con signals (`validando`, `curso`, `error`) y dos acciones: **Crear cuenta y matricularme**, con el formulario reutilizado de `register` (D8) dentro de un subcomponente con `input()` / `output()`, y **Ya tengo cuenta**, que lleva a `/login?returnUrl=/app/estudiante/materias&matricula=<pin>` (se pasa el PIN, no el token). Muestra los errores tal cual (`message`) |
| `features/student/student-courses` | El query param `matricularGrupo` se reemplaza por `matricula` (PIN). Llama a `matricularAutenticado({pin})`. El modal de código acepta solo un PIN de 6 dígitos |
| `teacher-matricula-modal.component.ts` | `grupoId = input.required<string>()`. Al abrir llama a `abrirVentana`. Muestra `<img [src]="'data:image/png;base64,' + ventana().qrPngBase64">`, el PIN en grande, "La ventana termina a las HH:mm" (hora absoluta, **sin** contador con timers) y los botones **Cerrar** y **Generar nuevo código** (`/rotacion`). **(D2)** Se suscribe a `MATRICULA_QR_ROTADO` con `RealtimeService` y reemplaza `ventana` (QR y PIN) sin `setInterval`; al reconectarse el SSE llama a `/rotacion` para refrescar. **Se retira** `api.qrserver.com` |
| `teacher-grupo-estudiantes.component.ts` | Se suscribe a `RealtimeService` y filtra `type === 'ESTUDIANTE_MATRICULADO' && payload.idGrupo === grupoId()`; luego vuelve a pedir el roster. Sin `setInterval` |
| Reglas | OnPush, `input()` / `output()`, sin `bypassSecurityTrust*`, sin generar ids en el cliente, sin parsear errores |

## 5. Realtime

**Cambio aditivo (D2):** `RealtimePublisherPort.publishToUser(String idUsuario, RealtimeEvent event)` entrega el evento **solo** a las conexiones SSE del usuario indicado (el `sub` / id del JWT con que se abrió `/realtime/stream`). `publish` (broadcast) no cambia. Igual que `publish`, no lanza excepción. Si el usuario no tiene conexiones, el evento se descarta.

```json
{ "type": "MATRICULA_QR_ROTADO", "payload": { "idGrupo": "uuid", "pin": "482913", "urlMatricula": "https://…/matricula/qr?t=…", "qrPngBase64": "iVBORw0…", "fechaRotacionUtc": "…", "fechaExpiracionUtc": "…" } }
{ "type": "ESTUDIANTE_MATRICULADO", "payload": { "idGrupo": "uuid", "origen": "QR" } }
```

- `MATRICULA_QR_ROTADO` lleva el token y **siempre** se envía con `publishToUser` al docente emisor.
- `ESTUDIANTE_MATRICULADO` se publica después del commit, también con `publishToUser` al docente emisor. **Sin PII**.
- El frontend ignora eventos cuyo `idGrupo` no sea el del modal o roster abierto.

## 6. Observabilidad

Se trazan los eventos `MATRICULA_QR_*` (incluido `MATRICULA_QR_ROTACION`) y los contadores `matricula_qr_total{resultado=ok|red_rechazada|rate_limit|token_invalido|conflicto}` y `matricula_qr_rotacion_total{resultado=ok|error}` (PLAN §3).

**Prohibido** loguear el token, el PIN, la contraseña, el documento o el correo.

## 7. Aprobación

| Rol | Estado | Fecha |
|---|---|---|
| Owner del producto (D1–D9) | ✅ | 2026-11-04 |
| Seguridad (endpoints públicos, rate limit, red) | ⏳ | — |
| Infra (rangos de red y proxies) | ⏳ | — |
| Congelamiento | ✅ (sin aprobación de seguridad) | 2026-11-04 |

---

## 8. Addendum A1 (2026-11-04) — fuente de los parámetros de rotación

Corrige una inconsistencia entre §2.3 y §2.6 detectada al derivar las pruebas RED. **Prevalece sobre §2.6 en lo que lo contradice.**

- `ROTACION_SEGUNDOS`, `TOLERANCIA_SEGUNDOS` y `DURACION_MAXIMA_MINUTOS` son **parámetros de la base de datos** (grupo `MATRICULA_QR`, tabla de parámetros). Los SP los leen con `ufn_obtener_parametro_int` y **no** los reciben del backend. Los SP de §2.3 y §2.3b conservan sus firmas.
- Son los mismos parámetros que lista `GET /admin/parametros` (pestaña "Parámetros Globales"). Por eso son la variable administrable que pidió el owner. La llave de App Configuration `asistencias:matricula-qr:duracion-maxima-minutos` **no se usa**.
- Siguen en App Configuration, para el backend: `asistencias:red:*`, `asistencias:matricula-qr:habilitada` y los límites de rate limit.
- El tick del job de rotación (§3.1c) es parámetro del backend y no cambia la regla de validez, que decide la DB.

---

## 9. Addendum A2 (2026-11-04) — separación de SP por modo (gate de seguridad)

**Motivo:** el gate `test_contract_closure` (`PUBLIC_SECURITY_EXECUTOR_REQUIRED`) prohíbe el patrón `@idUsuarioEjecutor IS NOT NULL` en cualquier módulo SQL, es decir, un ejecutor opcional que cambia de modo. Los SP de modo dual definidos en §2.3b y §2.5 lo incumplen. El owner decidió **separar los SP** en lugar de abrir una excepción en el gate. **Este addendum reemplaza las firmas de §2.3b y §2.5 en lo que las contradice**; el comportamiento no cambia.

| SP | Modo | Ejecutor | Notas |
|---|---|---|---|
| `usp_rotar_token_matricula_grupo` | Manual del docente | **Obligatorio** (NULL → `GEN_002`) | RBAC DOCENTE + titularidad. Rota siempre. Firma: `@idGrupo, @tokenHash, @pin, @idCorrelacion, @idUsuarioEjecutor`. Sin `@forzar`. |
| `usp_rotar_token_matricula_automatico` | Job del servidor | **No tiene parámetro** | Rota solo si `debeRotar = 1` (idempotente). Firma: `@idGrupo, @tokenHash, @pin, @forzar = 0, @idCorrelacion`. Excepción declarada de RBAC humano (§2.1); EXECUTE solo al rol de runtime. |
| `usp_matricular_estudiante_por_token` | Autenticado | **Obligatorio** (NULL → `GEN_002`) | RBAC ESTUDIANTE. Firma: `@idCorrelacion, @tokenHash = NULL, @pin = NULL, @idUsuarioEjecutor`. |
| `usp_registrar_estudiante_por_token_publico` | Público | **No tiene parámetro** | Crea cuenta + matrícula. Firma: `@idCorrelacion, @tokenHash = NULL, @pin = NULL, @idTipoIdentificacion, @numeroIdentificacion, @primerNombre, @segundoNombre, @primerApellido, @segundoApellido, @correo, @password`. Resultset igual al de §2.5. |

- La lógica compartida vive en `usp_internal_rotar_token_matricula` y `usp_internal_matricular_por_token` (internos, **sin EXECUTE** para el runtime). El modo se decide por un parámetro interno distinto de `@idUsuarioEjecutor`.
- Resultsets sin cambios: rotar manual y automático devuelven el de §2.3b; matricular autenticado y registro público devuelven el de §2.5.
- Permisos (§2.6 de seguridad): EXECUTE para el rol de runtime sobre **8** SP públicos: abrir, cerrar, rotar, rotar automático, consultar por rotar, validar, matricular y registro público.
- HTTP (§3): el use case de `POST /public/matricula-qr/registro` llama a `usp_registrar_estudiante_por_token_publico`; el de `POST /estudiante/matricular-grupo`, a `usp_matricular_estudiante_por_token`; el job (§3.1c), a `usp_rotar_token_matricula_automatico`; `POST .../rotacion`, a `usp_rotar_token_matricula_grupo`.

---

## 10. Addendum A3 (2026-11-04) — realtime existente y parámetros del backend

Corrige dos supuestos del borrador al leer el código del backend. **Reemplaza a §2.6 (parámetros de App Configuration), §5 (Realtime) y a las menciones de `publishToUser` de A1/A2 en lo que las contradice.**

1. **No se crea `publishToUser`.** `/api/v1/realtime/stream` ya es por grupo: exige `grupoId`, valida `InstitutionalScopePort.canDocenteAccessGrupo` al suscribirse (solo el docente titular) y `LocalSseRealtimeStreamGateway` entrega únicamente eventos cuyo `payload.grupo` coincide. Por eso un evento con `payload.grupo` ya llega **solo al docente titular de ese grupo**; no hay broadcast global (el supuesto del PLAN §2 / R6 era incorrecto). Los eventos usan la clave **`grupo`** (no `idGrupo`):
   - `MATRICULA_QR_ROTADO`: `{ grupo, pin, urlMatricula, qrPngBase64, fechaRotacionUtc, fechaExpiracionUtc }`.
   - `ESTUDIANTE_MATRICULADO`: `{ grupo, origen: "QR" }` (sin PII).
   - Sin cambios en `RealtimePublisherPort` ni en `ReactorRealtimeAdapter`. El frontend se suscribe con `grupoId`.
2. **Parámetros del backend en el catálogo de parámetros** (`ParameterCatalogPort`, grupo/clave como `REGISTRO/DOMINIO_CORREO_ESTUDIANTE`), no en llaves `asistencias:*` de App Configuration: `RED/CIDRS_CAMPUS`, `RED/PROXIES_CONFIABLES`, `MATRICULA_QR/HABILITADA`, `MATRICULA_QR/LIMITE_IP_POR_MINUTO` (20), `MATRICULA_QR/LIMITE_REGISTROS_TOKEN_POR_MINUTO` (5) y `MATRICULA_QR/URL_BASE_FRONTEND`. Se siembran en `29_parametros_matricula_qr.sql`; la semilla de `RED/CIDRS_CAMPUS` solo trae loopback de desarrollo.
3. Tests T-43 a T-45 del TEST_PLAN dejan de aplicar. Se sustituyen por: un docente no titular no puede suscribirse al grupo (ya cubierto por `LocalSseRealtimeStreamGatewayTest`) y el payload de ambos eventos lleva `grupo` y nunca `tokenHash`.

---

## 11. Addendum A4 (2026-11-04) — tipos de documento en la validación pública

`GET /api/v1/tipos-identificacion` exige JWT, pero el formulario público de alta (§3.4) necesita el catálogo para elegir el tipo de documento. Para no abrir otra ruta pública, la respuesta de `POST /public/matricula-qr/validar` (§3.3) agrega el campo **`tiposIdentificacion`**: `[{ id, tipoIdentificacion, nombre }]`, tomado del mismo caso de uso del catálogo. Es un catálogo estático sin datos personales. `CursoMatriculaQrResponse` queda: `{ idGrupo, nombreAsignatura, nombreGrupo, nombreDocente, fechaExpiracionUtc, cupoDisponible, tiposIdentificacion }`.
