---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-18
---

# LB-004 — Handoff al equipo DB: vínculo durable soporte ↔ solicitud de revisión

Destinatario: owner de `johnjduque/gestion-asistencia-db`. Este documento expresa **necesidades
del consumidor backend**; no es un diseño SQL aprobado, no propone tablas definitivas y no modifica
stored procedures congelados. La decisión de persistencia es del equipo DB
([RESUMPTION_CONTRACT_PLAN](RESUMPTION_CONTRACT_PLAN_2026-10-08.md), fase D0/D1).

```text
REVIEW_BINDING: BLOCKED_BY_DB_CONTRACT
DB_DECISION_OWNER: johnjduque/gestion-asistencia-db
BACKEND_STORAGE_FOUNDATION: READY (ver LB-004C-EXECUTION_REPORT.md)
DOCENTE_FILE_ACCESS: DENY_BY_DEFAULT hasta que exista la relación durable
```

## 1. AS-IS verificado en el repositorio DB (solo lectura, 2026-10-18)

Fuente: head del PR #37 (`codex/reconcile-db-2026-10-18`, SHA
`034e6e8acc0f6ffcf820a17e2115a0fac8906b80`, abierto, sin merge, base `develop`
`e1ca79d80d28f53dd06f3cfa13fa1ec6b4835295`). No se modificó el PR.

| Objeto | Hallazgo |
|---|---|
| `schema/tables/SolicitudRevisionAsistencia.sql` | Columnas `id, nombre, asistencia, fecha, estado, justificacionSolicitud, justificacionRespuesta`. **Sin referencia al soporte.** |
| `usp_radicar_solicitud_revision_asistencia` | Recibe `@soporteNombre NVARCHAR(250)` y `@soporteUrl NVARCHAR(500)`, los normaliza con `TRIM`, pero el `INSERT` **no los persiste**. Valida perfil `ESTUDIANTE`, estudiante, sesión y pertenencia al grupo de la sesión. |
| `usp_resolver_solicitud_revision_asistencia` | Resuelve `SolicitudRevisionAsistencia → Asistencia → Sesion → Grupo.docente` y valida docente titular. No ofrece lectura por `fileId`. |
| `uv_solicitud_revision_asistencia` / `uv_auth_solicitud_revision_asistencia` | Lectura con filtro de autorización encadenado (`uv_auth_asistencia → uv_auth_sesion → uv_auth_grupo`). Sin columna de soporte. |

Conclusión: subir un archivo a MinIO **no** certifica que quede vinculado a un reclamo ni que el
docente titular pueda descargarlo. Ese vínculo no existe hoy en SQL.

## 2. Qué ya resuelve el backend (independiente de DB)

- `fileId`: UUID v4 opaco generado por backend en `SubirArchivoUseCaseImpl`. Nunca derivado del
  filename ni de una URL del cliente.
- Bytes: MinIO privado `asistencias-soportes`, clave interna `soportes/<fileId>` (detalle exclusivo
  de Infrastructure; **no** debe persistirse en SQL como identidad pública ni exponerse al frontend).
- Metadata técnica temporal en el objeto MinIO: `owner-subject` (idUsuario del JWT),
  `original-filename` sanitizado, `Content-Type` verificado, `original-size`, `compressed`,
  `compression-algorithm`, `checksum-sha256`, `uploaded-at` (UTC ISO-8601).
- Ownership técnico `DRAFT`: solo el `owner-subject` descarga; cualquier otro actor recibe `404`
  indistinguible de inexistente (verificado por HTTP real con dos estudiantes, docente, coordinador
  y administrador).

## 3. Relación que el consumidor necesita poder resolver

```text
fileId (UUID, backend)
  -> soporte de una SolicitudRevisionAsistencia
       -> Asistencia
            -> Sesion
                 -> Grupo
                      -> docente titular (Grupo.docente)
```

Lecturas requeridas, todas por SP/vista aprobada y con contrato de salida canónico:

| Capacidad | Entrada | Debe responder |
|---|---|---|
| R1 — leer metadata de soporte por `fileId` para el propietario | `fileId`, `idUsuarioEjecutor` | metadata durable + estado si el ejecutor es el estudiante dueño; vacío si no |
| R2 — leer soporte de una revisión para el docente titular | `fileId`, `idUsuarioEjecutor` | metadata si el ejecutor es el docente titular del grupo de la sesión de la asistencia de la revisión; vacío si no (sin distinguir "no existe" de "no autorizado") |
| R3 — soportes candidatos a reconciliación | estado, ventana operacional (parámetro, sin plazo inventado) | lista `fileId` en `DRAFT`/`ORPHAN`/`REPLACED` |

Escritura requerida:

| Capacidad | Entrada | Reglas |
|---|---|---|
| W1 — registrar `DRAFT` | `fileId`, owner (idUsuario institucional del JWT), filename sanitizado, contentType verificado, tamaños, checksum SHA-256, `createdAtUtc`, `idCorrelacion` | `fileId` único; owner nunca proviene del request |
| W2 — adjuntar al radicar | `idSolicitud` (o radicación en la misma transacción), `soporteArchivoId` (= `fileId`), `idUsuarioEjecutor`, `idCorrelacion` | solo si el `fileId` es `DRAFT`, pertenece al estudiante ejecutor y la revisión es suya; `ATTACHED` no se reutiliza; atómico con la radicación |
| W3 — marcar `ORPHAN`/`REPLACED` | `fileId`, motivo, `idCorrelacion` | idempotente; nunca sobre `ATTACHED` sin contrato de reemplazo |

Todo SP debe conservar los cuatro campos canónicos de salida: `idCorrelacion`,
`mensajeUsuarioResultado`, `mensajeTecnicoResultado`, `estadoResultado`.

## 4. Estados del archivo (ya congelados en OWNERSHIP_DECISION)

`DRAFT` → `ATTACHED` (una sola vez) · `DRAFT` → `ORPHAN` (ventana superada / sin attach) ·
`ATTACHED` → `REPLACED` solo con contrato futuro (`POST_ATTACH_REPLACEMENT: OUT_OF_SCOPE`). Las
fechas de transición deben ser UTC.

## 5. Transacciones, idempotencia, compensación y reconciliación

- MinIO **no** participa en la transacción SQL. Orden propuesto por el consumidor (a validar por DB):
  1. backend sube bytes a MinIO (objeto sin fila SQL = huérfano técnico si falla lo siguiente);
  2. W1 registra `DRAFT` (si falla, backend invoca `FileStoragePort.delete` como compensación
     best-effort y el reconciliador cubre el resto);
  3. W2 adjunta dentro de la transacción de radicación (rollback SQL deja el archivo en `DRAFT`).
- Idempotencia: `idCorrelacion` por operación; W1/W2 repetidos con la misma correlación no deben
  duplicar filas ni cambiar de dueño.
- Reconciliación: proceso durable (no `@Scheduled` local por réplica) que compara R3 con el storage;
  borrar solo con estado durable, nunca por edad del objeto en MinIO. Evidencia real: existe en el
  bucket local un objeto del 2026-09-30 sin fila SQL (huérfano técnico de una validación previa),
  exactamente el caso que R3 debe cubrir.
- Compatibilidad: `@soporteNombre/@soporteUrl` siguen en la firma de `usp_radicar`; plan de
  deprecación coordinado hacia `soporteArchivoId` (decisión DB + frontend), sin doble confianza.

## 6. Firma esperada por Backend JPA (necesidad del consumidor, no diseño SQL)

Backend añadirá un puerto neutral en Application (nombre tentativo `SoporteArchivoMetadataPort`) con
adapters JPA en Infrastructure. Necesita, como mínimo, poder mapear:

```text
registrarBorrador(fileId UUID, ownerIdUsuario UUID, nombre, contentType, originalSize, storedSize,
                  checksumSha256, createdAtUtc, idCorrelacion) -> resultado canónico (4 campos)
adjuntarARevision(fileId UUID, idSolicitud UUID, idUsuarioEjecutor UUID, idCorrelacion) -> resultado canónico
consultarParaPropietario(fileId UUID, idUsuarioEjecutor UUID) -> Optional<metadata + estado>
consultarParaDocenteTitular(fileId UUID, idUsuarioEjecutor UUID) -> Optional<metadata + estado>
listarCandidatosReconciliacion(estado, ventana) -> List<fileId>
```

Tipos: `UNIQUEIDENTIFIER` para ids, `NVARCHAR` acotado para nombre/contentType, `BIGINT` para
tamaños, `CHAR(64)` o equivalente para SHA-256 hex, `DATETIME2` UTC.

## 7. Pruebas DB que el consumidor espera antes de B1

RED/GREEN en el repo DB: `fileId` único; attach propio permitido; attach ajeno/duplicado/`ATTACHED`
rechazado; rollback de radicación no deja filas huérfanas; R2 permite al titular y no revela a otro
docente (0/N filas); R1 preserva al estudiante dueño; idempotencia por correlación; nueva freeze
manifest sin alterar la baseline anterior.

## 8. Lo que el backend NO hará hasta tener el contrato

- No crear tablas, columnas, SP ni migraciones.
- No habilitar descarga docente (sigue `DENY_BY_DEFAULT`, 404).
- No persistir `objectKey`/bucket en SQL desde backend ni confiar en `soporteUrl` del cliente.
