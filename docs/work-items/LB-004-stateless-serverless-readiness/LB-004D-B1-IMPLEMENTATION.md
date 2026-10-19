---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-19
---

# LB-004D — B1: vínculo durable soporte ↔ reclamo (backend, JPA)

```text
CHANGE_CLASS: CONTRACT_EXTENSION (consumidor del contrato DB ONE_TABLE)
DB_CONTRACT: ArchivoSoporte + usp_registrar_archivo_soporte / usp_radicar_solicitud_revision_asistencia_v2 /
             usp_consultar_archivo_soporte_autorizado / usp_consultar_solicitudes_revision_docente
             (repo DB, rama codex/lb-004d-d1-archivo-soporte)
LB004D_CONTRACT_APPROVED: NO   (la revisión del owner DB — John — sigue pendiente; no se inventa aprobación)
MERGE: NO  (este PR depende de la aprobación y del merge del PR DB)
REVIEW_BINDING: IMPLEMENTADO EN RAMA, validado contra la base local principal (no certificado en CI hasta el PR DB)
DOCENTE_FILE_ACCESS: ALLOW solo al docente titular (SQL), DENY_BY_DEFAULT para cualquier otro actor
```

## Qué cambia

| Capa | Cambio |
|---|---|
| Application / puertos | Nuevo `SoporteArchivoMetadataPort` (`registrarBorrador`, `consultarAutorizado`) con excepciones neutrales `Rechazada` (la DB respondió, nada escrito) y `NoDisponible` (resultado desconocido). Sin bucket/objectKey/URL. |
| Subida | `SubirArchivoUseCaseImpl`: validación → ClamAV fail-closed → compresión/SHA-256 → MinIO → **registro DRAFT en SQL**. Rechazo definitivo ⇒ `FileStoragePort.delete` best-effort; resultado desconocido ⇒ un reintento idempotente y **sin** borrar bytes. Nombre > 250 caracteres ⇒ 400 (H-05). |
| Descarga | `DescargarArchivoUseCaseImpl`: **R1 en SQL antes de leer MinIO**; sin fila ⇒ 404 sin tocar el storage; SHA-256 y longitud de SQL son la autoridad de integridad; fila sin objeto ⇒ incidente 5xx, no 404. `owner-subject` de MinIO ya no concede ni deniega. |
| Radicación | DTO/dominio/repositorio reciben `soporteArchivoId` (opcional). El adapter JPA invoca `usp_radicar_solicitud_revision_asistencia_v2`. `soporteNombre`/`soporteUrl` del request se aceptan por compatibilidad de forma y **se ignoran** (sin doble confianza). |
| Reclamos del docente | `GET /api/v1/docente/reclamos?estado&pagina&tamano` deja de ser `FeatureUnavailable`: feature `docente/consultarreclamos`, puerto `SolicitudRevisionDocenteQueryPort`, adapter `SolicitudRevisionDocenteJpaRepository` (R2, paginación en SQL, titularidad por `Docente.usuario`). |
| Errores DB | `ARC_001` → 404 idéntico para soporte inexistente/ajeno/usado/huérfano; `ARC_002` → 409; `ARC_003` → 400; `ARC_005` → 403. |
| Infraestructura | `SoporteArchivoJpaRepository`, `SolicitudRevisionDocenteJpaRepository` y dos entidades de resultado de SP (`SpSoporteArchivoAutorizadoEntity`, `SpSolicitudRevisionDocenteEntity`) que leen `DATETIME2(7)` con `LocalDateTimeJdbcType` (sin zona de la JVM). |

## UTC v2 (instantes)

- Los `*Utc` de SQL son `DATETIME2(7)` UTC; el adapter los convierte a `Instant` con `ZoneOffset.UTC`, sin pasar por `Timestamp`.
  `SoporteArchivoSqlIT.el_instante_utc_leido_no_depende_de_la_zona_horaria_del_host` recorre `America/Bogota`,
  `Europe/London`, `Europe/Berlin`, `Asia/Kolkata` y `UTC` y verifica una hora inexistente en Londres
  (`2026-03-29T01:30:00.1234567Z`) con siete decimales intactos.
- La hora de la sesión solo se expone como instante (`fechaSesionUtc`) cuando la DB conoce su procedencia
  (`UTC_V2`/`UTC_GENERADOR`/`UTC_OWNER`); un dato histórico (`NULL`) llega como `null`. `fechaSolicitud` sigue siendo `DATE`.
- El JSON serializa `Instant` en ISO‑8601 con `Z`.

## Contrato HTTP

| Endpoint | Request | Respuesta |
|---|---|---|
| `POST /api/v1/archivos/subir` (ESTUDIANTE) | multipart `archivo` (PDF/PNG/JPEG ≤ 5 MiB) | `201 {fileId,nombre,url,tamanio}`; 400 malware/formato/tamaño/nombre; 5xx fail-closed |
| `POST /api/v1/asistencias/revisiones` (ESTUDIANTE) | `{sesionId,categoria,justificacion,soporteArchivoId?}` | `202`; `404` soporte no disponible (idéntico en los tres casos); `403` estudiante ≠ ejecutor |
| `GET /api/v1/archivos/{fileId}` | Bearer | `200` bytes (propietario o docente titular); `404` en cualquier otro caso |
| `GET /api/v1/docente/reclamos` (DOCENTE) | `estado`(P/A/R o nombre), `pagina`, `tamano`(1..100) | `ApiListResponse<ReclamoDocenteDTO>` + `total` |

`/api/v1/archivos/**` y `/asistencias/revisiones` siguen fuera del OpenAPI Golden Path (ver `HTTP_AS_IS_MATRIX`); no se modificó el YAML canónico.

## Validación (2026-10-19, base local principal `gestionasistenciadb`)

| Comprobación | Resultado |
|---|---|
| `./mvnw verify` (JUnit + ArchUnit + gates JaCoCo del `pom.xml`) | PASS — 1616 pruebas, 0 fallos, `BUILD SUCCESS` |
| ITs nuevos/cambiados contra SQL, MinIO y ClamAV reales (`SoporteArchivoSqlIT`, `SqlStoredProcedureContractIT`, `ArchivoUploadDownloadFlowIT`) | PASS — 44/44 |
| Suite de integración completa (`-Pintegration verify`) | 229 pruebas: 228 PASS, **1 FAIL** `AzureCloudIntegrationIT` (requiere recursos reales de Azure inexistentes en este entorno; no relacionado con LB-004D) |
| ArchUnit | PASS (capas, aislamiento JPA, aislamiento del provider de storage, sin JDBC productivo) |
| SonarCloud / CodeQL / Trivy / Docker build | NOT_RUN localmente — corren en el PR |
| E2E HTTP con Keycloak + SQL + MinIO + ClamAV | ver `LB-004D-E2E-REPORT.md` |

## Pendiente / bloqueos

- Aprobación del owner DB del contrato (ADR‑LB004D‑001) y merge del PR DB antes de integrar este PR.
- Q5 (segunda solicitud sobre la misma asistencia / plazo de 5 días hábiles), Q6 (reconciliación `DRAFT → ORPHAN`, ventana
  y identidad técnica) y Q9 (reclamos del estudiante) siguen abiertas: no implementadas.
- Los huérfanos MinIO (fila ausente) y los `DRAFT` abandonados siguen sin proceso de reconciliación (diferido, Q6).
