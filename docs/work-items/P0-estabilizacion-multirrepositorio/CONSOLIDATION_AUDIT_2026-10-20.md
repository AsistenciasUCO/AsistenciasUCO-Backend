# Auditoría de consolidación multirrepositorio — 2026-10-20

## Dictamen ejecutivo

`AUDIT_STATUS = PARTIAL_WITH_BLOCKERS`

`CONSOLIDATION_STATUS = PRESERVED_NOT_INTEGRATED`

`READY_FOR_IMPLEMENTATION = NO`

`READY_FOR_P1_MINIO = NO`

La misión P0 se atendió como auditoría y preservación. Se inventariaron las copias observables de backend, frontend, DB e infraestructura, sus remotos, ramas, worktrees, PRs, artefactos generados, backups y logs. Se conservaron los cambios locales y las líneas de trabajo divergentes; no se hizo `reset`, `clean`, `checkout` destructivo, merge, force-push, publicación ni borrado de datos.

La evidencia permite identificar candidatos de consolidación, pero no autoriza elegir un remoto canónico para frontend/DB, integrar PRs abiertos, promover el laboratorio PEP, cambiar contratos ni eliminar worktrees o logs. El work item vigente solo permite documentación bajo esta carpeta.

## Autoridad y alcance aplicado

- Work item vigente: `P0-estabilizacion-multirrepositorio`.
- Clase de cambio: `CONTRACT_ANALYSIS`.
- Rutas permitidas: `docs/work-items/P0-estabilizacion-multirrepositorio/`.
- Rutas no modificadas: código backend/frontend, SQL, OpenAPI, `pom.xml`, infraestructura, `.env*`, worktrees, backups y datos externos.
- Estados usados: `PASS`, `NOT_RUN`, `BLOCKED_BY_MISSING_EVIDENCE`, `BLOCKED_BY_ENVIRONMENT` y `SECURITY_FINDING`.
- Toda evidencia histórica se conserva como histórica; los resultados de este documento están fechados 2026-10-20 y no sustituyen resultados anteriores.

## Inventario y decisión de preservación

### Backend

Repositorio local: `C:\Users\josev\AsistenciasUCO\AsistenciasUCO`.

- Remoto `origin`: `https://github.com/alejovh12/AsistenciasUCO.git`.
- Remoto `upstream`: `https://github.com/AsistenciasUCO/AsistenciasUCO-Backend.git`.
- HEAD local: `d0f170b8a5bf`, rama `codex/p0-sec-001-coordinator-ownership`.
- `origin/develop`: `5c06087d`, actualizado 2026-10-19.
- `upstream/develop`: `253c3c95`; queda 23 commits detrás de `origin/develop`.
- El checkout raíz queda 28 commits detrás y 2 commits delante de `origin/develop`.
- Estado raíz preservado: 9 elementos modificados/no rastreados, incluyendo scripts y configuración de Keycloak, work items de paginación/E2E, `frontend-lb004d-working` y `w/`. No se descartó ninguno.
- Se observaron 22 worktrees backend. El worktree `w/` conserva dos documentos no rastreados; no se eliminó.
- El stash `stash@{0}` (`P0-B pre-canonical remote protection 2026-10-18`) se conservó.

La rama `codex/stabilize-lb004d-backend` contiene 11 commits únicos de estabilización LB-004D, y `jose-valencia/lb-004d-b1-support-binding` contiene esos commits más el cambio de estado de estudiante. Son candidatos de revisión, no integración automática. El checkout raíz P0-SEC no se trató como candidato funcional más nuevo.

### Frontend

Se identificaron dos familias remotas y varias copias locales.

- Copia oficial bajo OneDrive: `C:\Users\josev\OneDrive\Documentos\Front_Asistecias\AsistenciasUCO-Frontend`.
- Remoto histórico `legacy-frontend`: `https://github.com/shesho22/AsistenciasUCO-Frontend.git`.
- Remoto de organización `origin`: `https://github.com/AsistenciasUCO/AsistenciasUCO-Frontend.git`.
- `legacy-frontend/develop = cdb9bd74`, 22 commits delante de `origin/develop = bd2120f5`.
- La copia OneDrive está en `jose-valencia/maint-004-frontend-utc-v2-integration`, HEAD `b4368d1`, 30 commits detrás de `legacy-frontend/develop`, con 3 modificaciones locales. Se conservaron.
- Copia F1 preservada: `C:\Users\josev\AsistenciasUCO\AsistenciasUCO\frontend-lb004d-working`, rama `jose-valencia/lb-004d-f1-support-binding`, HEAD `fac2f640`, limpia, aproximadamente 47.619 archivos por incluir dependencias/artefactos generados. Contiene 16 commits sobre la base `06f70c`, incluidos merge F1 con develop, portal de estudiante, estados/gates y grupos docentes.
- También existen snapshots p0b/p0d, `fe-stab`, `lb004d-frontend` y `reconcile-frontend-2026-10-18`. Se preservaron sus diferencias.

Conclusión: `legacy-frontend` es la referencia con mayor avance observable, pero la elección entre `legacy-frontend` y `origin` es una decisión de owner. No se reescribieron remotos ni se promovió F1.

### DB

- Copia OneDrive: `C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db`.
- Remoto `legacy-db`: `https://github.com/johnjduque/gestion-asistencia-db.git`.
- Remoto `origin`: `https://github.com/AsistenciasUCO/AsistenciasUCO-DB.git`.
- La copia local está en `develop`, HEAD `32b70ebe`, y conserva el untracked `_codex-pr37/` y dos stashes.
- `origin/develop = 32b70ebe`; `legacy-db/develop = c35293b8`, 16 commits delante. La documentación y el badge del proyecto apuntan al repositorio de `johnjduque`; la duplicidad de alias no se resolvió por suponer autoridad.
- `_codex-pr37/` es una copia completa, rama `codex/reconcile-db-2026-10-18`, HEAD `0dd0a705`, limpia; se preservó.
- Se observaron worktrees DB limpios, worktrees con fixtures modificados y dos registros prunables cuya carpeta gitdir ya no existe. No se ejecutó `git worktree prune`.
- Backups `.bak` observados en `C:\Users\josev\AsistenciasUCO\db-backups`: 7 archivos, incluidos respaldos `maint003l`, `maint003k`, fase 1 y UTC-D06. No se restauraron ni eliminaron.

Conclusión: la autoridad DB y la protección del contrato SQL requieren decisión/versionado del owner DB. No se sincronizó `develop`, no se fusionaron fixtures y no se tocó el esquema.

### Infraestructura

Existe el repositorio separado `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Infra`, remoto `https://github.com/AsistenciasUCO/AsistenciasUCO-Infra.git`, HEAD `a544c49`, y el worktree candidato `AsistenciasUCO-Infra-p0d`, rama `codex/p0-d-infra-validation`, HEAD `05530ec`. El candidato está divergente (`ahead 3 / behind 2` frente a su `origin/develop`) y se preservó.

El backend también contiene `infra/keycloak`, `infra/observability` e `infra/files`; no se confundieron con el repositorio Infra separado.

## PRs y promoción

| PR | Estado observado | Clasificación P0 | Decisión |
|---|---|---|---|
| #24 | Cerrado, `merged=false`, draft; laboratorio MVC/PEP | Experimento parcial, no producción | Preservar; no promover |
| #25 | Cerrado, `merged=false`, draft; ownership de Coordinador | Mitigación aislada | Preservar evidencia; no asumir integración |
| #26 | Cerrado, `merged=true`; reconciliación P0 backend | Integrado en `origin/develop` por `27262769` | No duplicar |
| #27 | Cerrado, `merged=true`; MinIO/ClamAV LB-004C | Integrado en `origin/develop` por `e6c4df68` | Mantener hallazgos operativos |
| #28 | Abierto, draft; análisis de consumidores LB-004D | Documento D0 dependiente de D1 DB | No implementar desde este P0 |
| #29 | Abierto, draft; backend LB-004D B1 | Candidato funcional, dependiente de contratos | No fusionar |

PR #27 conserva un hallazgo operativo relevante: el compose heredado de MinIO expone un bind amplio y hubo credenciales hardcodeadas en historial. No se reproducen valores secretos en este informe; la rotación/revocación queda como acción del owner y no se ejecutó.

## Seguridad y PEP

### AS-IS comprobado

El runtime backend vigente usa Spring Security, JWT/Keycloak, scope institucional y ownership en Application. En el checkout actual no hay starter, selector ni wiring PEP en `pom.xml`, `src/main` o `src/test`.

### TARGET/propuesta no promovida

Las ramas/laboratorios `experiment-security-baseline-mvc`, `feat/filtro-seguridad-pep-baseline` y `test-integracion-seguridad` contienen filtros/adaptadores PEP experimentales, configuración por propiedades y clientes HTTP directos. No son el AS-IS ni una certificación de producción. No se añadió dependencia ni se cambió `SecurityConfig`.

### SECURITY_FINDING

En el laboratorio no integrado se observan tipos de riesgo que deben sanearse antes de cualquier promoción: logging potencial de endpoint/body/respuesta PEP, transporte HTTP inseguro por defecto y cliente HTTP embebido dentro del filtro. Se reportan archivo/tipo de hallazgo, no valores. La existencia en una rama no integrada no constituye por sí sola un defecto del runtime actual.

## Higiene y artefactos generados

Se observaron `.angular`, `node_modules`, `dist`, `coverage`, `target`, logs raíz y caches en varias copias. Las mediciones incluyen worktrees anidados y dependencias generadas; no deben interpretarse como tamaño de source canónico.

Los logs raíz `e2e-*` tienen fecha 2026-10-11 y corresponden a redirecciones de procesos/harness, no a una fuente de contrato. `.gitignore` y `.dockerignore` ya cubren `*.log`, `target`, caches y salidas principales. No se eliminaron logs ni se modificaron scripts porque la ruta de higiene requiere un work item hijo y una lista exacta de artefactos, con archivo histórico si procede.

Los dos worktrees DB marcados como prunables no se repararon ni eliminaron: antes debe confirmarse que no contienen cambios únicos o referencias recuperables.

## Validación 2026-10-20

| Comprobación | Resultado | Evidencia/limitación |
|---|---|---|
| Backend `git diff --check` | `PASS` | Sin salida ni errores en el checkout P0 al momento de la comprobación |
| Java del shell | `BLOCKED_BY_ENVIRONMENT` | `java -version` reporta 17.0.18 |
| Maven wrapper | `PASS` contextual | `mvnw.cmd -v` usa Maven 3.9.15 con runtime Java 25 (`C:\Program Files\Java\jdk-25`); la discrepancia de toolchain queda documentada |
| Frontend build development | `PASS` | `npm run build -- --configuration=development`, copia F1, exit code 0 |
| Frontend build production | `PASS` | `npm run build -- --configuration=production`, copia F1, exit code 0 |
| Frontend `test:ci` | `PASS` | Chrome Headless 155: `807/807`; statements 87.76%, branches 70.68%, lines 88.99%, exit code 0 |
| Backend `verify` en worktree limpio `origin/develop` | `PASS` | `5c06087d`, Java 25, `1581` tests, 0 fallos/errores/skips, JaCoCo line 90.85%, branch 79.97%, exit code 0, `BUILD SUCCESS` |
| SQL Server / `-Pintegration verify` | `NOT_RUN` | No se dispuso de DB controlada y fixtures autorizados |
| Docker/MinIO/ClamAV/Keycloak/Azure | `BLOCKED_BY_ENVIRONMENT` / `NOT_RUN` | Docker Desktop Linux engine no disponible; servicios reales no se iniciaron |
| E2E real 300/1000/5000 y validación manual | `NOT_RUN` | Requiere entorno controlado, contrato y fixture versionado |

No hay procesos de backend ni recursos Docker activos identificados al cierre; tampoco se eliminaron volúmenes, imágenes, contenedores o redes.

## Acciones requeridas antes de una consolidación real

1. Owner de frontend: decidir si `legacy-frontend` o `origin` es la autoridad; registrar la decisión antes de integrar F1.
2. Owner de DB: decidir la autoridad entre `legacy-db` y `origin`, revisar los 16 commits pendientes y congelar el contrato SQL aplicable.
3. Abrir work items hijos para triage de los 9 cambios del checkout backend, `w/`, `_codex-pr37/`, fixtures DB modificados y copia F1; comparar commits/archivos antes de limpiar.
4. Para LB-004D, resolver D1 DB y la matriz D0/#28 antes de considerar #29; no usar el PR como fuente contractual única.
5. Crear un work item de higiene para archivar/eliminar de forma recuperable logs y dependencias generadas, con lista exacta y verificación de cambios únicos.
6. Repetir backend `verify`, `-Pintegration verify`, MinIO/Keycloak/Azure y E2E en ambiente controlado; registrar cada skip, exit code y cobertura.
7. Solo después de DoR y RED aprobados, ejecutar consolidación selectiva en ramas nuevas; sin force-push, merge directo ni modificación del esquema DB desde este P0.

## Cierre de esta auditoría

La auditoría documental queda actualizada, pero el work item no se cierra como funcional: hay conflictos de autoridad entre remotos frontend/DB, PRs abiertos dependientes, worktrees/cambios locales que requieren triage y validaciones externas no ejecutadas. El siguiente paso autorizado es obtener las decisiones de owners y abrir work items hijos; no existe autorización implícita para borrar, fusionar, publicar o cambiar contratos.
