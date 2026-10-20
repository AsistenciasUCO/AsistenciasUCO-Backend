# VALIDATION — P0-H: activar y validar la última versión funcional

## Dictamen

`LOCAL_EXPERIMENTAL_PASS` para la combinación candidateada y el flujo real local. `NOT_READY / CONTRACT_CONFLICT` para elevar D1/R2 a integración oficial: la decisión del owner DB sobre R1/R2 y la autoridad del remoto siguen pendientes.

La validación no cierra el contrato oficial ni modifica producción. No se copiaron secretos al repositorio. El work item permanece abierto para la decisión contractual y la evidencia manual de IntelliJ/UI.

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado | Evidencia sanitizada |
|---|---|---:|---|---|
| 2026-10-20 / backend actual | inventario de refs/ascendencia Git | 0 | PASS | backend `6893f4d`; candidato `f45a5213`; frontend `95a202a` y `fac2f640`; DB `b698c76` |
| 2026-10-20 / backend actual | snapshot de cambios locales | 0 | PASS | `.p0-h-snapshot-20261020/` y stash `c09a7c4d...` preservados |
| 2026-10-20 / frontend original | `git status`, refs y ascendencia | 0 | PASS | tres archivos locales preservados; `fac2f640` ancestro de `95a202a` |
| 2026-10-20 / backend integration | `.\mvnw.cmd -B -ntp verify` | 0 | PASS | 1641 tests, 0 failures, 0 errors, 0 skipped; build SUCCESS |
| 2026-10-20 / frontend integration | `npm.cmd run verify` | 0 | PASS | 807 SUCCESS; realtime check y production build PASS |
| 2026-10-20 / stack real | `.\scripts\start-local-stack.ps1 -Check` | 0 | PASS | 1433/8081/9000/3310/8080/4200 LISTENING; `infra/files/.env` MISSING |
| 2026-10-20 / E2E real | `support_binding_e2e.py main` con SQLCMD en memoria | 0 | PASS | 34 checks, 34 PASS, 0 FAIL, 0 SQL NOT_RUN |
| 2026-10-20 / E2E sesiones | `student_sessions_e2e.py` con SQLCMD en memoria | 1 | FAIL parcial | 15 checks: 11 PASS, 4 FAIL; Arquitectura de Software y 2 sesiones PASS, estado de asistencia no coincide con el fixture esperado |
| 2026-10-20 / DB local respaldada | fixture LB-004D + concurrencia | 0 | PASS | resultsets, ownership, rollback, mínimo privilegio y concurrencia PASS |

## Checks

| Nivel | Check | PASS / FAIL / NO APLICA / BLOQUEADO | Evidencia/motivo |
|---|---|---|---|
| Alcance | plan y preservación | PASS | snapshots y referencias candidateadas registrados |
| Arquitectura | ArchUnit/dependencias | PASS | incluido en `mvn verify`; 1641 tests pasan |
| Contratos | HTTP/DB/security | PASS local / BLOQUEADO oficial | E2E, metadata y negativos reales pasan; R1/R2 no se elevan a contrato oficial |
| Tests | RED → GREEN | PASS | tests candidateados ejecutados sin rebajarlos ni modificarlos para ocultar fallos |
| Coverage | líneas ≥80 %, ramas ≥70 % | PASS | backend line 91.23 %, branch 80.07 %; frontend line 88.99 %, branch 70.68 % |
| Integración | DB/provider real | PASS local | SQL Server, Keycloak, MinIO y ClamAV reales; no mocks |
| Build | Maven verify / frontend verify | PASS | ambos exit 0 |
| Seguridad | JWT/RBAC/ownership/secret hygiene | PASS local | 34 E2E PASS, negativos 401/403/404, EICAR rechazado; secretos no registrados |
| Runtime | procesos y wiring | PASS | PIDs nuevos desde checkouts originales; backend health 200 y frontend 200 |
| Manual UI | IntelliJ + login visual docente/estudiante | NOT_RUN parcial | helper sin ventana IntelliJ seleccionable; no se inyectaron contraseñas al registro del navegador |
| Docente / sesiones | Arquitectura de Software, grupo y dos sesiones | PASS parcial | E2E de sesiones devuelve materia y exactamente 2 sesiones; no se certifica el estado de asistencia por fixture inconsistente |
| Documentos | work item y trazabilidad | PASS | PLAN/TEST_PLAN/VALIDATION creados |

## Integridad de RED

No aplica RED nuevo. Los candidatos y sus tests se fijan por SHA; cualquier conflicto de contrato se devuelve al owner correspondiente.

## Bloqueantes y limitaciones

- `CONTRACT_CONFLICT: P0H-DB-001`: DB baseline v1 fija resultset público de cuatro columnas, mientras D1/R2 candidatea R1 con diez y R2 con veinticinco columnas de lectura. Requiere decisión del owner DB.
- `BLOCKED_BY_MISSING_EVIDENCE: P0H-INTELLIJ-001`: el helper de escritorio no expuso una ventana/app seleccionable de IntelliJ; el runtime equivalente sí quedó iniciado desde el backend original mediante `spring-boot:run`.
- `DATA_FIXTURE_MISMATCH: P0H-DATA-001`: `student_sessions_e2e.py` encontró detalles de asistencia en ambas sesiones; el contrato del harness exige SES-01 sin detalle y SES-02 `SJC`. No se limpiaron filas históricas ni se inventó un fixture correctivo.
- `infra/files/.env` no está presente en el checkout. Los servicios de archivos ya estaban levantados y sanos; un arranque limpio futuro requiere ese archivo local ignorado aportado por un operador autorizado.
- La evidencia histórica de P0-C/P0-E orienta la reproducción, pero no es resultado actual.
- El snapshot Git se conserva porque el índice del backend no permitió `stash`; la recuperación se hace con el patch/manifest, sin borrar backups.

## Evidencias remotas/manuales pendientes

IntelliJ/UI y decisión contractual D1/R2 siguen pendientes. El resto quedó comprobado: backend `b48ce40f` / PID `19136` / puerto 8080 / health UP; frontend `6bf5787` / PID `11244` / puerto 4200; DB `b698c76`; SQL Server, Keycloak, MinIO y ClamAV activos; no se creó PR publicado.

## Evidencia de integración y runtime

| Componente | Checkout/rama/SHA | Resultado |
|---|---|---|
| Backend | `jose-valencia/p0-h-backend-functional-integration` / `b48ce40f4bf99fbd17a09ad7be9245650f25400e` | proceso PID 19136 iniciado 16:53:59 -05:00; `Started AsistenciasUcoApplication`; Tomcat 8080; health 200 `UP` |
| Frontend | `jose-valencia/p0-h-frontend-functional-integration` / `6bf5787aacbb176d55aa6b99e021df87fb289e93` | `95a202a` funcional y `fac2f640191cab4c6942d2cfed2c18770d0c4378` ancestro; PID 11244 iniciado 16:54:57 -05:00; 4200 responde 200 |
| DB | `jose-valencia/p0-h-db-functional-integration` / `b698c76112490b91298d16ccd1fb6a793648adec` | `gestionasistenciadb` ONLINE; cuatro SP presentes; no se aplicó esquema desde P0-H |
| Proveedores | SQL Server 1433, Keycloak 8081, MinIO 9000, ClamAV 3310 | contenedores activos; MinIO/ClamAV healthy; `start-local-stack.ps1 -Check` PASS |

El frontend usa por defecto `http://<host>:8080/api/v1` en `src/environments/environment.ts`; la ruta docente sin Bearer respondió 401, confirmando el wiring HTTP real.

## DB y E2E real

La DMV de SQL Server confirmó W1=4 columnas, W2=4, R1=10 y R2=25, sin errores de metadata. El backup copy-only del 2026-10-19 pasó `RESTORE VERIFYONLY`; después se ejecutaron el fixture transaccional LB-004D y la concurrencia: ambos exit 0, rollback/limpieza comprobados.

El flujo real comprobado fue: estudiante sube PDF limpio → ClamAV acepta → MinIO guarda → SQL crea `DRAFT` → propietario descarga → estudiante radica con `soporteArchivoId` → SQL vincula `ATTACHED` → docente titular lista y descarga con SHA-256 idéntico. También pasaron negativos de 401/403/404, docente ajeno, otro estudiante del grupo, coordinador, administrador, segunda radicación, EICAR, tamaño >5 MiB, nombre >250 y ejecutable disfrazado.

La primera corrida tuvo 3 SQL `NOT_RUN` por no heredar `SQLCMDPASSWORD`; se repitió con la variable solo en memoria y terminó `34/34 PASS`, `0 FAIL`, `0 SQL NOT_RUN`. Las identidades E2E se reprovisionaron únicamente en Keycloak local y sus secretos permanecen fuera del repo.

El E2E de sesiones fue lectura real: `Arquitectura de Software` y sus dos sesiones se obtuvieron correctamente, pero terminó `11/15 PASS` porque los datos actuales no corresponden al estado de ausencia esperado por el fixture existente. El fallo queda separado del código recuperado; no se ejecutó `apply`/`cleanup` sobre esas filas.

## Estado de cierre

No se crea `CLOSURE.md`: la validación experimental local pasa, pero la Definition of Done de integración oficial no se cumple mientras siga abierto `P0H-DB-001` y no exista evidencia IntelliJ/UI completa. El siguiente paso es el dictamen del owner DB y la auditoría/cierre independiente.
