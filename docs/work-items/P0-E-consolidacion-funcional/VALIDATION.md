# P0-E — VALIDATION

Estado: `OPEN` — validación local reproducible completada; consolidación funcional entre repositorios permanece `NOT_READY`.

Fecha de ejecución: `2026-10-20` (`America/Bogota`).

## Alcance y protección

- Se validaron los checkouts oficiales Backend, Frontend, DB e Infra.
- Se conservaron los cambios locales existentes. No se ejecutaron `reset`, `checkout` destructivo, `merge`, `push`, `force-push`, eliminación de volúmenes ni cambios de esquema.
- Las tres modificaciones SQL permanecen sin commit en `db-dev-consolidation` y no se integraron a `develop`: `test/fixtures/dev/dev_pagination_cleanup.sql`, `test/fixtures/dev/dev_pagination_fixture.sql` y `test/fixtures/dev/dev_pagination_validate.sql`.
- No se registraron passwords, secretos, tokens JWT, datos personales ni identificadores de archivos generados.

## Inventario Git

| Componente | Checkout / rama | Resultado |
|---|---|---|
| Backend | `C:\Users\josev\AsistenciasUCO\AsistenciasUCO` / `codex/p0-sec-001-coordinator-ownership` | `PASS`; cambios locales preservados |
| Frontend | `C:\Users\josev\OneDrive\Documentos\Front_Asistecias\AsistenciasUCO-Frontend` / `jose-valencia/maint-004-frontend-utc-v2-integration` | `PASS`; tres archivos modificados preservados |
| DB | `C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db` / `develop` | `PASS`; sin cambios tracked locales |
| DB worktree | `...\gestion-asistencia-db.worktrees\db-dev-consolidation` / `feat/maint-003k-db-dev-consolidation` | `PRESERVED_NOT_INTEGRATED`; tres SQL modificados |
| Infra | `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Infra` / `main` | `PASS`; sin cambios locales reportados |

Se ejecutó `git fetch` sobre los remotos disponibles de los cuatro repositorios. Todos terminaron con exit code `0`; no se alteró ningún working tree.

## Proveedores locales

- SQL Server: contenedor activo en `127.0.0.1:1433`; base esperada `gestionasistenciadb`; conectividad `PASS`; cinco vistas requeridas presentes. La consulta posterior enumeró los procedimientos reales de soporte existentes; no se asumieron nombres de procedures no comprobados.
- Keycloak: contenedor activo en `127.0.0.1:8081`; `validate-keycloak.ps1` terminó con `0` fallos críticos. Persisten seis warnings documentales por roles legacy; no se eliminaron.
- MinIO: servicios recreados mediante el compose canónico `backend/infra/files/compose.yaml`, publicado en `127.0.0.1:9000`; healthcheck `healthy`. Se reconciliaron las credenciales de aplicación del servicio local en memoria operativa, sin escribir secretos en el repositorio.
- ClamAV: servicio canónico publicado en `127.0.0.1:3310`; healthcheck `healthy`. La sonda EICAR devolvió `Eicar-Test-Signature FOUND`, resultado esperado para detección.
- Backend: proceso activo en `127.0.0.1:8080`; `/actuator/health` respondió `200`.
- Frontend: servidor del checkout oficial activo en `127.0.0.1:4200`; `/` respondió `200`.

## Backend

Comando:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Resultado: `PASS`, `BUILD SUCCESS`, `1544` tests, `0` failures, `0` errors, `0` skipped; ArchUnit incluido; JaCoCo reportó `All coverage checks have been met.`

Pruebas reales dirigidas de providers:

```powershell
.\mvnw.cmd -B -ntp -Pintegration '-Dtest=__P0E_NoUnitTests__' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dit.test=ClamAvMalwareScanAdapterIT,MinioFileStorageAdapterIT,ArchivoUploadDownloadFlowIT' verify
```

Resultado: `PASS`, `11` tests, `0` failures, `0` errors, `0` skipped; ClamAV real, MinIO real, upload/download real y respuesta `404` para archivo inexistente.

Dos intentos iniciales se clasifican como `NOT_RUN` metodológico: uno separó incorrectamente el parámetro `-Dit.test` en PowerShell y otro usó `-DskipTests`. No se contabilizan como fallos de producto.

## Frontend

Comandos ejecutados en el checkout oficial:

```powershell
npm run build -- --configuration=development
npm run verify
```

Resultados:

- Build development: `PASS`.
- `test:ci`: `PASS`, `341` pruebas ChromeHeadless exitosas.
- `coverage:realtime:check`: `PASS`, línea `95.38%` (umbral `90%`), ramas `83.95%` (umbral `80%`).
- Build production incluido en `verify`: `PASS`.

Los warnings de specs sin expectations y de una prueba que simula `network down` fueron emitidos por las pruebas y no produjeron fallos.

## E2E HTTP real

Comando:

```powershell
.\scripts\e2e\validate-pagination-e2e.ps1 -BackendBaseUrl 'http://127.0.0.1:8080'
```

Resultado: `E2E_VALIDATION_OK`.

- Docente de paginación 300: horarios `200/1`, estudiantes `200/300`, sesiones v2 `200/6`, UTC `4`.
- Docente de paginación 1000: horarios `200/1`, estudiantes `200/1000`, sesiones v2 `200/4`, UTC `4`.
- Docente de paginación 5000: horarios `200/1`, estudiantes `200/5000`, sesiones v2 `200/4`, UTC `4`.
- Ownership cruzado: `403`.
- Directorio coordinador: `200`, 20 elementos, `totalItems=5003`.
- Acceso docente al directorio general: `403`.
- La adquisición de tokens OIDC de las cuentas de prueba utilizadas terminó correctamente; el `invalid_grant` no se reprodujo en este recorrido.

## Arranque local único

Se añadió `scripts/start-local-stack.ps1`. El script no contiene credenciales: comprueba rutas y `.env`, reutiliza los puertos ya activos, inicia solo las dependencias que no estén escuchando y registra en un archivo temporal únicamente los PID de procesos que él mismo inicia.

Comandos ejecutados en el host Windows real:

```powershell
.\scripts\start-local-stack.ps1 -Check
.\scripts\start-local-stack.ps1
.\scripts\start-local-stack.ps1 -Stop
```

Resultados:

- `-Check`: `1433`, `8081`, `9000`, `3310`, `8080` y `4200` reportados como `LISTENING`; rutas oficiales y `.env` requeridos presentes salvo `infra/files/.env`.
- Arranque: `STACK|processes|REUSED_EXISTING` y `STACK|result|READY`; no creó contenedores ni procesos duplicados.
- `-Stop`: `STOP|processes|NONE_REGISTERED`; no detuvo procesos existentes porque no fueron iniciados por el script.
- Logs: backend en `logs/backend.out.log`; salida del frontend en `%TEMP%\asistencias-uco-local-stack-frontend.out.log` cuando el script lo inicia.

## Limitaciones y siguiente decisión

- `P0-E` no se cierra: la evidencia local es suficiente para restauración/validación, pero la integración de commits funcionales de Frontend/DB continúa bloqueada por la autoridad divergente `origin`/`legacy-*` documentada en la auditoría P0.
- Las tres SQL de paginación siguen siendo cambios de worktree preservados. Requieren decisión del owner DB y contrato/test plan antes de commit o integración.
- `infra/files/.env` canónico no existe en el checkout; por eso no se inventó ni se persistió un archivo con credenciales root. El servicio está operativo en la sesión actual, pero una secuencia nueva de arranque necesita que el operador provea ese archivo local ignorado, con valores fuera del repositorio.
- No se ejecutó una mutación E2E de asistencia, reclamo o soporte porque requiere fixtures/contratos DB autorizados y podría dejar datos persistentes. Las integraciones reales de archivos sí quedaron cubiertas por IT y los GETs E2E.
