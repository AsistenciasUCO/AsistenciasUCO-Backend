---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-18
---

# LB-004C — MinIO + ClamAV: certificación de la storage foundation independiente de DB

## Contexto

- Fecha: 2026-10-18. Autoridad: reanudación por decisión humana explícita ([PAUSE](PAUSE.md),
  condición 2). No aprueba el contrato de persistencia DB.
- Rama: `feat/lb-004-minio-storage-e2e`, creada desde `origin/develop`
  `27262769f469462d0f7e313702a9949371765269` (HEAD remoto verificado con `git ls-remote`).
  Worktree aislado: el checkout principal tenía cambios sin commitear de otro work item (MAINT-003L)
  que no se tocaron.
- Java 25 (`mvnw.cmd`), Docker Desktop, SQL Server 2022 existente (sin crear otra DB), Keycloak
  local `127.0.0.1:8081`.
- Clase de cambio: `HARDENING + VALIDATION`. Sin cambio de OpenAPI canónico, DB, frontend, roles ni
  rutas HTTP.

## Estado inicial (auditoría)

| Componente | Estado inicial |
|---|---|
| `FileStoragePort` (store/read/exists/delete, neutral) | IMPLEMENTED_AND_VERIFIED (B.2H) |
| `MalwareScanPort` + `ClamAvMalwareScanAdapter` (INSTREAM, fail-closed) | IMPLEMENTED_AND_VERIFIED |
| `MinioFileStorageAdapter` (bucket privado, metadata técnica, lectura acotada) | IMPLEMENTED_AND_VERIFIED |
| Timeouts del cliente MinIO | **MISSING** (SDK espera hasta 5 min) |
| Subida/descarga, ownership técnico estudiante, checksum, compresión | IMPLEMENTED_AND_VERIFIED (unit + IT sin Bearer) |
| Rechazo HTTP de multipart > 5 MiB | **PARTIAL** — `500 INTERNAL_ERROR` en vez de rechazo de cliente |
| IT reales | PARTIAL — credenciales por defecto hardcodeadas y default `localhost:9000` (en esta estación apunta a un MinIO heredado) |
| Malware en flujo HTTP | PARTIAL — R-LB004-025: no había fixture "PDF válido + EICAR" |
| E2E HTTP con Bearer real | MISSING |
| Persistencia tras reinicio real del backend | IMPLEMENTED_NOT_VERIFIED (solo "restart simulado") |
| Policy de mínimo privilegio | IMPLEMENTED_AND_VERIFIED (sin probar listado/escritura fuera de prefijo/admin) |
| Vínculo archivo ↔ revisión, descarga docente | BLOCKED_BY_DB_CONTRACT |

Componentes reutilizados sin reescribir: puertos, use cases, `ContentSecurityValidator`,
`CompressionPolicy`, `ChecksumCalculator`, ambos adapters, wiring, controller, `infra/files/*`.

## Entorno local

- `docker ps -a`: existían `minio_asistencias` (bitnamilegacy) y `clamav_asistencias` del compose raíz
  antiguo, publicados en `0.0.0.0:9000`/`0.0.0.0:3310` (R-LB004-026). **No se detuvieron ni
  borraron.** El stack vigente `infra/files/compose.yaml` se levantó en `127.0.0.1:19000` y
  `127.0.0.1:13310` mediante `MINIO_HOST_PORT`/`CLAMAV_HOST_PORT`, reutilizando la imagen y los
  volúmenes existentes `asistencias-minio-data`/`asistencias-clamav-data`.
- Credenciales root/aplicación generadas aleatoriamente en `%USERPROFILE%\.asistencias-files\files.env`
  (fuera del repo, nunca impresas). `docker compose ... config --quiet`: OK.
- `minio-init`: exit 0 (bucket privado, usuario técnico distinto de root, policy).
- ClamAV `1.5.4`, firmas `daily 28157` del 2026-10-18, `freshclamd` activo, healthcheck `healthy`.

## Errores encontrados y correcciones

| # | Hallazgo | RED (causal) | GREEN |
|---|---|---|---|
| 1 | `MinioClient` sin timeouts: un MinIO que acepta TCP y no responde bloquea la request hasta 5 min | `MinioClientTimeoutBoundaryTest` (2 casos): `execution timed out after 10000 ms` | `MinioProviderProperties` gana `connect/write/read-timeout-millis` (defaults 5000/30000/30000, `≤0` → default); `MinioStorageAdapterConfiguration` aplica `setTimeout`. Ahora falla cerrado en ~0,7 s |
| 2 | Multipart > 5 MiB → `MaxUploadSizeExceededException` caía en el handler genérico → `500 INTERNAL_ERROR` | `ArchivoControllerErrorHttpTest.multipart_que_excede_el_limite_retorna_400_controlado_y_no_500`: `Status expected:<400> but was:<500>` | `GlobalExceptionHandler.handleMaxUploadSizeExceeded` → `400 VALIDATION_ERROR` "El archivo excede el tamano maximo permitido (5 MiB)." (mismo rechazo que `ContentSecurityValidator`) |
| 3 | IT con `MINIO_ACCESS_KEY/SECRET_KEY` por defecto hardcodeados y endpoint por defecto ambiguo | Ejecutado sin variables: ahora 10/10 FAIL con "defina MINIO_ENDPOINT" | `testsupport.files.FilesIntegrationEnvironment`: variables obligatorias sin defaults |
| 4 | R-LB004-025 sin cobertura HTTP de malware | Sonda real: `%PDF-`+EICAR → `OK`; PDF con `/EmbeddedFile` EICAR → `Eicar-Signature FOUND` | `SoporteFixtures.pdfWithEmbeddedEicar()`; casos en IT y E2E |
| 5 | Policy sin probar denegaciones adicionales | — | `validate-policy.sh`: escritura fuera de `soportes/`, listado, cambio de policy anónima, API admin → `DENIED`; policy anónima `private` |

Comandos RED/GREEN:

```text
RED:   .\mvnw.cmd -B -ntp -q -Dtest=FileProvidersConfigurationTest,MinioClientTimeoutBoundaryTest,ArchivoControllerErrorHttpTest -Djacoco.skip=true test
       EXIT_CODE=1  Tests run: 11, Failures: 3 (2 timeouts >10 s, 1 status 500)
GREEN: .\mvnw.cmd -B -ntp -Dtest=FileProvidersConfigurationTest,MinioClientTimeoutBoundaryTest,ArchivoControllerErrorHttpTest,GlobalExceptionHandler*Test,ApiErrorCatalogTest -Djacoco.skip=true test
       EXIT_CODE=0  Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
```

## Evidencia con proveedores reales

### IT (MinIO + ClamAV reales, sin mocks)

```text
COMANDO: .\mvnw.cmd -B -ntp -Pintegration -Dit.test=MinioFileStorageAdapterIT,ClamAvMalwareScanAdapterIT,ArchivoUploadDownloadFlowIT -Dtest=NoSuchUnitTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true verify
EXIT_CODE: 0
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
MinioFileStorageAdapterIT: 10/10   (6 previos + metadata comprimida, sin comprimir, credenciales inválidas, endpoint caído)
ClamAvMalwareScanAdapterIT: 7/7    (3 previos + PDF con EICAR embebido, 5 MiB limpio, timeout real, protocolo no-clamd)
ArchivoUploadDownloadFlowIT: 9/9   (2 previos + EICAR→400, compresión real, PNG/JPEG, 5 MiB, 5 MiB+1, ownership, contrato público)
```

Este IT inyecta la identidad con un resolver de prueba: **no** certifica JWT; eso lo hace el E2E.

### Policy real

```text
COMANDO: docker compose --env-file <files.env> -f infra/files/compose.yaml --profile validation run --rm --no-deps minio-policy-test
EXIT_CODE: 0
MINIO_APP_PUT_GET_DELETE_TARGET_BUCKET: PASS
MINIO_APP_CREATE_BUCKET / ACCESS_OTHER_BUCKET / WRITE_OUTSIDE_PREFIX / LIST_BUCKET / SET_BUCKET_POLICY / ADMIN_API: DENIED
MINIO_BUCKET_ANONYMOUS_POLICY: PRIVATE
MINIO_ANONYMOUS_GET / MINIO_ANONYMOUS_PUT: DENIED
```

### E2E HTTP real (backend jar + Keycloak + MinIO + ClamAV + SQL Server existente)

Backend: jar de esta rama, `.env` raíz (SQL/Keycloak) + overrides de archivos. Identidades:
docente y coordinador de `infra/keycloak/.env`; las cuentas `E2E_ESTUDIANTE`/`E2E_ADMIN` devolvieron
`invalid_grant` (contraseñas del `.env` desalineadas con el Keycloak vivo) y **no se resetearon**; se
provisionaron identidades dedicadas `files.e2e.estudiante1`, `files.e2e.estudiante2`
(`ESTUDIANTE`) y `files.e2e.admin` (`ADMINISTRADOR`) con `scripts/e2e/files/files_storage_e2e.py
provision-users` (secretos solo en el archivo local).

| Fase | Resultado |
|---|---|
| `main` | **27/27 PASS**: 401 sin Bearer/Bearer inválido; subida PDF 201 con exactamente `{fileId,nombre,url,tamanio}` y `url` relativa; descarga del dueño 200 con SHA-256 idéntico, `nosniff`, `no-store`, filename sanitizado; estudiante 2/docente/coordinador/admin → 404 idéntico a inexistente; `fileId` malformado 404; EICAR en PDF 400; `.exe` renombrado, MIME incoherente, vacío, `.txt`, path traversal → 400; 5 MiB+1 → 400; 5 MiB exactos, PDF compresible, PNG y JPEG → 201 y descarga idéntica |
| Bucket tras `main` | 5 objetos nuevos = las 5 subidas aceptadas; ningún rechazo dejó objeto. Compresible: 306 486 → 1 250 bytes (`Compressed=true`, `DEFLATE`); 5 MiB: `Compressed=false`, `size=5242880` |
| `after-restart` (kill forzado del backend, JVM nueva PID 40680 → 5276) | **7/7 PASS**: los 5 archivos con SHA-256 idéntico; ajeno sigue 404 |
| `after-restart` tras `docker restart` de MinIO (backend sin reiniciar) | **7/7 PASS** |
| `clamav-down` (contenedor detenido) | **PASS**: `500 INTERNAL_APPLICATION_ERROR`, sin host/puerto; el bucket siguió con el mismo número de objetos |
| `minio-down` (contenedor detenido) | **2/2 PASS**: subida y descarga `500 INTERNAL_ERROR`, sin internals, respuesta inmediata |

Logs del backend y de los IT escaneados contra los valores del archivo de secretos, del `.env`
raíz y de Keycloak: `0` secretos, `0` JWT, `0` object keys, `0` payload EICAR.

### Regresión completa

```text
LÍNEA BASE (develop 2726276): .\mvnw.cmd -B -ntp clean verify → EXIT 0, Tests run: 1572, F0 E0 S0
                              LINE 8436/9292 = 90.79 %, BRANCH 2060/2580 = 79.84 %
ESTA RAMA:                     .\mvnw.cmd -B -ntp clean verify → EXIT 0, Tests run: 1577, F0 E0 S0
                              LINE 8453/9309 = 90.80 %, BRANCH 2066/2586 = 79.89 % (gates 80/70)
ARCHUNIT: 82/82 PASS en 20 clases (incluye StorageProviderIsolationRulesTest 6/6, CleanArchitectureRulesTest 20/20)
```

```text
DOCKER: docker build -t asistencias-uco-backend:lb004c-minio .  → EXIT 0
        (el stage build ejecuta clean verify: Tests run: 1577, F0 E0 S0, BUILD SUCCESS)
```

La cifra 1557 citada en la instrucción era anterior: `develop` ya tenía 1572 por los commits P0
fusionados. Diferencia de esta rama: **+5** unitarios (2 `MinioClientTimeoutBoundaryTest`, 2
`FileProvidersConfigurationTest`, 1 `ArchivoControllerErrorHttpTest`). Los +15 IT nuevos corren
solo en `-Pintegration`. No se deshabilitó ni se relajó ningún test.

## Arquitectura final

```text
ArchivoController (Bearer -> AuthenticatedUserResolver)
  -> SubirArchivoInputPort / DescargarArchivoInputPort
  -> SubirArchivoUseCaseImpl / DescargarArchivoUseCaseImpl
       |-- ContentSecurityValidator (5 MiB, extensión, MIME declarado, magic bytes, nombre)
       |-- MalwareScanPort -> ClamAvMalwareScanAdapter (INSTREAM, fail-closed, timeouts)
       |-- CompressionPolicy (>=10 % ahorro) + ChecksumCalculator (SHA-256)
       |-- FileStoragePort -> MinioFileStorageAdapter (bucket privado, soportes/<fileId>, timeouts)
GlobalExceptionHandler: MaxUploadSizeExceeded -> 400; ValidationException -> 400;
                        ResourceNotFound -> 404; fallos de proveedor -> 500 sin internals
```

Sin endpoint `DELETE`; `delete` sigue siendo capability interna (usada solo por limpieza de IT).

## Operación

Ver [infra/files/README.md](../../../infra/files/README.md): arranque/parada, puertos alternativos,
variables (sin valores), IT, E2E, inspección de objetos con `mc stat` y recuperación.

```sh
docker compose --env-file <files.env> -f infra/files/compose.yaml up -d     # iniciar
docker compose --env-file <files.env> -f infra/files/compose.yaml stop      # detener (conserva datos)
```

Nunca `down -v`. Al cierre de esta intervención los servicios `asistencias-files-*` quedan
corriendo en loopback y el backend E2E detenido.

## Riesgos de seguridad y limitaciones para producción

- R-LB004-024: MinIO Community archivado/AGPL-3.0, imagen source-built `LOCAL_DEV_TEMPORARY`; no
  certificado para producción (sin SLA, sin revisión de obligaciones AGPL, sin TLS, sin KMS/SSE,
  sin versioning/backup gestionado, nodo único).
- `SECURITY_FINDING` (R-LB004-026, tipo credencial versionada; valor no reproducido): el MinIO
  heredado `minio_asistencias` (`0.0.0.0:9000`) usa credenciales iguales a los defaults que los IT
  de `develop` tenían hardcodeados. Esta rama los elimina del árbol; siguen en el historial. Acción
  recomendada al owner: detener ese contenedor y descartar/rotar esas credenciales.
- ClamAV en un solo contenedor sin HA; su caída bloquea subidas (fail-closed, correcto pero visible).
- R-LB004-026 (estación local), R-LB004-027 (lectura previa a ownership), R-LB004-028 (código de
  error de storage) — ver [RISKS](RISKS.md).
- Retención/purga y cold storage siguen `DECISION_REQUIRED` ([STORAGE_LIFECYCLE](STORAGE_LIFECYCLE.md)).
- Existe un objeto huérfano técnico del 2026-09-30 en el volumen local (validación previa): se
  conservó como evidencia para el diseño de reconciliación.

## Pendientes

- DB: [DB_BINDING_HANDOFF](DB_BINDING_HANDOFF.md) (`REVIEW_BINDING: BLOCKED_BY_DB_CONTRACT`; PR DB
  #37 abierto, no modificado).
- Frontend: [FRONTEND_FILES_HANDOFF](FRONTEND_FILES_HANDOFF.md) (sin cambios en el repo frontend).
- CI remoto (Sonar, CodeQL, Trivy): se registran en el PR cuando ejecuten; no se presumen.

## Resultado

```text
MINIO_RUNTIME=PASS
CLAMAV_RUNTIME=PASS
PRIVATE_BUCKET_POLICY=PASS
FILE_UPLOAD=PASS
FILE_DOWNLOAD=PASS
FILE_CHECKSUM=PASS
FILE_OWNERSHIP=PASS (estudiante propietario; docente relacionado DENY_BY_DEFAULT por diseño)
MALWARE_SCAN=PASS
BACKEND_RESTART_PERSISTENCE=PASS
JAVA_TESTS=PASS
ARCHUNIT=PASS
DOCKER_BUILD=PASS
DB_REVIEW_BINDING=BLOCKED
FRONTEND_HANDOFF=READY
MINIO_FOUNDATION_READY=YES
FULL_STORAGE_E2E_READY=NO (falta binding DB, descarga docente y frontend)
LB004=IN_PROGRESS (no DONE)
```
