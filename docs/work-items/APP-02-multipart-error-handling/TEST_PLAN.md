---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-02
---

# TEST_PLAN — APP-02: errores en subida multipart

## Requisito y contrato congelados

Fuente: petición explícita del usuario y hallazgo APP-02/F-04 en `SEC-BASELINE-019/VALIDATION.md` y `PLAN-CORRECCIONES-POR-ROL.md`.

- JSON u otro media type no soportado en `POST /api/v1/archivos/subir` → 415 `INVALID_REQUEST`, `ApiErrorResponse` JSON.
- Multipart malformado → 400 `INVALID_REQUEST`, `ApiErrorResponse` JSON y mensaje genérico.
- Falta la parte `archivo` → 400 `VALIDATION_ERROR`, detalle `archivo` / `FIELD_REQUIRED`.
- Mantener 400 para archivo vacío, formato/tipo inválido y tamaño excedido; mantener 5xx sanitizado para fallos técnicos reales.
- Multipart válido conserva 201, campo `archivo`, ownership y recorrido de seguridad/storage.

## Matriz conductual

| ID | Requirement | Scenario | Input/State | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|
| APP02-01 | Tipo de contenido explícito | JSON en lugar de multipart | `application/json`, `{}` | status, content type, code y llamada al input port | 415, JSON `ApiErrorResponse`, `INVALID_REQUEST`, cero invocaciones | devolver 500 o aceptar/deserializar como archivo | Controller/HTTP |
| APP02-02 | Campo multipart requerido | Multipart válido sin `archivo` | `multipart/form-data`, cero partes | status, code, detalle y llamada al input port | 400, `VALIDATION_ERROR`, `archivo/FIELD_REQUIRED`, cero invocaciones | respuesta 500 o avanzar al caso de uso | Controller/HTTP |
| APP02-03 | Parser malformado es error de cliente | `MultipartException` en la ruta de manejo global | excepción contiene texto interno centinela | status, envelope y filtración | 400 `INVALID_REQUEST`; centinela ausente | handler genérico 500 o mensaje interno expuesto | Error HTTP |
| APP02-04 | Compatibilidad con validación existente | archivo vacío o tipo no admitido | bytes/formato no permitido | rechazo | 400 `VALIDATION_ERROR` | devolver éxito o cambiar el código de validación existente | Application |
| APP02-05 | Compatibilidad con límite existente | supera 5 MiB | `MaxUploadSizeExceededException` | status/código/mensaje seguro | 400 `VALIDATION_ERROR`, mensaje de límite, no 500 | handler genérico multipart devuelve status incorrecto | Controller/HTTP |
| APP02-06 | Provider falla cerrado | ClamAV/almacenamiento falla | excepción técnica | status/código y ausencia de internals | 5xx técnico sanitizado | convertir falla real en 4xx o filtrar detalles | Controller/HTTP |
| APP02-07 | Éxito no cambia | subida multipart válida | PDF permitido | status/body/llamada | 201 y representación existente | romper consumer o omitir escaneo/ownership | Controller + manual Angular |

## Pruebas existentes reutilizadas

- `ArchivoControllerErrorHttpTest`: tamaño excedido y error técnico sanitizado.
- `ArchivoControllerTest`: respuesta exitosa del controller.
- `ContentSecurityValidatorTest`: archivo vacío, extensión/MIME/magic bytes y límites.
- `SubirArchivoUseCaseImplTest`: orden y efectos del flujo de subida.
- `ArchivoUploadDownloadFlowIT`: recorrido HTTP/provider real disponible bajo perfil integration cuando el entorno controlado esté listo; no se ejecuta para demostrar nuevamente F-04.

## Comandos

- RED/GREEN focalizado: `./mvnw -B -ntp "-Dtest=ArchivoControllerErrorHttpTest,ArchivoControllerTest,ContentSecurityValidatorTest,SubirArchivoUseCaseImplTest" test` (Windows, desde la raíz: `.\mvnw.cmd -B -ntp "-Dtest=ArchivoControllerErrorHttpTest,ArchivoControllerTest,ContentSecurityValidatorTest,SubirArchivoUseCaseImplTest" test`).
- Regresión de cierre: `./mvnw -B -ntp verify` (Windows, desde la raíz: `.\mvnw.cmd -B -ntp verify`).
- Manual: frontend Angular con login del usuario; casos no mutantes de entrada y, si el usuario confirma el archivo válido, observar la carga desde el flujo existente. La carga crea un soporte DRAFT en SQL/MinIO; no enviar la reclamación, sembrar datos directamente ni borrar objetos.

## RED_SNAPSHOT

- Base commit: `3216406543ebdb439d41b7fa4852dfb5ef8ace71`.
- Archivo de test: `src/test/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/archivo/ArchivoControllerErrorHttpTest.java`.
- SHA-256 del estado RED: `B18559BD96E81B9C0FE4258F82AC20F22D46C76030CACD0C25524A714895E85D`.
- Comando: `.\mvnw.cmd -B -ntp "-Dtest=ArchivoControllerErrorHttpTest,ArchivoControllerTest,ContentSecurityValidatorTest,SubirArchivoUseCaseImplTest" test`; exit code `1`.
- Resultado RED esperado y observado: 48 tests, 3 fallos (APP02-01: esperado 415, recibido 500; APP02-02 y APP02-03: esperado 400, recibidos 500), 0 errores, 0 skips. Las 45 pruebas restantes pasaron. Snapshot tomado antes de cambiar producción.
