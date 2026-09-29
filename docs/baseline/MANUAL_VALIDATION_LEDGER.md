---
status: active
type: ledger
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# Ledger de validaciones manuales

Único registro de validaciones externas/manuales pendientes. Ninguna fila afirma ejecución sin evidencia. Cuando se automatice, marcar AUTOMATIZADA y enlazar el test y su corrida; cuando pase, conservar evidencia sanitizada y fecha.

| ID | Fecha de registro | Escenario | Motivo/ambiente requerido | Evidencia y condición de cierre | Responsable por rol | Estado |
|---|---|---|---|---|---|---|
| MV-001 | 2026-09-20 | Golden Path frontend + Keycloak + SQL Server + SSE | Validación externa reportada en la autorización LB-001C.1; artefacto/log externo no está en este checkout | PASS reportado: Bearer, F5, horarios/grupos/sesiones/estudiantes, consulta+lote, AN/SJC/EX, SSE entre navegadores, offline→online y reconciliación HTTP; fixture temporal retirado. Límite: no hay enlace/log reproducible local, ver [ENTRY_EVIDENCE](../work-items/LB-001C-openapi-contract-first/ENTRY_EVIDENCE.md) | backend-team + frontend/DB | PASS_REPORTED_EXTERNAL (2026-09-24) |
| MV-002 | 2026-09-20 | Provisioning y roles con IdP real | Fake HTTP no certifica Keycloak real | DB-first, UUID/correo canónicos, roles, compensación interna y fallo tras commit; no exponer secretos | backend-team + identidad | PENDIENTE |
| MV-003 | 2026-09-20 | Logs/metrics/traces y Azure runtime del ambiente | Config/adapters y mocks no demuestran operación cloud | Key Vault real; parámetros y mensajes de App Configuration reales; Event Grid; invalidación y refresh observable de caches; correlationId/traceId/spanId y métricas aplicables; cero exposición de secretos; ambiente/fecha y evidencia sanitizada. Evidencia parcial LB-001D.2 (2026-09-26, `-Pazure-integration`, solo lectura vía Ports): Key Vault, parámetros y mensajes reales PASS; no cubre Event Grid real, invalidación/refresh, observabilidad completa ni evidencia operacional | backend-team + operaciones | PENDIENTE |
| MV-004 | 2026-09-29 | CI remoto y protección de ramas | Validar Ruleset y required checks sobre el commit/PR actual | PASS: ruleset `Protect develop` activo; PR requerido; deletion/non-fast-forward bloqueados; sin bypass; required checks `Backend Quality Gate`, `CodeQL Java Analysis`, `Dependency Review`, `SonarCloud Code Analysis`. PR #14 sobre SHA `e92afb73221ac3a967e63c673312d3961cfb0688`: los cuatro checks PASS; Backend CI #44 SUCCESS; Backend Security #44 SUCCESS; check adicional `CodeQL` PASS. SonarCloud Quality Gate PASS, 0 Security Hotspots, 100 % coverage on new code y 0 % duplication. Ver [LB-003 Remote CI Evidence](../work-items/LB-003-quality-gate-golden-path/REMOTE_CI_EVIDENCE.md) | mantenedor del repositorio | PASS (LB-003, 2026-09-29) |
| MV-005 | 2026-09-24 | Swagger UI runtime sobre OpenAPI canónico | Render real del JAR local en navegador, sin depender de DB/IdP externos | PASS local: `/swagger-ui/` cargó 9 operaciones; PATCH expandible con parámetros/body/Execute, PUT deprecated, `operationId` y Authorize visibles; assets solo localhost; consola 0 warnings/errores; YAML HTTP `application/yaml` con SHA canónico MATCH. Swagger UI muestra un aviso no bloqueante por `jsonSchemaDialect` distinto del dialecto base; el contrato no se alteró. Servidor y mock OIDC efímeros retirados. Ver [LB-001C.3 VALIDATION](../work-items/LB-001C-openapi-contract-first/LB-001C.3-VALIDATION.md) | backend-team | PASS |
| MV-006 | 2026-09-29 | LB-002.2E — JPA Runtime Golden Path (query + command) con realtime | Validación externa reportada en revisión humana; requiere `local` con `query=jpa`/`command=jpa`, SQL Server real y frontend Angular real; artefacto/log externo no está en este checkout | PASS reportado: `local`, query/command JPA, Java 25, SQL Server real, frontend Angular real; autenticación, POST lote, readback, AN/SJC/EX, dos clientes SSE y reconexión PASS; reconvergencia ~25 s; HTTP como fuente de verdad. 501 de perfil/reclamos queda fuera del Golden Path. Ver cierre LB-002.2E | backend-team | PASS_REPORTED_EXTERNAL (2026-09-29) |

La integración SQL automatizada se registra en VALIDATION; solo una verificación humana inevitable va aquí. Una validación obligatoria pendiente impide DONE del alcance correspondiente.
