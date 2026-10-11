---
status: REVIEWED_WITH_SCOPED_PROVIDER_BLOCKER
date: 2026-10-10
topic: CC-003G-01
published_backend_ref: jose-valencia/maint-003f-utc-v2-implementation@30fcd5e
published_db_ref: feat/utc-d06-post-freeze@6946a73
reported_local_backend_commit: e45c36a
reported_local_db_commit: 3842f70
---

# CC-003G-01 — Auditoría independiente de proveedores DB faltantes

## Alcance y fiabilidad
Se revisaron archivos publicados de backend y DB en GitHub y el reporte del usuario. Los commits locales `e45c36a` (backend) y `3842f70` (DB) **NO estaban en origin** a la hora de esta revisión. No se afirma haber revisado esos cambios locales ni ejecutado Maven, Failsafe, SQL Server o Keycloak en esta pasada.

Ramas remotas verificadas: backend `30fcd5e`, DB `6946a73`. Informes **reportados**, pendientes de verificación en estos commits locales: Surefire 1495/1495 PASS; JaCoCo 91.06/78.77; SQL global 179 tests (178 PASS/1 skip); JWT + HTTP UTC v2 POST/GET/PATCH real PASS; Failsafe 191 (3 failures, 0 errors, 3 skipped). Por ello `UTC_V2_SCOPED_SECURITY_REPORTED_PASS`; `FULL_BACKEND_SQL_INTEGRATION=NOT_GREEN`.

## Proveedores ausentes confirmados en ramas remotas
1. `UsuarioJpaRepository.SQL_SINCRONIZAR_USUARIO` ejecuta `dbo.usp_sincronizar_usuario` con parámetros `@idTipoIdIdentificacion,@numeroIdentificacion,@primerApellido,@segundoApellido,@primerNombre,@segundoNombre,@correo,@password,@idCorrelacion`. No aparece ese SP en `schema/stored-procedures/**` de DB publicada.
2. `PlanEstudioJpaRepository.SQL_REGISTRAR_O_ACTUALIZAR_PLAN` ejecuta `dbo.usp_registrar_o_actualizar_plan_estudio` con `@idPlanEstudio,@idPrograma,@inp,@idCorrelacion`. Tampoco está en DB publicada.
3. `SqlStoredProcedureContractIT` exige ambos SP y la forma exacta de sus parámetros; `AcademicUserCommandsSpParityIT` los invoca por puertos, no son tests artificiales desconectados de producción.
4. Existe `usp_sincronizar_usuario_interno` en DB, pero utiliza 3 parámetros OUTPUT para resultado y no expone por sí solo una fila de resultado canónico. **No sustituirlo** cambiando nombre en `UsuarioJpaRepository`. Además, el hecho de que un método se llame «sincronizar» no implica que el interno implemente upsert idempotente: validar semántica real. No se encontró un SP equivalente de planes de estudio en el árbol publicado.
5. `AcademicUserCommandsSpParityIT` documenta en un Javadoc que TD-043 está cerrado y esos proveedores existen; contradice la fuente DB publicada. La resolución correcta es actualizar estado/contrato y código con evidencia, no cambiar expectativas de tests ni falsear que proveedor existe.
6. Ninguno de estos dos proveedores es específico de `/api/v2/sesiones`, pero ambos son requeridos por funciones reales de usuario y coordinador; la integración integral no puede certificarse mientras falten.

## Decisión requerida al equipo backend + owner DB
**Recomendación preferida:** acordar e implementar los proveedores públicos exigidos por el backend **en un work item DB separado del D06 de sesiones**, con sus contratos de comportamiento completos y tests unitarios, contractuales e integración de éxito/rechazo. No fabricar SP dummy con resultado SUCCESS ni hacer alias directo a `usp_sincronizar_usuario_interno` para esquivar los tests.

- `usp_sincronizar_usuario`: definir CREATE vs UPSERT, unicidad por identificación/correo, tratamiento de reintentos, no exposición de contraseña/ hash, correlación `DBCODE`, id del usuario generado/consultable, límites de autorización según `/api/v1/usuarios`. Comprobar transacción interna antes de wrapper. El SP público deberá devolver UNA fila EXACTA `idCorrelacion,mensajeUsuarioResultado,mensajeTecnicoResultado,estadoResultado`, sin resultsets intermedios.
- `usp_registrar_o_actualizar_plan_estudio`: definir titularidad/autorización de COORDINADOR sobre `idPrograma`, qué identifica registro existente (id, programa o regla de negocio), idempotencia, INP válido, insert/update, error de programa inexistente, transacción/rollback y respuesta canónica. **Hallazgo P1 de autorización:** el adapter JPA actual no envía `idUsuarioEjecutor` a este procedimiento, aunque Application valida el alcance. Decidir si el contrato debe añadir `idUsuarioEjecutor` y propagarlo explícitamente por puertos/DTO/JPA y pruebas; no dejar un procedimiento con privilegios amplios sin autorización de DB por confiar solo en el controlador.
- Para ambos: coherencia con freeze y estándares de SP del owner, permisos por objeto al runtime no admin, no GRANT EXECUTE ON SCHEMA::dbo, no saltar métodos internos, no cambiar shape legacy, desplegar en DB aislada y probar JWT/ownership pertinentes antes de aprobar release.
- Si se determina que una funcionalidad **está formalmente fuera de alcance**, decisión funcional/contratos + remoción controlada de consumidor y tests correspondientes, con evidencia. No usar `@Disabled` ni `assumeTrue` para tapar ausencia del SP mientras el código lo sigue invocando.

## Pruebas RED/GREEN que debe generar Codex con owner
1. Metadata DB: `OBJECT_ID('dbo.usp_sincronizar_usuario','P')` y `OBJECT_ID('dbo.usp_registrar_o_actualizar_plan_estudio','P')`; firma, orden, tipo, salida canónica.
2. Usuario: sincronización válida/invalidaciones, duplicados y carreras, atomicidad, identificación inexistente, password hash protegido, correlación y ausencia de secretos en errores. Si es sincronización, demostrar comportamiento idempotente.
3. Plan: alta, actualización, intento con programa ajeno, idempotencia, entrada inválida y error funcional, 2 actores de roles distintos, rollback, vista `uv_plan_estudio` consistente.
4. Ejecución efectiva con login SQL de backend de mínimo privilegio, no login SA: SELECT requerido, EXEC públicos permitido, EXEC internos/DML directo denegados.
5. `SqlStoredProcedureContractIT` y `AcademicUserCommandsSpParityIT` y todos los Failsafe `-Pintegration` GREEN, sin ocultar los 3 skips: identificar nombre y aprobación por cada skip.
6. Regresión UTC: POST→GET→PATCH→GET Bogotá/Berlín, 401/403, v1 invariantes, D06 SQL gate, JaCoCo y ArchUnit; verificar que cambios de usuario/plan no alteren UTC.

## Handoff exacto
Primero `git fetch origin` en worktrees locales sin reset forzado, publicar por push seguro `e45c36a`/`3842f70`, capturar diffs `30fcd5e..e45c36a` y `6946a73..3842f70`, exportar Surefire/Failsafe XML y tres test failures completos. Luego resolver el contrato funcional de los dos SP con owner, implementar en rama DB **separada de D06**, actualizar backend solo si el contrato cambia, volver a ejecutar matrices y preparar PR Draft por repo. No merge, no desplegar ni clasificar históricos de sesión.

## Gate frontend
Se puede **empezar el desarrollo frontend UTC en una rama independiente con feature flag OFF**, usando el OpenAPI v2 publicado y mocks contractuales, porque la evidencia E2E UTC v2 fue reportada PASS. No afirmar BACKEND_SQL_INTEGRATION_PASS ni activar frontend v2 hasta cerrar CC-003G-01, validar SHA exacto/CI y aprobar DB deployment y seguridad. No eliminar rutas v1 ni contenedor de ensayo todavía.

## Estado de esta auditoría
Se publicó este documento, sin escribir código de producción ni modificar tests. Para correcciones seguras de código DB requerimos el contrato y los commits locales actualizados. Sin evidencias locales publicadas no se certifican los nuevos permisos y fixtures agregados por Codex.
