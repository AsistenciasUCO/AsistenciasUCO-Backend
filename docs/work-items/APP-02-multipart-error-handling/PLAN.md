---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-02
---

# PLAN — APP-02: errores de entrada en subida multipart

## Identidad y objetivo

- Fecha / autor o rol / base Git: 2026-11-02 / backend-team / `3216406543ebdb439d41b7fa4852dfb5ef8ace71`.
- Objetivo: impedir que `POST /api/v1/archivos/subir` con contenido distinto de multipart, multipart malformado o sin el campo `archivo` termine como error HTTP 500 genérico.
- Criterios: solicitud JSON → 415 `ApiErrorResponse`; multipart malformado → 400 `ApiErrorResponse`; parte `archivo` ausente → 400 `ApiErrorResponse` con detalle requerido. Archivo vacío, tamaño excedido y tipo inválido conservan sus rechazos 400. Fallos internos auténticos conservan 5xx sanitizado. Una subida multipart válida conserva ruta, respuesta exitosa, ClamAV y ownership.
- Skills y fuentes: `uco-contratos`, `uco-baseline`, `uco-testing`; `API_DESIGN_RULES.md`, `HTTP_AS_IS_MATRIX.md`, OpenAPI canónico (la ruta está fuera de la especificación Golden Path), `SEC-BASELINE-019/VALIDATION.md` F-04, `PLAN-CORRECCIONES-POR-ROL.md` APP-02, LB-004C, `HTTP_CONTRACT_TARGET.md`, código y tests actuales.
- Autorización: petición explícita del usuario del 2026-11-02 y encargo adjunto. No se extiende a APP-01 ni a otros hallazgos.

## AS-IS y evidencia

| Hecho | Archivo + símbolo / contrato + versión | Evidencia y límites |
|---|---|---|
| Un `POST` a `/api/v1/archivos/subir` con JSON respondió 500 `INTERNAL_ERROR`. | `SEC-BASELINE-019/VALIDATION.md`, F-04, resultado medido del 2026-11-02. | Se reutiliza el resultado documentado; no se repite la sonda original. |
| El controlador recibe `@RequestParam("archivo") MultipartFile`; el mapping no declara `consumes`. | `src/main/.../archivo/ArchivoController.java`, `subirArchivo`. | La ausencia de restricción de tipo de medio deja que solicitudes no multipart lleguen al resolver de argumento. |
| `GlobalExceptionHandler` ya normaliza `MaxUploadSizeExceededException` a 400 `VALIDATION_ERROR`; los errores inesperados quedan en 500. No hay tratamiento explícito para tipo de medio no soportado, parte ausente ni `MultipartException`. | `src/main/.../error/GlobalExceptionHandler.java`; `ApiErrorCatalog.uploadSizeExceeded()`. | El límite de tamaño ya se corrigió en LB-004C y se preserva. |
| Los archivos vacíos y los formatos/extensiones/MIME no permitidos se rechazan por el validador de contenido. | `ContentSecurityValidator`; `ContentSecurityValidatorTest`; `SubirArchivoUseCaseImplTest`. | Se reutilizan las validaciones existentes; no se cambia la política de contenido. |
| El consumidor Angular envía `FormData` en el campo `archivo` con Bearer. | `LB-004/FRONTEND_FILES_HANDOFF.md`. | No se requiere cambio frontend. |
| La subida válida recorre validación, ClamAV y MinIO; metadata/binding DB sigue el estado de LB-004. | `LB-004C-EXECUTION_REPORT.md`, `LB-004/HTTP_CONTRACT_TARGET.md`, matriz HTTP AS-IS. | No se modifica DB ni el flujo de almacenamiento. |

## TARGET

- Restringir el mapping existente al media type multipart publicado por el consumidor. JSON recibe 415 con el código existente `INVALID_REQUEST` y envelope JSON `ApiErrorResponse`.
- Traducir `MultipartException` (sin incluir `MaxUploadSizeExceededException`, que conserva su handler específico) a 400 `INVALID_REQUEST` genérico y sanitizado.
- Traducir la falta de la parte requerida a 400 `VALIDATION_ERROR`, con `details[].field=archivo` y `details[].code=FIELD_REQUIRED`.
- No cambiar el éxito 201, el campo `archivo`, límites, extensiones/MIME, ownership, ClamAV, MinIO, persistencia DB ni los errores técnicos reales.

## Clase de cambio y alcance de rutas

- Change class: `BEHAVIOR_CHANGE`.
- PRIMARY_VARIABLE: `uploadRequestErrorMapping`.
- ALLOWED_PATHS:
  - `src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/archivo/ArchivoController.java`
  - `src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/error/GlobalExceptionHandler.java`
  - `src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/error/ApiErrorCatalog.java`
  - `src/test/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/archivo/ArchivoControllerErrorHttpTest.java`
  - `docs/work-items/APP-02-multipart-error-handling/**`
  - `docs/work-items/README.md`, `docs/baseline/LINEA_BASE.md`, `docs/contracts/HTTP_AS_IS_MATRIX.md` (actualizar tras GREEN).
- FORBIDDEN_PATHS: DB y SQL; frontend; esquema/configuración runtime; `SecurityConfig`; API de almacenamiento/validación; cambios de OpenAPI o endpoints; otros work items; datos/semillas/usuarios persistentes.
- CONTRACTS: HTTP de error `ApiErrorResponse`; éxito existente `201 {fileId,nombre,url,tamanio}` y multipart `archivo` preservados. `400/415` para entradas inválidas están autorizados por el encargo APP-02.
- PROVIDERS: MinIO y ClamAV sin cambios. No se amplía la frontera Application.
- EXTERNAL_ENVIRONMENT: tests de controller no necesitan providers. Validación manual frontend usa servicios locales existentes y requiere login del usuario.
- SECURITY_IMPACT: respuestas sin detalle del parser, librerías, host, puerto o stack trace; RBAC/ownership sin cambios.
- OBSERVABILITY_IMPACT: logging estructurado existente; sin nuevos datos sensibles.
- TEST_LEVEL_REQUIRED: controller/excepción HTTP, validaciones existentes y prueba manual desde Angular; `verify` backend al finalizar.
- ROLLBACK: revertir los cambios de esta rama; preservar servicios y objetos existentes; no requiere migración ni rollback DB.
- CONSUMERS: `StudentCoursesComponent` vía `attendance-claim.service.ts`; `FormData("archivo")` y Bearer confirmados en el handoff frontend.
- STOP_CONDITIONS: si el parser no expone una excepción global normalizable, si el error real del provider se clasifica como entrada inválida, o si el envelope/contrato vigente discrepa, detener ese caso y registrar `CONTRACT_CONFLICT`/`TEST_CONTRACT_CONFLICT`.

## Alcance

Normalizar en el adaptador HTTP las entradas multipart inválidas del endpoint existente y proteger el comportamiento exitoso/error técnico con pruebas de regresión.

## No alcance

APP-01, otros APP/SES, contratos de DB, decisiones de soporte/retención, cambios del formulario frontend, altas de usuario, limpieza de objetos y resolución del binding DB pendiente de LB-004.

## Archivos afectados

- EXISTENTES: controlador de archivos, `GlobalExceptionHandler`, `ApiErrorCatalog`, `ArchivoControllerErrorHttpTest`, matriz HTTP, baseline e índice de work items.
- NUEVOS: `PLAN.md`, `TEST_PLAN.md`, `VALIDATION.md` de este item.
- RETIRAR: ninguno.

## Contratos y consumidores afectados

- HTTP: una entrada JSON anteriormente observada como 500 pasa a 415; multipart malformado pasa a 400 `INVALID_REQUEST` y parte ausente a 400 `VALIDATION_ERROR`. Se conserva el envelope `ApiErrorResponse` y sus códigos existentes. `HTTP_AS_IS_MATRIX.md` registra el comportamiento ya implementado.
- DOMAIN/PERSISTENCE/SECURITY/REALTIME: sin cambios.
- Consumidor Angular actual ya envía multipart `archivo`; la compatibilidad se verifica con la validación desde navegador.

## Riesgos y dependencias

- El resolver multipart de Spring puede lanzar el error antes de entrar al controller; el advice global debe producir JSON aun en esa ruta.
- `MaxUploadSizeExceededException` hereda de multipart; su handler más específico debe mantener 400 `VALIDATION_ERROR` y el límite de 5 MiB.
- Integración manual requiere backend, frontend, identidad local, MinIO y ClamAV. No se requiere SQL para comprobar la traducción de formato.

## Test plan

Ver [TEST_PLAN.md](TEST_PLAN.md). Requisito → contrato → pruebas RED → GREEN → validación.

## Rollback y stop conditions

Rollback mediante reversión de esta rama; no borrar ni recrear servicios o volúmenes. Si una excepción del provider real cae en el nuevo mapeo 4xx, conservarla en 5xx y añadir el caso de regresión antes de cerrar.

## Deuda conocida y validación manual

No se introduce deuda. La carga multipart válida se verificó manualmente desde Angular y queda registrada en `VALIDATION.md`; no se envió la reclamación. La carga exitosa crea el soporte DRAFT por el flujo normal en SQL/MinIO; no se sembraron datos directamente ni se borraron objetos.

## Definition of Ready

**READY (2026-11-02).** El usuario autorizó explícitamente APP-02. La evidencia F-04 y el contrato de multipart/errores están versionados; el target se limita a 400/415 y al envelope existente. El consumer usa `FormData("archivo")`. `develop` personal está en `3216406543ebdb439d41b7fa4852dfb5ef8ace71`; los PR #72 (head `d1347663`) y #73 (head `fe816d9e`) reportan `Backend Quality Gate` y `ArchUnit Architectural Guardrails` PASS en GitHub. El merge de PR #71 agregó el backlog/evidencia APP-02; el alcance de este work item queda limitado a ese hallazgo. El TEST_PLAN está congelado antes de RED. DB, SQL y providers no se alteran.
