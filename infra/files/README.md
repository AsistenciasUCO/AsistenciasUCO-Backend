# Infraestructura de archivos — MinIO + ClamAV

Esta carpeta contiene la infraestructura local de la storage foundation LB-004B.2H. El compose de
la raíz conserva únicamente la responsabilidad del backend.

## Preparación y arranque DEV_ONLY

Copiar `.env.example` a `.env` y reemplazar todos los `CHANGE_ME_*` por valores locales distintos.
No versionar `.env` ni reutilizar las credenciales root como credenciales de aplicación.

```sh
docker compose --env-file infra/files/.env \
  -f infra/files/compose.yaml up -d
```

```sh
docker compose --env-file infra/files/.env \
  -f infra/files/compose.yaml down
```

Los puertos `9000` (MinIO API) y `3310` (clamd) se publican exclusivamente en `127.0.0.1` para que
un backend iniciado desde IntelliJ pueda accederlos. Esta publicación es `DEV_ONLY`. La consola de
MinIO está deshabilitada y no se publica ningún puerto de consola.

En una ejecución completamente containerizada o deployment, el backend debe unirse a la red
privada del runtime y usar nombres internos (`http://minio:9000`, `clamav:3310`); los bloques
`ports` DEV_ONLY no se incluyen en el manifiesto de deployment. MinIO/ClamAV no se publican al host
ni a ingress. Este compose local no es el manifiesto de producción.

### Puertos ocupados por otro MinIO/ClamAV

Si `9000`/`3310` ya están en uso (por ejemplo, contenedores heredados de un compose anterior), no
los detengas ni borres para liberar el puerto: define en el `.env`, por ejemplo,
`MINIO_HOST_PORT=19000` y `CLAMAV_HOST_PORT=13310` y apunta el backend a esos puertos. El archivo
`.env` puede vivir fuera del repositorio (p. ej. `%USERPROFILE%\.asistencias-files\files.env`) y
pasarse con `--env-file`.

### Variables del backend (sin valores en el repositorio)

| Variable | Uso |
|---|---|
| `APP_ADAPTERS_STORAGE_PROVIDER` | `minio` (único valor soportado) |
| `MINIO_ENDPOINT` | p. ej. `http://127.0.0.1:9000` (o el puerto de `MINIO_HOST_PORT`) |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | identidad técnica de aplicación, **nunca** las credenciales root |
| `MINIO_BUCKET` | `asistencias-soportes` |
| `MINIO_CONNECT_TIMEOUT_MILLIS` / `MINIO_WRITE_TIMEOUT_MILLIS` / `MINIO_READ_TIMEOUT_MILLIS` | opcionales; defaults 5000/30000/30000. Sin timeouts el SDK espera hasta 5 min |
| `CLAMAV_HOST` / `CLAMAV_PORT` | p. ej. `127.0.0.1` y el puerto de `CLAMAV_HOST_PORT` |
| `CLAMAV_CONNECT_TIMEOUT_MILLIS` / `CLAMAV_READ_TIMEOUT_MILLIS` | opcionales; defaults 5000/15000 |

Los IT reales (`MinioFileStorageAdapterIT`, `ClamAvMalwareScanAdapterIT`,
`ArchivoUploadDownloadFlowIT`) exigen `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`,
`CLAMAV_HOST` y `CLAMAV_PORT`; no tienen valores por defecto y fallan si faltan.

```sh
./mvnw -B -ntp -Pintegration \
  -Dit.test=MinioFileStorageAdapterIT,ClamAvMalwareScanAdapterIT,ArchivoUploadDownloadFlowIT \
  -Dtest=NoSuchUnitTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true verify
```

### E2E HTTP real (backend + Keycloak + MinIO + ClamAV)

`scripts/e2e/files/files_storage_e2e.py` (Python stdlib) provisiona identidades dedicadas en un
Keycloak **local** y ejecuta la matriz HTTP. Guarda las credenciales generadas en el archivo de
secretos indicado (fuera del repo) y nunca imprime valores.

```sh
python -I scripts/e2e/files/files_storage_e2e.py provision-users --keycloak-env infra/keycloak/.env --secrets <files.env>
python -I scripts/e2e/files/files_storage_e2e.py main --keycloak-env infra/keycloak/.env --secrets <files.env> --state <state.json>
# reiniciar el backend (o el contenedor minio) y luego:
python -I scripts/e2e/files/files_storage_e2e.py after-restart --keycloak-env infra/keycloak/.env --secrets <files.env> --state <state.json>
# con el contenedor clamav detenido / con minio detenido:
python -I scripts/e2e/files/files_storage_e2e.py clamav-down --keycloak-env infra/keycloak/.env --secrets <files.env>
python -I scripts/e2e/files/files_storage_e2e.py minio-down --keycloak-env infra/keycloak/.env --secrets <files.env> --state <state.json>
```

Exit code `0` = todos PASS, `1` = algún FAIL, `2` = ambiente incompleto (`BLOCKED`).

### Verificar objetos guardados

Solo con la identidad root, dentro de la red privada del compose (el usuario de aplicación no puede
listar el bucket por diseño):

```sh
docker compose --env-file <files.env> -f infra/files/compose.yaml run --rm --no-deps \
  --entrypoint sh minio-init -c 'export MC_CONFIG_DIR=/tmp/.mc-inspect; \
  mc alias set b http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null; \
  mc ls b/asistencias-soportes/soportes/; mc stat b/asistencias-soportes/soportes/<fileId>'
```

`mc stat` muestra la metadata técnica (`X-Amz-Meta-Checksum-Sha256`, `-Compressed`,
`-Original-Size`, `-Owner-Subject`, `-Uploaded-At`). No compartir capturas con `owner-subject` real.

### Recuperación

- Contenedor caído: `docker compose ... up -d` (los volúmenes `asistencias-minio-data` y
  `asistencias-clamav-data` conservan objetos y firmas).
- Credencial de aplicación rotada o policy alterada: volver a ejecutar `minio-init`
  (`docker compose ... up -d minio-init`), es idempotente.
- ClamAV sin firmas tras un volumen nuevo: esperar al healthcheck (`start_period` 90 s); mientras
  tanto las subidas fallan cerrado (`500`), nunca se aceptan sin análisis.
- **Nunca** usar `docker compose down -v` ni borrar volúmenes para "arreglar" errores: borra los
  soportes. Respaldo puntual: `mc mirror` con la identidad root hacia un destino controlado.

## Identidades y policy

- `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`: bootstrap y administración; solo llegan a `minio`,
  `minio-init` y al perfil explícito de validación.
- `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY`: usuario técnico de aplicación creado/verificado por
  `minio-init`; son las únicas credenciales que recibe Spring Boot. El bootstrap falla si usuario
  o secreto de aplicación coinciden con los equivalentes root.
- `asistencias-backend`: permite `s3:GetObject`, `s3:PutObject` y `s3:DeleteObject` únicamente sobre
  `arn:aws:s3:::asistencias-soportes/soportes/*`. `HeadObject` usa la autorización `GetObject` del
  protocolo S3. Además permite `s3:GetBucketLocation` exclusivamente sobre
  `arn:aws:s3:::asistencias-soportes`: el SDK MinIO Java ejecuta esa consulta antes del
  `PutObject`; sin ella la integración real responde `AccessDenied`. No permite crear/eliminar
  buckets, administrar usuarios/policies ni acceder a otro bucket/prefix.

El bucket `asistencias-soportes` se crea y mantiene privado en `minio-init`. El backend no crea
buckets en runtime, no entrega presigned URLs y no recibe credenciales root.

## Validación real de policy

Con los servicios levantados:

```sh
docker compose --env-file infra/files/.env \
  -f infra/files/compose.yaml --profile validation run --rm minio-policy-test
```

El comando exige Put/Get/Delete en el prefix permitido y exige `DENIED` para create-bucket, otro
bucket, escritura fuera del prefix `soportes/`, listado del bucket, cambio de policy anónima, API de
administración, anonymous GET y anonymous PUT; además verifica que la policy anónima del bucket sea
`private`. No sustituye los IT Java de MinIO/ClamAV/HTTP.

## Imagen MinIO

`infra/files/minio/Dockerfile` compila MinIO Community desde el tag upstream verificable
`RELEASE.2025-10-15T17-29-55Z` (commit corto publicado `9e49d5e`) y `mc` desde
`RELEASE.2025-08-13T08-35-41Z` (commit corto publicado `7394ce0`), usando Go `1.24.8`. No usa
`:latest`, `bitnamilegacy` ni una distribución comercial silenciosa.

MinIO Community es AGPL-3.0 y upstream quedó archivado/read-only en 2026. Por ello esta imagen se
clasifica `LOCAL_DEV_TEMPORARY`: es source-built y reproducible por tag, pero no se declara target
productivo mantenido ni con SLA. La selección de un target productivo mantenido y la revisión de
obligaciones AGPL permanecen abiertas antes de deployment.
