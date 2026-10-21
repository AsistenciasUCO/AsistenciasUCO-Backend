# TEST_PLAN — P0-E: Consolidación funcional y restauración del entorno local

## Fuentes

`P0-E/PLAN.md`, `AGENTS.md`, `SOURCE_OF_TRUTH.md`, `LINEA_BASE.md`, OpenAPI canónico, contratos DB liberados, runtime Keycloak, `PROFESSOR_DECISION.md` de LB-004B.2, `TESTING_STANDARD.md` y `VALIDATION_RUNBOOK.md`.

## Behavioral Matrix

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|---|
| P0E-01 | Las cuatro rutas oficiales son repositorios correctos | HEAD/refs/estado por repositorio | Rutas accesibles | Leer metadata Git y remotos | Repositorio, rama, HEAD, cambios, stashes y refs registrados | Evidencia reproducible sin mutación | Ejecutar desde cuarentena o mezclar repositorios | documental |
| P0E-02 | Los cambios recuperados no se duplican ni se pierden | Commit equivalente, exclusivo o ausente | Refs locales/remotas disponibles | Comparar ancestry y contenido | Matriz por commit y repositorio | Cada cambio queda `PRESENT`, `RECOVERED`, `DUPLICATE` o `BLOCKED` | Cherry-pick duplicado o pérdida de cambio local | contract-analysis |
| P0E-03 | Los tres SQL pendientes permanecen seguros | Fixtures de paginación | Worktree `db-dev-consolidation` | Comparar contra HEAD | Tres archivos y diff exacto | Preservados; no alteran schema/datos reales | Integrar una migración no aprobada | DB fixture |
| P0E-04 | El runtime usa puertos/configuración coherentes | Backend fuera de Docker con providers locales | SQL/Keycloak/MinIO/ClamAV controlados | Arrancar en orden y consultar health | Puertos, endpoints, bucket, clamd, logs | Sin duplicados; valores por variables; secretos ausentes | Deriva `9000/19000`, `3310/13310` o defaults inseguros | provider |
| P0E-05 | Autenticación de prueba es reproducible | Usuarios E2E por rol | Realm/client/issuer y cuentas controladas | Obtener token y llamar API | Token válido, `idUsuario`, 401/403 esperados | `PASS` solo con Keycloak real y cuenta controlada | `invalid_grant`, client/realm equivocado o usuario inexistente | E2E |
| P0E-06 | Docente conserva grupos sin horario | Docente con grupos y sin horarios | Fixture DB/contrato disponible | Consultar materias/grupos/horarios | Respuesta y UI | Grupos visibles; horarios solo donde existen | Filtrar grupos por existencia de horario | E2E |
| P0E-07 | Reclamos/soportes conservan ownership y estado | Estudiante crea/sube; docente revisa | Binding DB y endpoints aprobados | Ejecutar upload/consulta/validación | Persistencia, malware scan, autorización | `PASS` solo con DB+MinIO+ClamAV reales | Acceso ajeno, archivo sin scan o estado no persistido | E2E |
| P0E-08 | Roles no cruzan ownership | Coordinador/admin y recurso ajeno | Tokens/fixtures controlados | Leer y mutar recursos | 401/403/404/501 según contrato | Código y ownership coinciden con contrato | IDOR, permiso ampliado o error no seguro | seguridad/E2E |
| P0E-09 | Validación técnica mantiene gates | Backend/frontend afectados | JDK/Node/Docker disponibles | Ejecutar comandos oficiales | Exit code, tests, cobertura, skips | Gates reales registrados; no históricos | Marcar PASS por compilación o mock | build |
| P0E-10 | El arranque local es único, idempotente y reversible | Servicios ya activos o apagados | `.env` locales presentes; sin secretos en el script | Ejecutar `-Check`, arranque y `-Stop` | Detecta puertos, reutiliza servicios, inicia solo procesos faltantes y detiene solo PIDs registrados | Duplicar contenedores/procesos o matar procesos ajenos | operacional |

## Integration Requirement

- REAL_PROVIDER_REQUIRED: **YES** para SQL Server, Keycloak, MinIO y ClamAV; **NO** para la clasificación Git y unit tests.
- ENVIRONMENT: Docker local controlado, SQL Server `gestionasistenciadb`, Keycloak realm `asistencias-uco`, MinIO bucket privado `asistencias-soportes`, ClamAV `clamd`, backend `8080`, frontend `4200`.
- MOCK_SUFFICIENT: **NO** para cualquier afirmación de integración/provider; sí para lógica unitaria aislada ya cubierta.
- WHY: los mocks no prueban contratos SQL, issuer/client/usuarios, credenciales S3 ni protocolo clamd.

## Negative / Boundary Coverage

Incluir credencial inválida sin registrar el secreto, client/realm incorrecto, token ausente/expirado, 401/403/ownership ajeno, grupos sin horario, bucket/prefix no autorizado, archivo EICAR, ClamAV no disponible, MinIO con credenciales inválidas, tamaño límite, página posterior al final y procesos/puertos duplicados.

## Side Effects / Rollback

Fixtures sintéticos deben tener cleanup explícito y no tocar datos reales. No eliminar volúmenes persistentes. Usuarios E2E solo se crean/reconcilian con scripts autorizados y flags explícitos; nunca se resetean usuarios institucionales. Los procesos iniciados por la tarea deben identificarse por PID y detenerse de forma acotada.

## Pruebas RED requeridas

Para cambios funcionales recuperados: usar el test/commit existente como candidato, congelar el contrato/fixture y registrar hash/commit antes de integrar. No aplica RED nuevo a la fase de inventario; cualquier fallo de provider se clasifica ambiental, no como RED funcional.

## RED_SNAPSHOT

No aplica todavía al preflight documental. Se completará antes de cada implementación autorizada de Backend, Frontend o DB; ningún implementador puede editar el RED congelado.

## Congelación

Pendiente de decisión del owner de Frontend y DB y de aprobación de los contratos afectados. La autoridad remota no se infiere del nombre de la carpeta o de una rama legacy.

## Integración y E2E

La secuencia será: SQL Server/schema/fixtures → Keycloak/seed controlado → MinIO/ClamAV/policy → Backend → Frontend → smoke HTTP/UI → recorridos por rol → verify/build/coverage. Cada skip, proceso no disponible, cuenta faltante o dependencia externa se registra como `NOT_RUN`/`BLOCKED`, nunca como `PASS`.

## Falsos positivos que deben evitarse

No usar documentación histórica como ejecución actual, no confundir `200` de health con contrato de negocio, no tratar mock como provider real, no aceptar token de otro realm/client, no usar `window.open` sin Bearer para descargar, no validar solo presencia visual de grupos y no cerrar soportes por un simple upload sin persistencia/ownership/revisión.
