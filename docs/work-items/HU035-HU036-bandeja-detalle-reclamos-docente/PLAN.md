---
status: closed
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-21
---

# PLAN — HU035 + HU036: Bandeja y detalle de solicitudes de revisión del docente

Fase 01: Planificación y Definition of Ready conforme a `GUIA_METODICA_IMPLEMENTACION_HU.md`,
[DEFINITION_OF_READY](../../governance/DEFINITION_OF_READY.md) y `AGENTS.md`.

## Identidad y objetivo

- **Historias de Usuario:**
  - **HU035** — Consultar solicitudes de revisión pendientes (rol `DOCENTE`).
  - **HU036** — Consultar detalle y causal de una solicitud (rol `DOCENTE`).
- **Work Item ID:** `HU035-HU036-bandeja-detalle-reclamos-docente`
- **Fecha:** 2026-10-21
- **Base Git:** Backend `develop@4ff37ff`, DB `develop@585f7f0`, Frontend `develop@ea1e537`.
- **Rol / Actor:** Docente titular del grupo (`DOCENTE`). Jerarquía nivel 4 (`HOJA_DE_RUTA_JERARQUIA_ROLES.md`).
- **Justificación de agrupación:** ambas HU comparten pantalla (`TeacherClaimsComponent`), puerto de consulta y
  dominio (`SolicitudRevisionAsistencia`); entregar la bandeja sin su detalle no es demostrable por separado.
- **Objetivo:** que el docente vea, filtre y pagine en servidor las solicitudes de revisión de **sus** grupos y abra
  el detalle de una solicitud con su causal, como paso previo a resolverla (HU037/HU038, ya implementadas en backend).

### Criterios de aceptación

| ID | Criterio |
|---|---|
| AC-01 | El docente solo ve solicitudes de grupos donde es titular (`Grupo.docente → Docente.usuario = ejecutor`), validado **dentro del SP**. |
| AC-02 | La bandeja se pagina en servidor (`pagina`, `tamano` 1..100) y muestra el total real (`totalRegistros`). |
| AC-03 | El filtro por estado (`P`/`A`/`R` o `PENDIENTE`/`APROBADA`/`RECHAZADA`) se aplica en servidor; estado desconocido → `400`. |
| AC-04 | El contador "pendientes" refleja el total de pendientes del servidor, no solo la página cargada. |
| AC-05 | `GET /api/v1/docente/reclamos/{id}` devuelve el detalle de una solicitud del docente titular; si no existe **o no es suya** → `404` (no revela existencia). |
| AC-06 | El detalle incluye la causal: causa registrada de la inasistencia (`RazonCausa`: código y nombre), si asistió, estado de la solicitud (código y nombre legible), justificación del estudiante, respuesta del docente y soporte adjunto (metadatos). |
| AC-07 | Rol distinto de `DOCENTE` → `403`; sin token → `401`. |
| AC-08 | La UI usa `PaginationComponent`, filtro reactivo con Signals (sin `ngModel` sobre campo mutable) y abre el detalle desde el backend; cero polling. |
| AC-09 | Ningún campo de la UI muestra "No informado" por falta de contrato cuando el dato existe en DB. |

## AS-IS y evidencia

| Hecho | Archivo + símbolo / contrato | Evidencia y límites |
|---|---|---|
| SP de bandeja existe, paginado, con titularidad en SP | `GestioAsistenciaDB/schema/stored-procedures/usp_consultar_solicitudes_revision_docente.sql` (LB-004D R2) | Hash en `DB_LB004D_SUPPORT_BINDING_MANIFEST.sha256`; pruebas R2 en `test/test_lb004d_support_binding.sql` (titularidad, otro docente = 0 filas, estudiante = 0 filas, filtro, paginación). |
| SP no proyecta la causal | mismo SP | No une `DetalleAsistencia`/`RazonCausa`; no expone `nombreEstado` al backend (sí lo selecciona). |
| La categoría de radicación se descarta | `usp_radicar_solicitud_revision_asistencia_v2.sql` (`@categoria` solo declarado) y `dbo.SolicitudRevisionAsistencia` (sin columna `categoria`) | El backend la exige (`ERR_CATEGORIA_REVISION_REQUERIDA`) y la envía, pero no se persiste. Ver decisión D1. |
| Literal quemado en SP | `arc.estado = 'ATTACHED'` en R2 | Viola "Cero Datos Quemados"; no existe parámetro equivalente en `uv_parametro`. |
| Endpoint de bandeja implementado | `DocentePortalController#consultarReclamos` → `ConsultarReclamosDocenteInputPort` → `ConsultarReclamosDocenteUseCaseImpl` → `SolicitudRevisionDocenteQueryPort` → `SolicitudRevisionDocenteJpaRepository` | Devuelve `ApiListResponse<ReclamoDocenteResponse>` (`exitoso`, `datos`, `total`), conforme a `http-success-responses.md`. `HTTP_AS_IS_MATRIX`: `IMPLEMENTATION_AVAILABLE_PENDING_E2E`. |
| Sin endpoint de detalle | `DocentePortalController` | Solo `GET /reclamos` y `PATCH /reclamos/{id}`. |
| RBAC | `SecurityConfig` línea 136: `/api/v1/docente/**` → `hasRole("DOCENTE")` | Aplica también al nuevo `GET /reclamos/{id}`. |
| Tests backend existentes | `ConsultarReclamosDocenteFeatureTest` (6), `DocentePortalControllerTest`, `ReclamoDocenteHttpMapperTest`, `SoporteArchivoSqlIT` | Cubren bandeja; no hay detalle. |
| Frontend consume bandeja sin paginar | `core/services/attendance-claim.service.ts#getReclamosDocente` | No envía `pagina`/`tamano`/`estado` → solo los primeros 10. Enmascara `501/404` como `exitoso: true` con lista vacía. |
| Filtro de estado no reactivo | `teacher-claims.component.ts`: `selectedEstado: string` + `[(ngModel)]` leído dentro de `computed()` | `computed` no rastrea campos no-signal: con OnPush el filtro no recalcula. Defecto real. |
| Detalle construido solo con la fila del listado | `teacher-claims.component.ts` vista `RESOLUCION` | `estadoOriginal`, `categoria`, `estudianteCorreo` llegan vacíos → "No informado". |
| Catálogo de HU | `AsistenciasUCO-Infra/docs/user-stories/HU-CATALOG.md` | Corregido el 2026-10-21: HU035 `Fase 3 (FRONTEND)`, HU036 `[PARCIAL]` (antes `[BLOQUEADO 501]`). |
| Datos | `dbo.Estado` = `P/A/R`; `dbo.RazonCausa` = `AN, SJC, EX, CPVP, CPI`; `SolicitudRevisionAsistencia` = 0 filas en local | Se requiere fixture de prueba. |

## TARGET

- **DB:** nuevo SP de lectura `usp_consultar_detalle_solicitud_revision_docente` (titularidad en SP, 0..1 fila, con causal).
  El SP R2 de bandeja **no se modifica** (contrato con hash LB-004D).
- **Backend:** nuevo caso de uso `consultardetallereclamo` y `GET /api/v1/docente/reclamos/{id}`; bandeja sin cambios de contrato.
- **Frontend:** bandeja paginada y filtrada en servidor, filtro con Signals, detalle cargado desde el backend.

## Clase de cambio y alcance de rutas

- Change class: `BEHAVIOR_CHANGE` (nuevo endpoint y SP de lectura; corrección de consumo en UI).
- PRIMARY_VARIABLE: consulta de solicitudes de revisión por el docente titular (bandeja paginada + detalle con causal).
- ALLOWED_PATHS:
  - `GestioAsistenciaBackend/docs/work-items/HU035-HU036-bandeja-detalle-reclamos-docente/*`
  - `GestioAsistenciaBackend/docs/baseline/TECHNICAL_DEBT.md` (solo nuevas entradas)
  - `GestioAsistenciaBackend/docs/contracts/HTTP_AS_IS_MATRIX.md` (registrar `GET /docente/reclamos/{id}`; enmienda E-02 aprobada 2026-10-21)
  - `GestioAsistenciaDB/schema/stored-procedures/usp_consultar_detalle_solicitud_revision_docente.sql` (NUEVO)
  - `GestioAsistenciaDB/schema/seed/16_hu035_parametro_estado_adjunto.sql` (NUEVO, D3 = A)
  - `GestioAsistenciaDB/test_suite.sql` (solo agregar la línea `:r` del test nuevo)
  - `GestioAsistenciaDB/test/test_contract_closure.sql` (solo clasificar el SP nuevo como `READ_QUERY_RESULTSET` en la lista
    normativa de exclusiones; ampliación aprobada por el owner el 2026-10-21, precedentes `b7667b6` y `231be58`)
  - `GestioAsistenciaDB/test/test_usp_consultar_detalle_solicitud_revision_docente.sql` (NUEVO)
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/application/features/docente/consultardetallereclamo/**` (NUEVO)
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/application/secondaryports/academic/SolicitudRevisionDocenteQueryPort.java`
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/application/secondaryports/academic/projection/DetalleSolicitudRevisionDocenteProjection.java` (NUEVO)
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/jpa/repository/SolicitudRevisionDocenteJpaRepository.java`
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/jpa/entity/SpDetalleSolicitudRevisionDocenteEntity.java` (NUEVO)
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/docente/**`
  - `GestioAsistenciaBackend/src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/wiring/<Docente>WiringConfiguration.java`
  - `GestioAsistenciaBackend/src/test/java/**/docente/**` y `**/jpa/**` (tests nuevos)
  - `GestioAsistenciaFrontend/src/app/core/services/attendance-claim.service*.ts`
  - `GestioAsistenciaFrontend/src/app/core/mappers/claim.mapper*.ts`
  - `GestioAsistenciaFrontend/src/app/core/models/role-management.model.ts`
  - `GestioAsistenciaFrontend/src/app/features/teacher/teacher-claims/**`
- FORBIDDEN_PATHS:
  - `GestioAsistenciaDB/schema/tables/**` (salvo aprobación explícita de D1 = A)
  - `GestioAsistenciaDB/schema/stored-procedures/usp_consultar_solicitudes_revision_docente.sql` y demás SP del manifiesto LB-004D
  - `GestioAsistenciaDB/docs/contracts/**`, `GestioAsistenciaBackend/docs/contracts/**` (WORM)
  - `usp_resolver_solicitud_revision_asistencia.sql` y el flujo de resolución (HU037/HU038)
  - `SecurityConfig` (el matcher existente ya cubre la ruta)
- CONTRACTS: nuevo SP de lectura; nuevo endpoint HTTP aditivo; modelo TS de detalle (ver `CONTRACT.md`).
- PROVIDERS: SQL Server 2022 (Docker `sql_server_asistencias`), Keycloak (token real E2E), MinIO solo para la prueba del soporte adjunto.
- EXTERNAL_ENVIRONMENT: contenedores locales activos (verificado 2026-10-21: SQL Server y Keycloak `Up`).
- SECURITY_IMPACT: lectura de datos personales mínimos del estudiante (nombre; **sin correo**). Titularidad en SP. 404 en lugar de 403 para solicitudes ajenas.
- OBSERVABILITY_IMPACT: `@idCorrelacion` desde `CorrelationIdContext.require()`; operación JPA nombrada `consultarDetalleSolicitudRevisionDocente`.
- TEST_LEVEL_REQUIRED: T-SQL transaccional + unit + controller + IT JPA contra SQL Server real + Karma + E2E con token Keycloak.
- ROLLBACK: cambios aditivos. Revertir PR de Frontend → Backend → DB; el SP nuevo se elimina con `DROP PROCEDURE` sin afectar R2.
- CONSUMERS: `TeacherClaimsComponent` (único consumidor de `/docente/reclamos`).
- STOP_CONDITIONS: ver sección Rollback.

## Alcance

1. HU035: consumo paginado y filtrado en servidor; contador de pendientes desde servidor; quitar el enmascaramiento `501/404`.
2. HU036: SP de detalle + endpoint + caso de uso; vista de resolución alimentada por el detalle.
3. Corrección del filtro no reactivo (`selectedEstado` → `signal`).
4. Registro de deuda: categoría de radicación no persistida (si D1 = B) y literal `'ATTACHED'` en R2.

## No alcance

- Resolver la solicitud (HU037/HU038): ya implementado; solo se reutiliza el botón existente.
- Descarga/visor de soporte (HU039): depende de TD-004 (lectura docente del storage).
- Migrar `ApiListResponse` → `ApiPageResponse` (desvío global; requiere work item propio).
- Persistir la categoría de radicación (solo si D1 = B; ver decisiones).
- Notificación SSE de nuevas solicitudes al docente (no exigido por HU035/HU036).

## Archivos afectados

- **EXISTENTES:** `SolicitudRevisionDocenteQueryPort.java`, `SolicitudRevisionDocenteJpaRepository.java`, `DocentePortalController.java`,
  wiring del docente, `attendance-claim.service.ts`, `claim.mapper.ts`, `role-management.model.ts`, `teacher-claims.component.ts` (+ specs).
- **NUEVOS:** `usp_consultar_detalle_solicitud_revision_docente.sql`, su test T-SQL, paquete `application/features/docente/consultardetallereclamo/**`,
  `DetalleSolicitudRevisionDocenteProjection.java`, `SpDetalleSolicitudRevisionDocenteEntity.java`, `DetalleReclamoDocenteResponse.java`, tests.
- **RETIRAR:** ninguno.

## Contratos y consumidores afectados

| Dimensión | Cambio | Compatibilidad |
|---|---|---|
| PERSISTENCE | SP nuevo de lectura | Aditivo; R2 intacto. |
| HTTP | `GET /api/v1/docente/reclamos/{id}` nuevo | Aditivo; `GET /reclamos` sin cambios de contrato. |
| DOMAIN | Nuevo caso de uso y proyección | Aditivo. |
| SECURITY | Matcher existente `DOCENTE` | Sin cambios. |
| REALTIME | Ninguno | — |

## Decisiones requeridas (bloquean READY)

| ID | Pregunta | Opción A | Opción B (recomendada) |
|---|---|---|---|
| **D1** | ¿Qué es la "causal" de HU036? | Persistir la categoría elegida por el estudiante: columna nueva `categoria` nullable (o FK a catálogo) en `SolicitudRevisionAsistencia` + cambio en `usp_radicar_solicitud_revision_asistencia_v2` (contrato LB-004D, requiere alineación y nuevo hash). | Causal = causa registrada de la inasistencia (`DetalleAsistencia.razonCausa` → `RazonCausa`) + `asistio`. Sin DDL. La persistencia de la categoría se registra como deuda y se aborda con HU005/HU014 (catálogo de causales). |
| **D2** | ¿Cómo se obtiene el detalle? | Extender el SP R2 con nuevas columnas (toca contrato con hash). | SP nuevo `usp_consultar_detalle_solicitud_revision_docente` + `GET /reclamos/{id}`. |
| **D3** | Literal `'ATTACHED'` en el nuevo SP | Crear parámetro en catálogo (`ARCHIVO_SOPORTE` / `ESTADO_ADJUNTO`) vía seed y usar `ufn_obtener_parametro`. | Replicar el literal como R2 y registrar deuda. |

Recomendación: **D1 = B, D2 = B, D3 = A**. **Aprobado por el owner el 2026-10-21.**

## Riesgos y dependencias

- R1: sin datos de solicitudes en la DB local → el TEST_PLAN incluye fixture transaccional con rollback.
- R2: si D1 = A, se abre un `CONTRACT_CONFLICT` con el manifiesto LB-004D; se requiere consenso del equipo DB.
- R3: la norma backend vigente ([http-success-responses](../../architecture/http-success-responses.md)) define `ApiListResponse`
  (`exitoso`, `datos`, `total`), mientras la Constitución menciona `ApiPageResponse`. Se sigue la norma backend vigente y se
  registra la inconsistencia documental.
- Dependencias: HU010 (radicación) para generar datos E2E; HU037 para continuar el flujo.
- Desvío de la hoja de ruta: `HOJA_DE_RUTA_JERARQUIA_ROLES.md` sugiere HU076 como siguiente paso (Etapa 2). HU035/HU036
  pertenecen a la Etapa 4 y no dependen de HU076 (docentes, grupos y estudiantes ya existen en la semilla).

## Test plan

Ver [TEST_PLAN.md](TEST_PLAN.md). Secuencia RED → GREEN por capa (DB → Backend → Frontend) → VALIDATE.

## Rollback y stop conditions

- `CONTRACT_CONFLICT`: cualquier necesidad de modificar R2 o `usp_radicar_*_v2` sin aprobación de D1 = A.
- `TEST_CONTRACT_CONFLICT`: un test RED que solo pase cambiando el contrato aprobado.
- `BLOCKED_BY_MISSING_EVIDENCE`: imposibilidad de levantar SQL Server/Keycloak para IT/E2E (se registra `NOT_RUN`).

## Deuda conocida y validación manual

- TD-004 (lectura de soportes por docente): excluye HU039.
- Nueva (a registrar): categoría de radicación no persistida (si D1 = B).
- Nueva (a registrar): literal `'ATTACHED'` en `usp_consultar_solicitudes_revision_docente` (R2).
- Inconsistencia documental: Constitución (`ApiPageResponse<T>`) vs norma backend (`ApiListResponse<T>`).

## Definition of Ready

**READY (condicionado al build base)** — D1/D2/D3 y `CONTRACT.md` aprobados por el owner el 2026-10-21.
Condición: cada capa verifica su build/suite base en verde antes de escribir RED y lo registra en `VALIDATION.md`;
si la base está roja se detiene esa capa (`BLOCKED_BY_MISSING_EVIDENCE`) y se reporta.

Ejecución: implementación delegada por capa a un subagente (modelo Sonnet) a pedido del owner; la auditoría (fase 05)
la realiza el agente principal antes de pasar a la siguiente capa. Sin commits ni push hasta aprobación del owner.
