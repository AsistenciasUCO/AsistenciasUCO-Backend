# PLAN — P0: Sincronización multirrepositorio y auditoría arquitectónica

## Identidad y objetivo

- Fecha / rol: 2026-10-18 / auditoría y arquitectura backend.
- Base Git backend: `develop` en `72312d7c60449cd0118b8d32b8da02ad1eb0074d` (`origin/develop`, avance fast-forward aplicado antes de este work item).
- Objetivo: producir una línea base comprobada para backend, frontend, DB e infraestructura; auditar el flujo de Coordinador, seguridad y persistencia; separar hallazgos confirmados de solicitudes no contractuales; y dejar micro-work-items implementables solo cuando exista evidencia y DoR.
- Criterios de aceptación: inventario de repositorios/snapshots/remotos/ramas/PRs; AS-IS/TARGET separados; matriz de contratos y proveedores; resultado de gates con `PASS`, `FAIL`, `NOT_RUN` o `BLOCKED`; hallazgos con archivo/símbolo y severidad; decisión explícita sobre `READY_FOR_P1_MINIO`.
- Skills y fuentes autoritativas: `uco-baseline`, `uco-arquitectura`, `uco-contratos`, `uco-persistencia`, `uco-testing`, `uco-seguridad`; `AGENTS.md`; `SOURCE_OF_TRUTH.md`; arquitectura backend; ADR-003/004; API/OpenAPI canónicos; `TESTING_STANDARD.md`; `DEFINITION_OF_READY.md` y `DEFINITION_OF_DONE.md`.

## AS-IS y evidencia

| Hecho | Archivo / repositorio / versión | Evidencia y límites |
|---|---|---|
| Backend local único dentro del workspace | `C:\Users\josev\AsistenciasUCO\AsistenciasUCO`, `develop`, `72312d7c` | `origin` = `https://github.com/alejovh12/AsistenciasUCO.git`; avance fast-forward desde el snapshot local anterior; modificaciones locales preexistentes preservadas. |
| Frontend local oficial fuera del workspace | `C:\Users\josev\OneDrive\Documentos\Front_Asistecias\AsistenciasUCO-Frontend`, `jose-valencia/maint-004-frontend-utc-v2-integration`, `b4368d1e` | remoto = `https://github.com/shesho22/AsistenciasUCO-Frontend.git`; snapshot limpio observado; lectura externa, sin escritura autorizada por el sandbox. |
| DB local oficial fuera del workspace | `C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db`, `develop`, `f2871a9` | remoto = `https://github.com/johnjduque/gestion-asistencia-db.git`; snapshot limpio observado; el `develop` local estaba detrás de `origin/develop` (`e248619`); no se actualiza esta copia desde este work item. |
| Repositorio Infra separado | No localizado en el perfil/OneDrive inspeccionado | El backend contiene `infra/`; no existe evidencia local suficiente de un repositorio separado `AsistenciasUCO-Infra`. |
| Persistencia backend | `docs/adr/ADR-003-jpa-only-persistence.md`, `ADR-004`, `LB-008/VALIDATION.md` | El addendum final documenta JPA-07 PASS, `DIRECT_JDBC_IN_SRC_MAIN = 0`, `BACKEND_MISSING_PROVIDER = 0` y 188 ITs con 1 skip preexistente; los bloques anteriores del mismo documento son históricos. |
| Coordinador AS-IS | `src/main/java/.../coordinador/CoordinadorPortalController.java`, `CoordinadorWiringConfiguration.java` y tests asociados | Consultas y mutaciones existentes se preservan; rutas deliberadamente no disponibles lanzan `FeatureUnavailableException`. No se presume que una solicitud del texto sea contrato vigente. |
| Seguridad AS-IS | `src/main/java/.../security/SecurityConfig.java` y configuración JWT | Spring Security/JWT y filtros actuales; no se encontró starter/wiring PEP en `pom.xml`, `src/main` o `src/test`. P0 no autoriza una migración mayor a PEP. |
| Gate local en curso | `mvnw.cmd -B -ntp clean verify` | El proceso fue lanzado; al crear este plan aún no se dispone de exit code final. No se marca PASS hasta recibirlo. El shell reporta Java 17, aunque Maven compiló con `--release 25`; debe reconciliarse la evidencia del runtime/toolchain. |

## TARGET

Un registro P0 reproducible y versionado que permita abrir micro-work-items independientes para defectos confirmados. Cada corrección posterior debe congelar primero su contrato, crear su RED, declarar su proveedor/consumidor y cerrar con la validación aplicable. P0 no redefine el OpenAPI, el esquema SQL ni la arquitectura de almacenamiento.

## Clase de cambio y alcance de rutas

- Change class: `CONTRACT_ANALYSIS`.
- PRIMARY_VARIABLE: `P0_LINE_BASE_EVIDENCE_COMPLETE`.
- ALLOWED_PATHS: `docs/work-items/P0-estabilizacion-multirrepositorio/`.
- FORBIDDEN_PATHS: `src/main/`, `src/test/`, `pom.xml`, `schema/`, `infra/`, OpenAPI canónico, frontend externo, DB externa, secretos, `.env*`, merges, force-push y cambios de esquema.
- CONTRACTS: OpenAPI canónico, `BACKEND_GOLDEN_PATH_CONTRACT`, `HTTP_AS_IS_MATRIX`, contratos DB comprobados y protocolo de alineación interrepositorios.
- PROVIDERS: backend JPA/SQL Server documentado; proveedores frontend, DB e Infra solo se clasifican con evidencia disponible.
- EXTERNAL_ENVIRONMENT: SQL Server, Keycloak, Azure/Event Grid y MinIO reales no se consideran disponibles hasta observar ejecución controlada; no se inventan resultados.
- SECURITY_IMPACT: auditoría de autenticación, autorización, scope institucional, secretos y doble wiring; sin cambios de seguridad en esta fase.
- OBSERVABILITY_IMPACT: solo registrar evidencia; no cambiar logs, métricas, trazas ni correlation ID.
- TEST_LEVEL_REQUIRED: análisis estático/documental para P0; cualquier cambio de comportamiento exige un work item hijo con RED → GREEN → VALIDATE.
- ROLLBACK: eliminar únicamente los artefactos documentales de este work item antes de commit, o revertir el commit propio si se publica; no tocar cambios de usuario.
- CONSUMERS: frontend oficial, DB oficial, CI/PR del backend y operadores del entorno.
- STOP_CONDITIONS: `CONTRACT_CONFLICT`, `TEST_CONTRACT_CONFLICT`, `BLOCKED_BY_MISSING_EVIDENCE`, secreto expuesto, provider no disponible para una integración obligatoria o petición fuera del contrato canónico.

Lo no listado como Allowed no se modifica. Ampliar el alcance exige un work item hijo y su propio `PLAN.md`.

## Alcance

- Identificar snapshots, remotos, ramas, PRs y estado de sincronización.
- Auditar estructura del Coordinador, composition root, puertos, scope institucional, repositorios JPA, errores HTTP y contratos de persistencia.
- Revisar la presencia/ausencia comprobable de PEP y clasificarla como AS-IS, TARGET o propuesta.
- Revisar frontend/DB/infra hasta el límite de lectura autorizado y marcar `NOT_RUN` lo no ejecutado.
- Ejecutar o documentar gates reproducibles sin alterar contratos ni datos externos.
- Proponer micro-work-items para defectos reales; no implementar mejoras por analogía.

## No alcance

- Implementar MinIO, binding de almacenamiento, Redis, nuevos endpoints, filtros/paginación no presentes en OpenAPI, grupos o docentes no contractuales.
- Cambiar SQL, stored procedures, esquema DB, roles, JWT, PEP, frontend externo o infraestructura externa.
- Reorganizar paquetes por estética cuando la estructura actual cumple la arquitectura.
- Declarar integración real, E2E con fixtures 300/1000/5000 o CI remoto como ejecutada sin evidencia de ejecución.

## Archivos afectados

EXISTENTES leídos: documentación normativa indicada arriba, código/test del Coordinador, configuración de seguridad y persistencia, metadatos Git de los tres repositorios. NUEVOS: `PLAN.md`, `TEST_PLAN.md`, `VALIDATION.md` y `CLOSURE.md` de este work item, conforme se obtenga evidencia. RETIRAR: ninguno.

## Contratos y consumidores afectados

Sin cambio contractual en P0. DOMAIN / HTTP / PERSISTENCE / SECURITY / REALTIME se auditan y se referencian; cualquier discrepancia se devuelve al owner del contrato antes de implementación. Consumidores: frontend oficial y DB oficial identificados por remoto y snapshot; compatibilidad aún pendiente de matriz detallada.

## Riesgos y dependencias

- Los repositorios frontend y DB están fuera del workspace escribible.
- La DB local está desactualizada respecto de su remoto y no se sincroniza aquí sin una decisión de escritura separada.
- No se localizó un repositorio Infra independiente.
- La ejecución local informa Java 17 en el shell mientras el proyecto exige Java 25 y Maven compiló con release 25; la procedencia del toolchain debe quedar explícita.
- No existe evidencia local de fixtures de Coordinador en los tamaños solicitados ni de un entorno MinIO/SQL Server/Keycloak disponible para repetirlos.
- La documentación LB-008 contiene bloques históricos y un addendum vigente; se deben citar por fecha/estado y no mezclar resultados.

## Test plan

Referencia `TEST_PLAN.md`. Esta fase es análisis documental/contractual: no crea un RED Java ni altera comportamiento. Los gates funcionales de cualquier work item hijo se derivan de la fuente contractual aplicable.

## Rollback y stop conditions

No se cambia código ni proveedor en P0. Ante evidencia insuficiente, se registra `BLOCKED_BY_MISSING_EVIDENCE` con archivo/owner/impacto; ante fuentes incompatibles, `CONTRACT_CONFLICT`; ante test que contradiga contrato, `TEST_CONTRACT_CONFLICT`.

## Deuda conocida y validación manual

Se revisarán TD/MV citadas por los hallazgos. La validación manual de integraciones externas queda `NOT_RUN` mientras no exista ambiente autorizado y fixture reproducible.

## Definition of Ready

**NOT_READY para implementación; READY para CONTRACT_ANALYSIS.** Los repositorios/snapshots iniciales, owner/consumer y fuentes canónicas están identificados. Faltan cerrar la matriz detallada frontend↔backend↔DB, la reconciliación Java 25/toolchain, la disponibilidad de providers reales y cualquier decisión contractual necesaria para un cambio.

## Addendum de ejecución — 2026-10-18

- El backend quedó alineado por `git merge --ff-only origin/develop` en `72312d7c60449cd0118b8d32b8da02ad1eb0074d`; se conservaron todas las modificaciones locales preexistentes.
- Se abrió el work item hijo `P0-SEC-001-coordinador-asignatura-ownership` para el hallazgo confirmado de ownership. Su mitigación Application fue implementada con RED → GREEN y sin cambiar wire HTTP, OpenAPI ni SQL.
- La evidencia completa está en `VALIDATION.md`. El resultado no habilita `READY_FOR_P1_MINIO`: la integración SQL Server y la ejecución funcional del frontend siguen `NOT_RUN`/`BLOCKED`, y la defensa equivalente dentro de los providers DB requiere decisión del owner de DB.

## Addendum de auditoría multirrepositorio — 2026-10-20

- Se completó el inventario de las copias backend, frontend, DB e Infra visibles, incluyendo remotos alternos, ramas, PRs #24/#25/#26/#27/#28/#29, worktrees, stashes, backups, logs y artefactos generados. El detalle está en `CONSOLIDATION_AUDIT_2026-10-20.md`.
- El resultado es `CONSOLIDATION_STATUS = PRESERVED_NOT_INTEGRATED`: se preservaron divergencias y cambios locales; no se ejecutó `reset`, `clean`, prune, merge, force-push, publicación ni borrado.
- La suite CI de la copia frontend F1 pasó con `807/807` y cobertura statements 87.76%, branches 70.68%, lines 88.99%. Esto no sustituye integración backend/DB/E2E.
- Frontend y DB tienen dos familias de remotos con divergencia de autoridad (`legacy-*` frente a `origin`). No se elige una por suposición; requiere decisión del owner.
- El PEP permanece clasificado como laboratorio/TARGET no integrado. No se añadió starter, filtro ni wiring al runtime actual.
- El work item sigue `NOT_READY` para implementación y `READY_FOR_P1_MINIO = NO`; solo queda autorizada la documentación en esta carpeta hasta abrir work items hijos con contrato y DoR.
