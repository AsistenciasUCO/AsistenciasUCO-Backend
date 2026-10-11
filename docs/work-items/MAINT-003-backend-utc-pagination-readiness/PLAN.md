---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# MAINT-003 — backend primero: validación real de paginación y contrato UTC

## Fuente de verdad y situación actual

Base: develop `bfc4fd3fee9fadefbb00fd6f4652ff1dc5423d69`, PR #17 merged `bfc4fd3`. PR #18 paginación y PR #19 caché siguen abiertos (no hacer merge). BD: `johnjduque/gestion-asistencia-db` rama develop, `dbo.uv_estudiante_identidad`, `dbo.uv_estudiante`, `dbo.uv_usuario`, `dbo.Sesion`, `dbo.uv_sesion` y SP `usp_crear_sesion`/`usp_actualizar_sesion`.

La DB declara fechas de sesión como instantes UTC en columnas `DATETIME2` (sin offset): son fechas UTC **por convención**, no porque SQL Server preserve una zona. En v1, HTTP de sesiones acepta texto `LocalDateTime` sin zona y devuelve `LocalDateTime`. No existe información en el wire para reconstruir la zona real con certeza. **NO reinterprete fechas históricas de manera automática.**

## Contrato futuro recomendado (DECISION_REQUIRED)

Para no romper consumidores v1, proponer endpoints **v2 diferenciados** (nombre exacto y autoridad sujetos a aprobación de backend/DB/frontend):
- POST `/api/v2/sesiones` y PATCH `/api/v2/sesiones/{id}` con `fechaHoraInicio`/`fechaHoraFin` ISO-8601 obligatorio con `Z` o `±HH:mm`;
- GET de sesiones v2 devuelve instantes UTC con `Z`, sin formato dependiente de la zona de la JVM ni la base.
- Conversión en adaptador HTTP: `OffsetDateTime.parse` → mismo instante en UTC → `LocalDateTime` UTC hacia SP `DATETIME2`. En GET: `LocalDateTime` UTC → string `Z`. Mantener JPA y el SP 4-columnas sin cambio.
- Los endpoints v1 conservan semántica y contrato hasta migración aprobada. No crear ruta v2 vacía/501 ni generar respuesta engañosa.
- Crear/editar sesión: si el usuario introduce wall-clock local `date` + `time`, el frontend debe obtener offset del navegador para ese día concreto y validar tiempos inexistentes/ambiguos en DST; no `new Date(...)` interpretando ambiguamente cadenas antiguas. El UTC normalizado nunca cambia por la zona de visualización.
- Los horarios recurrentes con `dia` + `LocalTime` **no son instantes** y NO deben convertirse a UTC sin fecha y zona institucional contractual (por determinar).
- Auditoría ya usa `DATETIMEOFFSET +00:00`, contrato separado.

## Alcance de este PR

**READY (pure adapter code/tests/docs)**: añadir `HttpUtcInstantCodec` sin consumidores y siete pruebas de parseo/serialización. Source change es opt-in/inactivo, no sustituye `HttpTemporalParser`. Sin efectos sobre API, base ni despliegue. No necesita SQL Server para certificar el codec, sí Java 25 CI.

**NOT_READY (cambio de wire HTTP o migración de datos)**: faltan contrato aprobado, inventario de consumidores, decisión datos históricos, estrategia de compatibilidad y validación SQL real. **Nunca** conectar el codec al controller v1 antes de esas aprobaciones. Después de contract signoff abrir micro-PR v2 con endpoint/test OpenAPI, integración SQL, autenticación y documentación de rollback.

**Se prohíbe en esta fase:** DDL / migraciones, modificar SP, convertir `DATETIME2` a `DATETIMEOFFSET` unilateralmente, ajustes Hibernate globales que muevan horas, retocar JSP/frontend, ocultar 401/403, fusionar PR #18/#19 automáticamente.

## Orden estricto backend → frontend

1. Certificar PR #18 con SQL Server real aislado, test `CoreViewQueriesJpaParityIT` ejecutado **sin skips**, fixture mínimo >=11 estudiantes y uno multigrupo, orden estable `e.id`, filtros y totalElements/totalPages. No crear registros de prueba en producción. Guardar evidencia de paginación 0/1/2, pageSize=5, tamaño 100, 400 inválidos y RBAC 401/403.
2. Certificar PR #19 con SQL Server real `CatalogJpaParityIT` y pruebas de dos claves distintas / caché; observar SQL y logs, no exponer secretos. No confundir unit con IT.
3. Validar contract-first UTC: baseline de registros históricos de `dbo.Sesion` + criterio humano de si realmente son UTC; si son legacy locales, no cambiar automáticamente. Firmar contrato v2. Implementar controlador/DTO v2 y SP reuse en PR separado.
4. Ejecutar contra backend+SQL Server reales crear→consultar→actualizar→consultar con offset `Z`, Bogotá, Berlín verano/invierno, Londres verano/invierno y DST (solapamiento y gap). Verificar bytes SQL UTC, respuesta con Z, no double conversion. Probar roles y ownership.
5. Java 25 `./mvnw -B -ntp clean verify`, `-Pintegration` con provider real, ArchUnit, OpenAPI, JaCoCo, Sonar, CodeQL, Trivy; conservar skips=0 para tests relevantes. No afirmar todo PASS sin resultados.
6. Publicar **backend release contract** versionado, endpoints aprobados, respuestas ejemplo, seguridad/roles, timezone semantics, evidencia y fecha de merge. Solo entonces desbloquear el paquete frontend.

## Condiciones de parada / rollback

- Contradicción entre valores persistidos y supuesto UTC.
- Fixture ausente: BLOCKED, nunca skip interpretado como PASS.
- Frontend consulta datos que endpoint no devuelve (programa, semestre, estado de matrícula, etc.): NO inventar esos atributos.
- Un cambio rompe roles/ownership, SSE, horarios recurrentes o Golden Path.
- Rollback por revert del PR de codec; migraciones SQL pendientes requieren estrategia autorizada distinta.

## Estado al preparar

* Paginación PR #18: 3 GitHub Actions PASS (CI unit, Trivy/ArchUnit, CodeQL/Sonar), SQL real aún sin evidencia.
* Caché PR #19: 3 GitHub Actions PASS, SQL real aún sin evidencia.
* Codec nuevo: source preparado, JDK25 y tests aún por correr en CI de este PR.
* Activación UTC wire v2: CONTRACT_DECISION_REQUIRED (NO IMPLEMENTADA). No se declara backend completamente certificado.

## 2026-10-09 — hardening del códec inactivo UTC-D02

El códec opt-in ahora exige forma `yyyy-MM-ddTHH:mm:ss[.1..7]Z|±HH:mm`, sin whitespace, offset abreviado, segundos en offset ni precisión >7 decimales; se rechaza `-00:00` porque es offset desconocido RFC3339. Después de normalizar el instante comprueba rango SQL Server `datetime2(7)` y nanosegundos divisibles por 100, sin truncamiento/rounding silencioso. Pruebas nuevas para entradas inválidas, límites y roundtrip exacto. **No conecta API v2**, no cambia el wire v1, la DB o la política de históricos. UTC-D06 y la decisión v2 de código HTTP quedan pendientes. Exigir Java25 CI del nuevo SHA y pruebas SQL reales después de aprobación del owner DB.
