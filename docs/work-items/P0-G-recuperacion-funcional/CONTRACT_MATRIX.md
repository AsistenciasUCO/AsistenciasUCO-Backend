# CONTRACT_MATRIX — P0-G

Fecha de observación: 2026-10-20, America/Bogota. Los valores de configuración y secretos no se reproducen.

## Candidatos

| Componente | Commit/ref candidate | Estado |
|---|---|---|
| Backend B1 | `f45a5213` / `jose-valencia/lb-004d-b1-support-binding` | Recuperado, no integrado. |
| Frontend F1 + teacher fix | `95a202a`, con antecedentes `06f70c4`, `fac2f640` | Recuperado, no cambiado de checkout. |
| DB D1/R2 | `b698c76`, base D1 `231be58` | Preservado, aprobación DB pendiente. |
| Infra | `a544c496` / `main` | Línea activa sin cambio propuesto. |

## SQL observado en la instancia local

| Procedimiento | Uso | Parámetros observados | Resultado observado | Consumidor B1 | Decisión |
|---|---|---|---|---|---|
| `usp_registrar_archivo_soporte` | W1 mutation | archivo, metadata, correlación, ejecutor | 4 columnas canónicas | `SoporteArchivoJpaRepository` | Pendiente owner DB |
| `usp_radicar_solicitud_revision_asistencia_v2` | W2 mutation | estudiante, sesión, categoría, justificación, soporte, correlación, ejecutor | 4 columnas canónicas | `AsistenciaJpaRepository` | Pendiente owner DB |
| `usp_consultar_archivo_soporte_autorizado` | R1 query | archivo, correlación, ejecutor | 10 columnas de lectura | `SoporteArchivoJpaRepository` | Confirmar como READ_QUERY_RESULTSET |
| `usp_consultar_solicitudes_revision_docente` | R2 query | ejecutor, correlación, estado, página, tamaño | 25 columnas de lectura + total | `SolicitudRevisionDocenteJpaRepository` | Confirmar como READ_QUERY_RESULTSET |

## Conflicto y decisión requerida

`DB_BASELINE_CONTRACT` fija cuatro columnas para operaciones de mutación. La propuesta D1 clasifica R1/R2 como consultas y documenta resultsets de lectura distintos. Esto no autoriza una implementación nueva ni una modificación SQL: el owner DB debe confirmar si esa clasificación es la fuente contractual vigente, además de la autoridad entre `origin` y `legacy-db`.

## HTTP B1 candidate

- `POST /api/v1/asistencias/revisiones` → W2; `soporteArchivoId` es el vínculo vigente del candidato.
- `GET /api/v1/docente/reclamos` → R2 con ownership/paginación SQL.
- `POST /api/v1/archivos/subir` → W1.
- `GET /api/v1/archivos/{fileId}` → R1 después de autorización de ownership/teacher.

Estos endpoints se documentan como consumidores del candidato existente; no se crean ni cambian endpoints en P0-G.
