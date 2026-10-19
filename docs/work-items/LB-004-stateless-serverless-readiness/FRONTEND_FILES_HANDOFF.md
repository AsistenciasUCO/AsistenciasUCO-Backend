---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-18
---

# LB-004 — Handoff Frontend: subida y descarga de soportes

Destinatario: `shesho22/AsistenciasUCO-Frontend`. Fase F1 del
[plan de reanudación](RESUMPTION_CONTRACT_PLAN_2026-10-08.md). **No se modificó el frontend.** La
asociación del archivo al radicar queda bloqueada por el contrato DB
([DB_BINDING_HANDOFF](DB_BINDING_HANDOFF.md)).

Revisión de solo lectura: rama local `codex/reconcile-frontend-2026-10-18` (`0366fc5`), cuyo
contenido fue fusionado en `develop` (`d13398f`, PR #1).

## AS-IS del consumidor

| Archivo | Estado |
|---|---|
| `core/services/attendance-claim.service.ts` `subirSoporte` | `FormData` campo `archivo`, `POST /archivos/subir` por `HttpClient` (Bearer vía interceptor). Tipado `{ fileId, nombre, url, tamanio }`. **Compatible.** |
| `core/services/attendance-claim.service.ts` `descargarSoporte` | `GET /archivos/{fileId}` con `responseType: 'blob'`. **Compatible.** Sin `window.open`. |
| `student-courses.component.ts` `onFileSelected` | Guarda `fileId` en `archivoAdjunto`; sin pre-validación de tamaño/tipo; error genérico. |
| `student-claim-form.component.ts` | Texto "Máx. 10MB": **incorrecto**, el contrato es 5 MiB. |
| `attendance-claim.service.ts` `crearReclamo` | Envía `soporteNombre` y `soporteUrl` (= `urlSimulada`). El backend no debe confiar en ellos; falta `soporteArchivoId` (pendiente de contrato DB). |
| `teacher-claims.component.ts` `descargarSoporte` | Usa Blob + `URL.createObjectURL`. Hoy recibirá `404` para cualquier docente: es la postura `DENY_BY_DEFAULT` vigente, no un defecto. |
| `mock-backend.interceptor.ts` | Mock devuelve `url` con nombre de archivo; desalineado con la `url` real `/api/v1/archivos/{fileId}`. |

## Contrato backend verificado por HTTP real (2026-10-18)

| Caso | Respuesta |
|---|---|
| Subida válida (PDF/PNG/JPEG, ≤ 5 242 880 bytes) | `201`, `datos: { fileId, nombre, url, tamanio }`; `url` relativa `/api/v1/archivos/{fileId}` |
| Sin Bearer / Bearer inválido | `401` |
| Archivo vacío, extensión no permitida, MIME declarado incoherente, magic bytes falsos, nombre con ruta | `400 VALIDATION_ERROR` |
| Archivo > 5 MiB | `400 VALIDATION_ERROR` (antes de este cambio era `500`) |
| Malware detectado por ClamAV | `400 VALIDATION_ERROR` |
| ClamAV no disponible / timeout | `500 INTERNAL_APPLICATION_ERROR` (fail-closed: el archivo no se guarda) |
| MinIO no disponible | `500 INTERNAL_ERROR` |
| Descarga del propietario | `200`, bytes idénticos, `Content-Type` verificado, `Content-Disposition: inline; filename="<sanitizado>"`, `X-Content-Type-Options: nosniff`, `Cache-Control: no-cache, no-store, ...` |
| Descarga de archivo ajeno, inexistente o `fileId` malformado | `404 RESOURCE_NOT_FOUND`, indistinguibles |

## Cambios recomendados para F1 (sin duplicar servicios)

1. Reutilizar `AttendanceClaimService.subirSoporte/descargarSoporte`; no crear servicios nuevos.
2. Validar en cliente `file.size <= 5 * 1024 * 1024` y extensión `pdf|png|jpg|jpeg` antes de subir
   (UX); el backend sigue siendo la autoridad. Corregir el texto "Máx. 10MB" → "Máx. 5 MiB".
3. Manejo de errores por status/código: `400` → mostrar `message` del backend (incluye
   rechazo por malware sin detalles de firma); `401` → flujo de sesión; `404` → "soporte no
   disponible"; `5xx` → "servicio de archivos no disponible, intente más tarde" (no reintentar en
   bucle).
4. Mantener el `fileId` en el estado de la solicitud (`archivoAdjunto.fileId`); no usar `url`/nombre
   como identidad.
5. Al radicar: enviar `soporteArchivoId = fileId` **cuando el contrato DB/backend B1 esté aprobado**.
   Hasta entonces el reclamo puede radicarse sin soporte vinculado (`SUPPORT_OPTIONAL: YES`).
6. Descarga docente: mantener el botón detrás de un estado "pendiente de habilitación" o tratar el
   `404` con un mensaje neutro hasta que exista `DOCENTE_READ_RELATED` en runtime.
7. No usar `window.open`, `<a href>` directo ni token en query string para descargas autenticadas.
   Revocar el object URL después de que el navegador inicie la descarga (p. ej. `setTimeout`).
8. Si la UI necesita el filename desde la cabecera, el backend debe exponer `Content-Disposition` en
   CORS (`exposedHeaders` hoy solo incluye `X-Correlation-Id`): requiere decisión backend; mientras
   tanto usar `nombre` de la respuesta de subida.
9. Alinear el mock (`mock-backend.interceptor.ts`) con `url = /api/v1/archivos/{fileId}`.

## Pruebas esperadas en F1

Unitarias de servicio (FormData, `responseType: 'blob'`), validación de 5 MiB, mapeo de errores
400/401/404/5xx, y E2E manual con dos identidades (propietario descarga; otro estudiante recibe 404).
