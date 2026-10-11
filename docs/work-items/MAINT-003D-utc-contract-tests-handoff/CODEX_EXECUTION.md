---
status: HANDOFF_READY_WITH_EXTERNAL_DEPENDENCIES
work-item: MAINT-003D
---
# Instrucciones exactas para Codex (no pedir que rediseñe)

## Entrada y reconciliación
Trabajar en un worktree nuevo del backend `alejovh12/AsistenciasUCO`. Leer **en este orden**: este directorio DECISIONS, DB_OWNER_HANDOFF, ACCEPTANCE_MATRIX, FRONTEND_HANDOFF; documentación original `MAINT-003`; RED de `maint-003b-utc-v2-red`. Localmente el usuario reportó ramas `maint-003c-utc-v2-red-hardening` (19b5a957...) y `maint-003c-utc-d06-owner-proposal` (32dc74ed...) aún **sin push**. Esta rama nueva deriva de 003b remoto, NO incluye esos commits. Antes de integrar, cotejar diffs y **no sobrescribir** tests ni docs 003c; preferir su RED estricto y resolver duplicados preservando comportamiento. No hacer cherry-pick automático del SQL ilustrativo como migración.

## Secuencia mecánica
1. Verificar rama/head e inventario v1: `SesionController`, `SesionHttpMapper`, `HttpTemporalParser`, `GlobalExceptionHandler`, OpenAPI canonical, `SesionJpaRepository` y proyecciones. Registrar contratos v1 y sus tests sin tocarlos.
2. **Gate DB**: comprobar que owner DB ha publicado NUEVO work item pos-DB-GP-001C y ejecutado SQL real con columna nullable CHECK, 3 SP, 2 vistas, control de DML y clasificación histórica; obtener SHAs + firmas. Si falta, preparar integración en rama aislada pero NO activar v2 o afirmar release.
3. Ajustar `HttpUtcInstantCodec`: regex RFC3339 exacta + `OffsetDateTime/Instant` con resolver estricto, sin trim permisivo, precisión 0..7. Producir errores de `RequestValidationException` por campo en controlador/adaptador v2; no lanzar IllegalArgumentException hasta un handler 500.
4. Implementar controller v2 independiente, usando los **mismos** puertos de aplicación y `AuthenticatedUserResolver`, sin roles/actor del body. POST 201, PATCH 200, GET único y por grupo. Evitar `PUT v2`. Debe tener validación de negocio `SES_004` y respetar ownership. Usar la forma canonical existente de envelopes y HTTP errors.
5. Implementar lectura de procedencia junto a fecha sin falsificarla; proyectar `estadoTemporal` y horas `null` si desconocidas. Mantener v1 DTO/mappeo independientes; v1 modifica marca a NULL desde SP; generador usa su marca.
6. Versionar OpenAPI: schemas `CrearSesionV2Request`, `ActualizarSesionV2Request`, `SesionV2`, security, detalles 400. Pruebas RED de MAINT-003B + MAINT-003C y de esta rama → GREEN sin deshabilitarlas. Revisar `SesionUtcActivationGuardTest`: su protección temporal "ningún v2 mientras D06 no decidido" debe sustituirse **solo tras gate DB/owner** por guard que exija procedencia, nunca borrarse para esconder fallo.
7. Correr JDK25 `./mvnw -B -ntp clean verify` y `./mvnw -B -ntp -Pintegration verify` SQL Server **real** (falla explícita si fixture falta), ArchUnit, JaCoCo, OpenAPI, Sonar, CodeQL, Trivy. Verificar v1 3/3, codec original 7/7, MAINT-003C strict, POST/GET/PATCH v2, 401/403, SQL exacto, timezone DST y rollback.
8. Publicar `VALIDATION.md` con números REALES, skips, SHA de cada repo y del manifiesto owner, comandos, evidencia SANITIZADA. No declarar GREEN ni READY si faltan ejecuciones.
9. Luego y solo luego abrir PR de microfase v2 y publicar contrato de backend. Frontend en PR separado: implementar FRONTEND_HANDOFF y sus tests, a partir del contrato aprobado.

## STOP / no hacer
- No modificar DB shape ni ejecutar SQL de propuesta desde backend.
- No merge de PR18/19/20 ni 003b/003c por defecto.
- No escribir a v1 campos Z; no convertir históricos por inferencia ni convertir `LocalTime` recurrente.
- No cambiar auth/RBAC/ownership, no ocultar 403 con 501, no bajar cobertura ni quitar tests o SKIPs.
- No afirmar que ya hay token Keycloak, DB SQL real, owners firmados o CI verde de v2 sin evidencia.
- Si contrato owner difiere de este diseño, actualizar decisiones y tests en conjunto mediante nueva revisión, nunca adaptar silenciosamente código para que los tests pasen.

## Estado del handoff
`DOCS_READY_TO_REVIEW`; `V2_IMPLEMENTATION_APPROVED=NO` hasta firmas DB, funcional y contratos. `SQL_INTEGRATION_VERIFIED=NO` en esta entrega. La rama es de **docs y pruebas**, no una migración ni implementación.
