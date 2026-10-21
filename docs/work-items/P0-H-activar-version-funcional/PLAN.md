# PLAN — P0-H: activar y validar la última versión funcional

## Identidad y objetivo

- Fecha / rol / base Git: 2026-10-20 / integración local y validación / backend actual `6893f4d`; candidatos B1 `f45a5213`, F1+teacher `95a202a`, D1/R2 `b698c76`.
- Objetivo: activar en los repositorios originales una combinación local reproducible que permita repetir las pruebas manuales de docente y estudiante, sin convertir la evidencia histórica en PASS actual ni aprobar unilateralmente D1.
- Criterios de aceptación: ramas de integración preparadas, cambios candidatos realmente presentes en los checkouts originales, stack centralizado levantado sin secretos en evidencia, `mvn verify`/`npm run verify` intentados, pruebas manuales registradas y bloqueos contractuales separados.
- Skills y fuentes autoritativas: `uco-baseline`, `uco-arquitectura`, `uco-contratos`, `uco-testing`, `uco-persistencia`, `uco-seguridad`; `AGENTS.md`, `SOURCE_OF_TRUTH.md`, `DB_BASELINE_CONTRACT.md`, OpenAPI canónico, `BACKEND_GOLDEN_PATH_CONTRACT.md`, `P0-G` y `PROFESSOR_DECISION.md`.

## AS-IS y evidencia

| Hecho | Archivo + símbolo / contrato + versión | Evidencia y límites |
|---|---|---|
| Backend activo | Repo backend, `HEAD 6893f4d`; reporte previo `befbdbf5` | La rama actual contiene documentación P0 posterior; no coincide con el SHA reportado. |
| Backend candidato | `jose-valencia/backend-functional-recovery` → `f45a52137674415871882043771676b6592fb72c` | Contiene B1 W1/W2/R1/R2, soporte y claims; diverge desde `72312d7c`. |
| Frontend activo | Repo frontend, `jose-valencia/maint-004-frontend-utc-v2-integration` → `b4368d1e` | Tiene tres cambios locales que deben preservarse. |
| Frontend candidato | `95a202a6b767e537f519012a847cffe3a7c100ab` | `fac2f640` es ancestro de `95a202a`; F1 y corrección docente quedan incluidos. |
| DB activa | Repo DB, `develop` → `32b70ebe` | Checkout limpio observado; contrato oficial sigue siendo v1 de cuatro columnas para SP públicos. |
| DB candidata | `jose-valencia/db-functional-recovery` → `b698c76112490b91298d16ccd1fb6a793648adec` | D1/R2 añade W1/W2/R1/R2; aprobación del owner DB pendiente. |

## TARGET

Una combinación local candidateada en ramas `jose-valencia/p0-h-*-integration`, sin merge final ni publicación: backend B1 integrado sobre la línea actual, frontend F1+teacher activado sobre `95a202a`, DB D1/R2 disponible solo como implementación experimental local y la infraestructura vigente. La clasificación oficial de R1/R2 y la autoridad DB permanecen pendientes.

## Clase de cambio y alcance de rutas

- Change class: BEHAVIOR_CHANGE + CONTRACT_ANALYSIS + INFRASTRUCTURE (activación local, no cambio de producción).
- PRIMARY_VARIABLE: `FUNCTIONAL_RUNTIME_REPRODUCIBILITY`.
- ALLOWED_PATHS: backend `src/**`, `pom.xml`, tests y configuración local necesaria; `docs/work-items/P0-H-activar-version-funcional/**`; frontend y DB únicamente en las ramas de integración de los repositorios originales; scripts locales de arranque/validación sin secretos.
- FORBIDDEN_PATHS: producción, secretos y `.env` reales, merges finales, force-push, reset destructivo, borrado de backups/cuarentenas, cambios unilaterales al contrato oficial DB/OpenAPI y cambios de esquema fuera de D1/R2 candidateado.
- CONTRACTS: OpenAPI canónico, `BACKEND_GOLDEN_PATH_CONTRACT`, `DB_BASELINE_CONTRACT` v1, evidencia D1/R2 de P0-G y decisiones de almacenamiento/ownership de LB-004.
- PROVIDERS: SQL Server, Keycloak, MinIO y ClamAV locales; Angular y Spring Boot de los repositorios originales.
- EXTERNAL_ENVIRONMENT: puertos observados por P0-G: SQL Server 1433, backend 8080, frontend 4200, MinIO 9000, ClamAV 3310 y Keycloak 8081 cuando aplique.
- SECURITY_IMPACT: conservar JWT, roles, ownership y `SEC_001/SEC_002`; no registrar tokens, credenciales, URLs privadas ni contenido de soportes.
- OBSERVABILITY_IMPACT: usar logs/correlation existentes solo para comprobar wiring; no cambiar observabilidad por esta activación.
- TEST_LEVEL_REQUIRED: Maven verify, frontend verify, contratos/IT DB afectados, preflight real y E2E manual real cuando exista fixture autorizado.
- ROLLBACK: volver a la rama/commit previo de cada repositorio o retirar únicamente la rama de integración con autorización; restaurar snapshots, sin `reset --hard` ni borrar respaldos.
- CONSUMERS: frontend F1, backend B1, SP D1/R2, Keycloak, MinIO/ClamAV y operadores de IntelliJ/Angular.
- STOP_CONDITIONS: `CONTRACT_CONFLICT` DB, `TEST_CONTRACT_CONFLICT`, secreto expuesto, checkout con cambios no preservados, provider obligatorio no disponible o E2E mutante sin fixture/rollback autorizado.

Lo no listado como Allowed no se modifica. La activación local está autorizada por la solicitud P0-H; no equivale a aprobación contractual ni a merge/publicación.

## Alcance

- Preservar cambios locales de backend y frontend antes de mover referencias.
- Crear ramas locales de integración con nombres `jose-valencia/*`.
- Integrar el backend candidato compatible y activar frontend/DB candidatos en sus checkouts originales.
- Levantar el stack oficial evitando procesos duplicados y verificar que frontend consume backend 8080.
- Ejecutar gates y pruebas reales disponibles; registrar `PASS`, `FAIL`, `NOT_RUN` o `BLOCKED` con exit code.
- Preparar evidencia de PR contractual para D1/R2 sin aprobarla ni modificar producción.

## No alcance

- No cambiar el contrato público OpenAPI ni el esquema oficial DB.
- No declarar D1 aprobado por presencia de scripts/SP.
- No afirmar PASS del E2E completo sin ejecutar estudiante → reclamo → soporte → docente con persistencia y autorización reales.
- No mezclar clones en cuarentena, no crear worktrees y no eliminar backups.

## Archivos afectados

### EXISTENTES

- Backend: archivos Java, tests, `pom.xml` y configuración local que el merge candidato afecte.
- Frontend: checkout original y los tres archivos locales preservados.
- DB: schema/tests/scripts del commit `b698c76`, sin modificar el contrato oficial desde backend.
- Infra: scripts existentes de `start-local-stack.ps1` y configuración local ignorada, sin copiar secretos.

### NUEVOS

- Esta carpeta: `PLAN.md`, `TEST_PLAN.md`, `VALIDATION.md` y `CONTRACT_MATRIX.md`.
- Solo si la validación lo exige: evidencia sanitizada de hashes/puertos, nunca logs crudos ni secretos.

### RETIRAR

- Ninguno.

## Contratos y consumidores afectados

| Ámbito | Consumidor | Compatibilidad | Decisión |
|---|---|---|---|
| HTTP | frontend F1 ↔ backend B1 | candidata, se comprueba con tests/E2E | No cambiar OpenAPI sin decisión. |
| Persistence command | W1/W2 | cuatro columnas | Compatible con baseline de mutaciones, pendiente owner DB. |
| Persistence query | R1/R2 | diez/veinticinco columnas observadas | `CONTRACT_CONFLICT`/decisión requerida si se pretende elevar a oficial. |
| Security/storage | archivo privado + ownership | candidateada | Validar con providers reales; no exponer object key ni secretos. |

## Riesgos y dependencias

- La integración de backend es grande y puede contener diferencias no relacionadas con B1; cualquier conflicto se detiene y se documenta.
- El frontend tenía cambios locales; solo se restauran después de confirmar que no sobreescriben la corrección candidateada.
- D1/R2 puede ejecutar localmente sin ser contrato aprobado.
- E2E mutante puede contaminar datos; requiere fixture controlado y rollback.
- `infra/files/.env` puede faltar; se reporta sin inventar valores.

## Test plan

Referencia `TEST_PLAN.md`. Secuencia: requirement → contrato → snapshot de tests existentes → integración local → GREEN/verify → validación real de providers → E2E manual.

## Rollback y stop conditions

- `CONTRACT_CONFLICT: P0H-DB-001`: DB v1 de mutaciones frente a clasificación/resultsets R1/R2 D1.
- `BLOCKED_BY_MISSING_EVIDENCE: P0H-E2E-001`: fixture/rollback/autorización del flujo mutante si no existe.
- `P0H-PRESERVE-001`: cambios locales no preservables o conflicto al restaurarlos; detener checkout afectado.

## Deuda conocida y validación manual

- `TD-P0-G-001` / `MV-P0-G-001` siguen abiertos hasta resolver autoridad DB y ejecutar el flujo real.
- La evidencia histórica de grupos, sesiones, reclamos y soportes se usa para orientar la comprobación, no como PASS actual.

## Definition of Ready

`READY_WITH_LOCAL_EXPERIMENTAL_EXCEPTION` para activar y validar localmente, por instrucción explícita P0-H versionada en este plan. `NOT_READY` para integración oficial de D1/R2: el owner DB debe aprobar la clasificación de R1/R2, la autoridad del remoto y el contrato. Responsable del desbloqueo: equipo/owner DB; auditoría posterior: backend + frontend.
