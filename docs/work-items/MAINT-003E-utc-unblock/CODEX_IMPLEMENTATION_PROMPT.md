# Prompt de ejecución para Codex — MAINT-003E UTC v2 completo

## Mandato y autoridad
El usuario autoriza **proponer e implementar en ramas aisladas los ajustes necesarios de DB** para que UTC v2 funcione correctamente. No se autoriza modificar datos de producción, hacer deploy productivo, forzar push, fusionar PR, borrar tests ni fingir firmas de otros propietarios. No pedir más decisiones técnicas resueltas aquí. Ejecutar el máximo trabajo posible y dejar PRs independientes listos para revisión cuando corresponda. El freeze DB-GP-001C sigue siendo la línea base v1: abrir nuevo work item versionado que documente su extensión, no editar baseline v1 retrospectivamente.

Repos: backend `alejovh12/AsistenciasUCO`; DB `johnjduque/gestion-asistencia-db`; frontend `AsistenciasUCO/AsistenciasUCO-Frontend`. Priorizar worktrees locales existentes. No utilizar como autoridad la rama remota 003E para pisar el commit local `afbde3a` del usuario.

## Paso 0: reconocimiento y reconciliación obligatorios
1. En backend local `.workspace/m3d` inspeccionar `git status --short`, `git log --graph --oneline --decorate -15`, `git branch -vv`, `git remote -v`. Guardar HEAD de cada repo, proteger worktrees sucios sin reset/clean/rebase forzado.
2. Si remoto disponible, hacer `git fetch origin` y comparar con `origin/jose-valencia/maint-003e-utc-unblock-decisions-tests`; integra mediante cherry-pick o merge seguro, preservando merges locales de 003C, `afbde3a`, pruebas RED y cambios ajenos. Resolver duplicados, no perder tests. Verificar realmente las ramas por `git branch -a`, no asumir.
3. Leer `AGENTS.md`, skills de backend/DB/frontend, contratos, work items MAINT-003B/003C/003D/003E. Leer específicamente `ROOT_CAUSE_AND_FINAL_DECISIONS.md`, `DB_OWNER_WORK_ITEM_READY_FOR_REVIEW.md`, `TEST_EXECUTION_POLICY_AND_ACCEPTANCE.md`, `DB_D06_READONLY_CONTRACT_ASSERTIONS.sql` y documentación vigente DB `docs/contracts/DB_BASELINE_CONTRACT.md`.
4. Verificar la divergencia: remoto 003E no tiene todavía codec estricto ni 003C; local reportado sí. No sobrescribir clase estricta con `HttpUtcInstantCodec` antiguo del remoto.
5. En DB auditar todas ramas, PR, SQL schema, SP, vistas, seed, scripts, permisos, tests; validar cómo se ejecuta el deploy SQL en el repo antes de redactar migraciones. En frontend revisar `session.service.ts`, entornos `/api/v1`, todos los consumidores y exports.

## Paso 1: codec D02 y tests
- Conservar gramática exacta RFC3339 `YYYY-MM-DDTHH:mm:ss[.1..7](Z|±HH:mm)` con validación estricta calendario, offset y rango SQL tras UTC. `.5Z` es VÁLIDO (500000000 ns); 8–9 decimales son inválidos, jamás redondear; no trim; no dependencia del timezone host.
- Corregir **solo la aserción contradictoria** del test strict local 003C que rechaza `.5Z`, convirtiéndola en aceptación con precisión exacta. Conservar todas las otras aserciones, originales 7/7, D02 5/5, strict 8/8, guard v1 3/3, nuevas pruebas `HttpUtcInstantCodecFractionContractTest`. No "solucionar" tests bajando precisión.
- Ejecutar unidades de codec en Java25 y reportar números reales. Registrar incidente de test corregido con justificación en VALIDATION.

## Paso 2: migración DB **como nuevo work item post-freeze**
- Abrir rama de DB desde `develop` actual, sin modificar el contrato v1. Elegir código de work item explícito `UTC-D06-POST-FREEZE`. Fuente verdad `schema/**`; diseñar deploy idempotente/reversible consistente con el repo, sin seeds destructivas.
- Agregar `Sesion.procedenciaTemporal NVARCHAR(24) NULL` con CHECK `NULL|UTC_V2|UTC_GENERADOR|UTC_OWNER`, sin valor por defecto. Filas históricas conservan exactamente sus dos `datetime2(7)` y NULL. Auditar cada escritor SQL, seed, DML directo, variables y transacciones.
- Actualizar `usp_crear_sesion` y `usp_actualizar_sesion` para aceptar modo interno v2 de manera controlada **sin romper llamadas v1 por parámetros nombrados**; ejecutar INSERT/UPDATE de horas y marca en la MISMA sentencia. v1 deja NULL, v2 usa UTC_V2, generador `usp_generar_sesiones_grupo` usa UTC_GENERADOR después de zona institucional validada. No confiar en una bandera libre del request ni suponer que cambiar un SP es prueba de autenticación DB: revisar permisos efectivos/identidad del servicio y, si exige separación robusta, SP v2 separado con privilegios de ejecución diferenciados. Conservar 4 columnas de retorno SP exactamente y ownership existente.
- NO ALTERAR `uv_sesion` ni `uv_auth_sesion` existentes: el baseline exige proyección exacta de `uv_sesion`. Añadir `uv_sesion_v2` y, si RBAC lo requiere, `uv_auth_sesion_v2`, con las 10 columnas legacy más procedencia. Tests de esquema en `DB_D06_READONLY_CONTRACT_ASSERTIONS.sql`, snapshots de columnas exactas.
- Protección ante UPDATE directo que cambia horas 08→18 sin invalidar etiqueta: permisos DML runtime sin bypass y tests negativos efectivos, estrategia de auditoría/trigger/firmas de módulos según DB owner. No prometer imposibilidad frente a sysadmin. Debe quedar registro del mecanismo y sus límites.
- Resolver con tests la semántica de `DetalleAsistencia.fechaHoraInicio/Fin`. `usp_sincronizar_asistencia_estudiante_interno` copia las horas de la sesión tanto en INSERT como en UPDATE del detalle. Si son snapshot histórico, mantener la política expresa; si son vigentes, implementar sincronización atomizada autorizada. No modificar a ciegas.
- Proponer proceso administrativo de clasificación `UTC_OWNER` con manifiesto por ID y ambiente; **las 3 sesiones locales quedan NULL** hasta evidencia, no convertirlas ni clasificarlas por intuición. La ausencia de clasificación NO impide generar nuevas sesiones confirmadas una vez desplegada la DB.
- Ejecutar tests SQL en contenedor SQL Server 2022 aislado, con backup previo y restauración ensayada. Incluir compatibilidad v1, fechas exactas, 3 SP, vistas y permisos; concurrencia, idempotencia, rollback, no escritura parcial; asegurar 0 skips focales. No ejecutar DDL en DB compartida ni producción.

## Paso 3: backend v2 contract-first
- Crear DTO/controller independientes POST/PATCH/GET ID/GET grupo en `/api/v2/sesiones`; ningún PUT v2. Reutilizar hexagonal + JPA, sin JDBC nuevo. No crear 501/placeholder. Actor proviene de JWT y verificar `DOCENTE` y ownership usando controles reales existentes.
- POST/PATCH requieren ambas fechas y validan todos los campos; string offset estricto; errores `400 VALIDATION_ERROR` con details de campo (`FIELD_REQUIRED/FIELD_INVALID_FORMAT/FIELD_UNKNOWN`), sin 500 de codec y sin eco de secretos. `fin <= inicio` comparado en UTC conserva `SES_004`.
- Consulta v2 incluye `procedenciaTemporal`, `estadoTemporal`. Confirmada produce `Z` usando valor UTC; indeterminada deja las fechas nulas y muestra estado INDETERMINADA, preservando identificación/listado. No excluir históricos.
- Crear proyección JPA de vista v2 (`@Entity` para vista), sin cambiar wire v1 `SesionConsultadaDTO` ni convertir recurrentes `LocalTime`. Documentar OpenAPI versionado con schemas y seguridad. `SesionUtcActivationGuardTest` protege ausencia v2 antes del gate; solo sustituir por guard de activación/compatibilidad verificable al completar DB real. No borrar guard.
- Llevar las 13 pruebas HTTP y 3 OpenAPI RED a GREEN **por implementación**, no silenciándolas. Revisar la diferencia entre cantidad reportada y métodos test reales: usar Surefire XML para conteos correctos.

## Paso 4: validación E2E y frontend
- SQL real + backend real + Keycloak real: POST→SELECT→GET→PATCH→SELECT→GET, marcaje en misma sentencia, JWT 401/403, ownership y correlación. Caso Bogotá 2026-07-15 20–22 (-05) = UTC 2026-07-16 01–03 (Z), Berlín verano 03–05 (+02) y Londres 02–04 (+01). Verificar diferencia de fecha local.
- Frontend vive en Angular `AsistenciasUCO/AsistenciasUCO-Frontend`, cuyo `environment.apiUrl` termina por defecto en `/api/v1`; crear base v2 diferenciada o API client explícito, sin reemplazar todo el apiUrl v1.
- Guardar instantes UTC Z en modelo canónico; calcular visualización con `Intl.DateTimeFormat` y zona IANA actual/seleccionada después de refresh; si el usuario usa selección de zona, respetarla de forma consistente. Implementar formulario local a offset real del día; rechazar DST gap y pedir elección en overlap; nunca inferir una zona para un legacy naive. No cambiar aún rutas v1 de estudiantes y Excel; documentar que no se migraron.
- Tests Angular `npm ci`, `npm run verify` (según package.json) y E2E: cambio Bogotá→Berlín al recargar, Londres verano/invierno, cruce de medianoche, error de zona, lista mixta indeterminada, sin regresión de SSE/asistencia. Habilitar frontend v2 solo tras certificación backend. Si DB no está aprobada, preparar frontend tras feature flag OFF, NO afirmar release.

## Gates y reporting
- Gate A: codec estricto y tests sin contradictorios.
- Gate B: DB work item + DDL/SP/vistas + SQL IT real; aprobar contrato de coexistencia, respaldos.
- Gate C: API v2 + OpenAPI + MockMvc + JWT/SQL real + no skips focales; suite Java25, JaCoCo, ArchUnit, Sonar/CodeQL/Trivy según acceso. Reportar cada job no ejecutado como NOT_RUN.
- Gate D: frontend v2 + TZ/DST + recarga + build/test; feature flag solo habilitada cuando Gate C real pase.
- Entregar por repositorio BRANCH, BASE_SHA, HEAD_SHA, modified files, test command, executed/passed/failed/errors/skipped, blockers, exact diff, rollback. Distinguir RED previsto de bug nuevo. No merge/push/deploy productivo sin permiso explícito o las reglas de colaboración del repo. Si no hay permisos GitHub de escritura DB, trabajar localmente y dejar patch/commits revisables; no detener desarrollo solo por un 403 del conector.

## Prohibiciones
No borrar/ignorar/deshabilitar tests, no alterar vistas v1, no modificar shape actual sin work item, no etiquetar históricos Z, no degradar RBAC ni manejo de errors, no reescribir DB shared, no hacer reset --hard, no revelar secretos, no afirmar GREEN por no ejecutar, no ofrecer fechas/horarios falsos para registros indeterminados.

## Definición de terminado
Nuevo work item DB versionado y aprobado, migración aplicada en entorno de pruebas aislado, SQL IT y seguridad reales, v2 POST/GET/PATCH implementados y certificados, v1 estable, contratos OpenAPI publicados, frontend consumiendo v2 en entorno de ensayo y pruebas zona/recarga en verde. Si faltan firmas/deploy/credenciales, marcar la etapa exacta BLOCKED_EXTERNAL_DEPENDENCY, dejando código/tests/SQL/PR preparados, no maquillar un READY.
