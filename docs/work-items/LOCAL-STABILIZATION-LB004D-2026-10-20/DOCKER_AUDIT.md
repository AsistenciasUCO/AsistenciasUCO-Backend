# DOCKER AUDIT — LOCAL-STABILIZATION-LB004D-2026-10-20

Fecha: 2026-10-20. Evidencia sanitizada; no se registran secretos, tokens ni valores `.env`.

## Postura observada

| Servicio | Estado final observado | Exposición / volumen | Dictamen |
|---|---|---|---|
| MinIO primario `asistencias-files-minio-1` | `running`, `healthy` | `127.0.0.1:19000`, `asistencias-minio-data` | Stack primario conservado y operativo |
| ClamAV primario `asistencias-files-clamav-1` | `running`, `healthy` | `127.0.0.1:13310`, `asistencias-clamav-data` | EICAR detectado y archivo benigno limpio; fail-closed sigue requerido |
| MinIO legacy `minio_asistencias` | detenido por esta auditoría | originalmente `0.0.0.0:9000`, `asistenciasuco_minio_data` | Duplicado detenido; no se borró nada por falta de binding DB |
| ClamAV legacy `clamav_asistencias` | histórico `exited(137)` | `asistenciasuco_clamav_data` | Se conserva para revisión; no se eliminó volumen |
| SQL `sql_server_asistencias` | running | `0.0.0.0:1433`, `sql_server_asistencias_data` | Quality gate local PASS; no se recreó la base |
| Keycloak oficial | running; readiness y realm HTTP 200 | `0.0.0.0:8081`, `0.0.0.0:9001` | Operativo local; exposición no-loopback queda como hallazgo de hardening |
| Redis/security lab | detenido | volúmenes preservados | No es provider requerido por el runtime observado; no se borró |

## Preservación de datos

- SQL Server: `ArchivoSoporte` y `SolicitudRevisionAsistencia` tenían 0 filas al momento de la consulta read-only.
- MinIO primario: inventario observado de 8 objetos y 5,376,421 bytes.
- MinIO legacy: inventario previamente observado de 52 objetos y 2,404 bytes; no se asumió que fueran descartables.
- Volúmenes `asistencias-minio-data`, `asistencias-clamav-data`, `asistenciasuco_minio_data`, `asistenciasuco_clamav_data` y `sql_server_asistencias_data` siguen presentes.

## Hallazgos de seguridad/operación

- `SECURITY_FINDING`: MinIO legacy y Keycloak/SQL tienen bindings host no limitados a loopback; requiere decisión de hardening del owner de infraestructura.
- `REVIEW_REQUIRED`: dos familias MinIO no pueden reconciliarse automáticamente porque no existe binding SQL actual observable; no se borran objetos ni volúmenes.
- `REVIEW_REQUIRED`: ClamAV legacy terminó con exit 137; el primario se recuperó healthy tras reintento, sin atribuir causa permanente sin evidencia adicional.
- `SECURITY_FINDING`: cualquier credencial real encontrada en `.env`/histórico se reporta solo por archivo/tipo; no se copia ni se imprime su valor.

## Acciones ejecutadas

- Se iniciaron temporalmente los dos MinIO existentes para inventario read-only.
- Se detuvo solo `minio_asistencias`, que esta auditoría había iniciado.
- Se reintentó `asistencias-files-clamav-1`; quedó `healthy`.
- No se ejecutaron `docker system prune`, `docker volume prune`, `docker compose down -v`, ni eliminaciones de objetos.
