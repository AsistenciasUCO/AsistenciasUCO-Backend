---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-18
---

# RISKS — LB-004A / LB-004B.0 / LB-004B.1

| ID | Riesgo | Evidencia | Impacto | Tratamiento requerido |
|---|---|---|---|---|
| R-LB004-001 | pérdida de bytes al reemplazar instancia | `ArchivoController` escribe disco local | alto | Blob privado + test restart/multi-instancia |
| R-LB004-002 | IDOR / falta de object-level authorization | `/archivos/**` solo exige autenticación | alto | ownership en Application, metadata durable y negativos 401/403/404 |
| R-LB004-003 | referencia de revisión rota | upload y attach separados; DB acepta URL cliente | alto | `fileId`, estados DRAFT/ATTACHED y compensación/reconciliación |
| R-LB004-004 | metadata insuficiente | solo `soporteNombre`/`soporteUrl` | alto | contrato DB aprobado para owner/state/binding; sin inventar schema |
| R-LB004-005 | rollback nominal | local no puede leer objetos Blob nuevos | alto | release compatible, migración verificada y rollback sin selector ciego |
| R-LB004-006 | descarga Angular sin Bearer | `window.open(url)` fuera de `HttpClient` | alto | frontend usa HttpClient Blob; no hacer público el objeto |
| R-LB004-007 | límite visible contradictorio | UI anuncia 10 MB; backend rechaza >5 MiB | medio | decisión contractual única y pruebas frontend/backend |
| R-LB004-008 | acceso administrativo inventado | no existe consumer de coordinador/admin para soportes | alto privacidad | `PRODUCT_DECISION_REQUIRED`; deny by default |
| R-LB004-009 | docente sin relación accede | UI pretende docente titular; listado real no implementado | alto | query/scope durable y test de docente relacionado/no relacionado |
| R-LB004-010 | objetos huérfanos | upload ocurre al seleccionar; cancelar/remover no llama delete | medio/alto | draft TTL aprobado + reconciliador idempotente desde estado durable |
| R-LB004-011 | tipo malicioso / inline | validación por extensión y respuesta inline | medio/alto | política magic bytes/scanning/content disposition antes de implementación |
| R-LB004-012 | recurso Azure supuesto | Azure existe para otras capabilities, Blob no está evidenciado | alto operacional | aprobación, provisioning LB-006, permisos/SLO y Cloud Integration real |
| R-LB004-013 | provider internals filtrados | consumer acepta URL absoluta y hoy persiste URL | alto acoplamiento/seguridad | backend-mediated URL; object key y Blob URL no son contrato |
| R-LB004-014 | migración incompleta | checkout local vacío no prueba ambientes vacíos | alto | inventario sanitizado, hash/tamaño, ventana read-only y stop condition |
| R-LB004-015 | retención/borrado incorrectos | no hay requisito legal/producto | alto | `PRODUCT_DECISION_REQUIRED`; no hard delete de adjuntos hasta decisión |
| R-LB004-016 | divergencia de caches por réplica | caches locales e invalidación por réplica | medio | freshness/test; `NO_DISTRIBUTED_CACHE_REQUIRED` |
| R-LB004-017 | logs locales perdidos | JSONL relativo al proceso | medio operacional | LB-006; no mezclar con storage funcional |
| R-LB004-018 | scope creep realtime/cloud | SSE local y Azure ya presentes | alto para fase | LB-005/LB-006 separados; no Redis/IaC/SDK en B.0 |

## Riesgos resueltos o reclasificados

- Consumer inexistente: resuelto por snapshot frontend limpio `b0c2225`.
- Provider especulativo: la aprobación histórica de Azure Blob en LB-004B.1 quedó `SUPERSEDED`;
  MinIO es el provider vigente por `D-LB004B2-001`.
- URL pública/directa: rechazada; backend mediation seleccionada.
- Rollback `provider=local`: rechazado; estrategia staged definida.
- `R-LB004-002` (IDOR): tratamiento congelado en `OWNERSHIP_DECISION.md` (404 ajeno, ownership por identidad institucional); implementación pendiente de `DR-LB004-DB-002`.
- `R-LB004-006` (descarga sin Bearer): tratamiento congelado en `CONSUMER_MATRIX.md §FRONTEND CONTRACT DELTA` (`HttpClient`/Blob); implementación frontend pendiente, prompt independiente.
- `R-LB004-007` (límite contradictorio): resuelto por decisión `D-LB004B1-003`, `MAX_FILE_SIZE_FINAL: 5 MiB`.
- `R-LB004-008` (acceso administrativo inventado): resuelto por `OUT_OF_SCOPE`/`DENY_BY_DEFAULT` explícito en `OWNERSHIP_DECISION.md`.
- `R-LB004-009` (docente ajeno): regla congelada (404); implementación pendiente de la capability de consulta que resuelve `DR-LB004-DB-002`.
- `R-LB004-011` (tipo malicioso/inline): política de content security congelada (`D-LB004B1-004`);
  scanning de malware `SUPERSEDED` de `DEFERRED` a `REQUIRED` en LB-004B.2 (`D-LB004B2-001`, ClamAV).
- `R-LB004-012` (recurso Azure supuesto): `SUPERSEDED` — ya no aplica un recurso Blob que provisionar;
  MinIO corre local vía `infra/files/compose.yaml`, sin dependencia de aprobación/provisioning cloud.
- `R-LB004-013` (provider internals filtrados): resuelto por contrato `HTTP_CONTRACT_TARGET.md` (`fileId`/URL relativa, nunca provider URL/objectKey/bucket/access key).

## Riesgo LB-004B.1 (reclasificado en B.2)

| ID | Riesgo | Evidencia | Impacto | Tratamiento requerido |
|---|---|---|---|---|
| R-LB004-019 | Ambigüedad de shape DB bloquea `REVIEW_BINDING` (attach/lifecycle DB-dependiente, docente-read) | `DB_BASELINE_CONTRACT.md` no documenta el objeto que respalda `soporteNombre`/`soporteUrl`; `GET /docente/reclamos` no tiene SP funcional; confirmado además por inspección read-only del repo DB (SP `usp_radicar_solicitud_revision_asistencia` recibe `@soporteNombre`/`@soporteUrl` pero no los persiste; tabla `SolicitudRevisionAsistencia` no tiene columna de soporte) | medio (ya **no** bloquea la storage foundation; sigue bloqueando `REVIEW_BINDING`, aislado explícitamente por `D-LB004B2-001`) | Resolver `DR-LB004-DB-002` en una microfase de contrato DB independiente antes de `FASE 2` de `E2E_PLAN.md` |

## Riesgos nuevos LB-004B.2

| ID | Riesgo | Evidencia | Impacto | Tratamiento requerido |
|---|---|---|---|---|
| R-LB004-020 | Dependencia local de Docker (MinIO + ClamAV) no disponible en algún entorno de validación | Integration tests reales requieren contenedores levantados; repo no tenía antes ningún IT contra infraestructura Docker-only (SQL Server IT usa instancia externa, no contenedor) | medio (bloquea solo IT real, no unit/RED) | IT falla con mensaje claro si Docker/MinIO/ClamAV no están disponibles, nunca se salta silenciosamente ni se presenta como PASS |
| R-LB004-021 | Falso negativo de malware si ClamAV no está listo (firmas aún no cargadas) | ClamAV requiere base de firmas cargada antes de aceptar `INSTREAM` | alto | healthcheck de ClamAV en compose antes de aceptar tráfico; `ERROR`/`NOT_READY` del scanner es `FAIL CLOSED`, nunca `CLEAN` por defecto |
| R-LB004-022 | Malware real usado en pruebas | Instrucción explícita prohíbe usar malware real | crítico si se violara | Únicamente firma EICAR (`MALWARE-001`); ningún fixture de test contiene payload malicioso real |
| R-LB004-023 | Testcontainers no es una convención existente en este repo (grep confirma cero usos) | Introducir un patrón de IT nuevo sin precedente | bajo/medio (riesgo de inconsistencia, no de seguridad) | IT reales (`MinioFileStorageAdapterIT`, `ClamAvMalwareScanAdapterIT`) se ejecutan contra servicios provistos por `infra/files/compose.yaml` (mismo patrón que SQL Server IT: infraestructura externa ya corriendo), no contra Testcontainers embebido |
| R-LB004-025 | `clamav/clamav:1.5.4-debian13-slim` no detecta la firma EICAR si va precedida por otros bytes (verificado empíricamente con `clamdscan` dentro del contenedor: `%PDF-` + EICAR → `OK`; `ABCDE` + EICAR → `OK`; EICAR pura → `FOUND`). Un PDF/PNG/JPEG real exige su magic byte en offset 0, por lo que no existe un fixture "archivo válido + EICAR" honesto para este motor | No afecta la detección de malware real (limitación específica de la firma de prueba EICAR en este build); sí limita qué se puede certificar end-to-end con datos de prueba seguros | `MALWARE-001` se certifica en el límite real `ClamAvMalwareScanAdapterIT` (EICAR pura, `FOUND` confirmado) y la orquestación fail-closed en `SubirArchivoUseCaseImplTest` (`MalwareScanPort` simulado); `ArchivoUploadDownloadFlowIT` no repite ese caso con un fixture PDF+EICAR que el motor real no detectaría, para no presentar un falso negativo como si fuera cobertura real |
| R-LB004-024 | MinIO Community pasó a distribución source-only y el repositorio upstream quedó archivado/read-only en 2026; no existe una imagen community productiva mantenida que pueda declararse target con soporte/SLA | alto para producción; no bloquea validación local controlada | Se elimina `bitnamilegacy`. `infra/files/minio/Dockerfile` compila source upstream fijado a `RELEASE.2025-10-15T17-29-55Z` y `mc` fijado, sin `:latest` ni licencia comercial silenciosa. Estado: `LOCAL_DEV_TEMPORARY`; mantener riesgo abierto hasta decidir un target productivo mantenido y revisar obligaciones AGPL-3.0 |
| R-LB004-025-UPDATE (2026-10-18) | Revisión LB-004C: el motor sí detecta EICAR dentro de un **stream de un PDF estructurado** (objeto `/EmbeddedFile`): `Eicar-Signature FOUND`. Solo `%PDF-`+EICAR plano sigue sin detectarse | Habilita un fixture seguro "PDF válido + EICAR" que pasa la validación de contenido y llega al scanner | `MITIGATED`: `ArchivoUploadDownloadFlowIT.pdf_valido_con_eicar_embebido_es_rechazado_por_el_clamav_real`, `ClamAvMalwareScanAdapterIT.pdf_estructurado_con_eicar_embebido_es_detectado_como_infectado` y E2E HTTP `MALWARE_EICAR_IN_PDF_400`. La limitación del EICAR plano tras magic bytes permanece documentada |
| R-LB004-026 | Contenedores heredados `minio_asistencias` (bitnamilegacy) y `clamav_asistencias` del compose raíz antiguo publican `0.0.0.0:9000` y `0.0.0.0:3310` en la estación local | `docker ps` 2026-10-18 | medio/alto (exposición LAN de un MinIO no gobernado por LB-004). `SECURITY_FINDING` (tipo: credencial versionada; valor no reproducido): las credenciales de ese MinIO en el `.env` raíz local coinciden con los defaults que los IT de `develop` tenían hardcodeados, por lo que están en el historial público del repositorio | No se tocaron (no autorizado). LB-004C elimina esos defaults del árbol (`FilesIntegrationEnvironment`). Recomendación al owner: detener los contenedores heredados y rotar/descartar esas credenciales; el stack vigente `asistencias-files` publica solo en `127.0.0.1` y usa credenciales generadas fuera del repo |
| R-LB004-027 | La descarga lee el objeto completo (≤ 5 MiB) antes de evaluar ownership: un actor autenticado ajeno provoca lectura de storage aunque reciba `404` | Logs E2E: descargas ajenas registran `storage download success` previo al 404 | bajo (sin fuga de datos; amplificación de carga acotada por 5 MiB) | Futuro: operación de metadata (`stat`) previa a `read` en `FileStoragePort` o autorización por SQL tras el binding DB; no se cambió el puerto en LB-004C |
| R-LB004-028 | Indisponibilidad de storage responde `500 INTERNAL_ERROR` (handler genérico) y la del scanner `500 INTERNAL_APPLICATION_ERROR` | E2E HTTP `MINIO_DOWN_*` vs `CLAMAV_DOWN_*` | bajo (ambos 5xx seguros sin internals) | `DescargarArchivoFailClosedTest` fija la propagación de `StorageUnavailableException`; unificar el código requiere decisión contractual de errores, no se cambió unilateralmente |

## Stop conditions

- Ownership y contrato HTTP conceptual ya están `FROZEN` (LB-004B.1). RED/GREEN de la storage
  foundation quedan `AUTHORIZED` en LB-004B.2 (`D-LB004B2-001`); lo que sigue bloqueado hasta resolver
  `DR-LB004-DB-002` es únicamente `REVIEW_BINDING` (attach/lifecycle DB-dependiente, docente-read).
  Retención permanece `OUT_OF_SCOPE` con tratamiento definido, sin plazo inventado.
- No relajar Bearer ni exponer bucket/objectKey/endpoint/access key/secret MinIO para conservar
  compatibilidad.
- No cambiar DB desde backend ni diseñar SQL por inferencia; no modificar el repositorio DB.
- No modificar frontend ni OpenAPI en LB-004B.2.
- No presentar mock de MinIO/ClamAV, EICAR como malware real, o `mvn verify` sin IT real como prueba
  de storage/malware.
- No iniciar LB-005 (realtime distribuido) ni LB-006 (IaC/cloud); no introducir Redis, RabbitMQ,
  Kafka, CQRS ni outbox.

