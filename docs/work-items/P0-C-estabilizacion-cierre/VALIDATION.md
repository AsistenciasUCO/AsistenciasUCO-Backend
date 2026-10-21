# VALIDATION — P0-C

Fecha: 2026-10-20 (America/Bogota).

## Estado

`PARTIAL_CLOSURE` — los gates locales de backend/frontend y los smoke checks
disponibles están estabilizados. El cierre funcional completo sigue bloqueado
por deriva de credenciales E2E y configuración MinIO local.

## Git y preservación

- Backend actual: rama `codex/p0-sec-001-coordinator-ownership`, `HEAD bf6de9e`.
  El baseline P0-C era `bf33c79`; conserva `66cf9e0`, `0694ad4`, `bf33c79` y
  el commit local P0-C. Está 5 commits detrás y 6 delante de
  `upstream/develop`. No se publicó ni se preparó PR desde esta rama mezclada.
- Frontend `frontend-lb004d-working`: rama
  `jose-valencia/lb-004d-f1-support-binding`, `HEAD 95a202a`. El baseline
  P0-C era `a33876b`; conserva `fac2f640` y el commit local P0-C. Está 7
  commits delante de `origin/develop`; los logs generados permanecen fuera del
  commit. No se mezcló por trabajo apilado y autoridad remota separada.
- No se sobrescribieron cambios locales previos. Copias y worktrees quedaron
  intactos.

## Gates de tooling

| Comando | Resultado | Evidencia |
|---|---|---|
| `JAVA_HOME=JDK 25; .\mvnw.cmd -B -ntp verify` | PASS | Surefire 3.5.6; 1544 tests, 0 failures, 0 errors, 0 skipped; ArchUnit incluido; exit code 0 |
| `verify` JaCoCo | PASS | LINE 8217/9064 = 90,65%; BRANCH 1983/2462 = 80,59%; mínimos 80/70; checks cumplidos |
| `npm run verify` en `frontend-lb004d-working` | PASS | 807/807 ChromeHeadless 155; Statements 87,76%, Branches 70,68%, Functions 86,49%, Lines 88,99%; realtime LINE 95,06%, BRANCH 84,52%; production build; exit code 0 |
| `npm run build -- --configuration=development` | PASS | bundle Angular generado; exit code 0 |
| `npm run build -- --configuration=production` | PASS | `dist/gestio-asistencia-frontend`; exit code 0 |
| `powershell -File .\scripts\tests\test-local-logging.ps1` | PASS | `passed=5 failed=0` |

### Cambios de tooling

- `pom.xml`: Surefire recibe el agente `mockito-core 5.20.0` mediante
  `-javaagent` y conserva `@{argLine}` para JaCoCo. No se desactivaron forks,
  tests, ArchUnit ni umbrales.
- `frontend-lb004d-working/karma.conf.js`: `ChromeHeadlessCI` usa perfil
  efímero y `--remote-debugging-port=0`; no se cambiaron specs ni cobertura.
- La ejecución frontend sin permiso local falló por socket `localhost:9876`;
  con el permiso local requerido completó 807 tests. Es una restricción del
  entorno de ejecución, no un skip.

## Runtime local

- Docker accesible; SQL Server publicado en 1433 y Keycloak en 8081.
  `validate-keycloak.ps1` terminó exit 0, con 0 fallos críticos y 6
  advertencias por roles legacy.
- Backend arrancado con `scripts/run-local.ps1`, JDK 25: Tomcat 8080 y
  `/actuator/health` HTTP 200 durante el smoke. La comprobación final encontró
  el proceso detenido; los logs quedan centralizados bajo `logs/`.
- Angular arrancado en desarrollo: HTTP 4200, bundle generado y watch mode
  activo durante la prueba. La comprobación final encontró el proceso detenido.
- `logs/`: 2 archivos activos, 12.666 bytes medidos. Backups persistentes: 7
  archivos, 86.159.360 bytes. `DELETED=0`; no se borraron históricos,
  backups, datos ni worktrees.

## E2E y flujos reales

### PASS

Comando:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\e2e\validate-pagination-e2e.ps1 -BackendBaseUrl http://127.0.0.1:8080
```

Resultado exit code 0: docentes de paginación 300/1000/5000 con horarios
200/1, estudiantes 200/300, 200/1000 y 200/5000; sesiones v2 200; ownership
cruzado 403; directorio coordinador 200 con 20 elementos y total 5003;
directorio docente 403. El script no imprimió tokens/passwords/datos
personales y no modifica la DB.

### BLOQUEADO POR CREDENCIALES EXTERNAS

`utc-real-jwt-e2e.ps1 -BaseUrl http://127.0.0.1:8080` terminó exit code 1 antes
del flujo completo: Keycloak devolvió `invalid_grant` para la cuenta estudiante
configurada. La inspección no mutante confirmó que las cuatro cuentas E2E
existen y tienen el rol esperado; DOCENTE y COORDINADOR autentican,
ESTUDIANTE y ADMINISTRADOR no autentican con los valores locales. No se
inventaron ni resetearon credenciales.

## Integración real

1. `load-env.ps1; .\mvnw.cmd -B -ntp -Pintegration verify` terminó exit code 1
   porque `APP_SESIONES_V2_ENABLED=true` del `.env` contaminó dos tests que
   verifican el default-off (`SesionUtcActivationGuardTest`).
2. Repetición con `APP_SESIONES_V2_ENABLED=false` solo en el proceso: Surefire
   PASS (1544), Failsafe ejecutó 198 IT; exit code 1 con 2 failures y 8
   errors. SQL Server/paridad/persistencia pasaron; fallaron bordes MinIO y
   ClamAV por configuración externa.
3. El `.env` local apunta MinIO a `localhost:9000` y ClamAV a `3310`, mientras
   Docker publica host `19000` y `13310`. Con esos puertos, ClamAV funcionó;
   MinIO respondió `InvalidAccessKeyId` porque las credenciales locales no
   corresponden al usuario técnico activo del contenedor. No se modificó
   `.env`, Docker ni datos persistentes.

Esto queda como `VALIDATION_BLOCKED_BY_ENVIRONMENT` para los IT de archivo,
no como defecto de implementación: los reportes muestran MinIO/ClamAV
inalcanzables o credencial inválida, y el backend reporta fail-closed.

## Inventario de copias/worktrees

Medición de lectura del 2026-10-20; bytes lógicos medidos por ruta, sin
eliminación. `changes` es el número de entradas `git status --porcelain`.

| Ruta | Clasificación | Rama/HEAD | changes | bytes medidos |
|---|---|---|---:|---:|
| `AsistenciasUCO` | canónica, preservar | `codex/p0-sec-001.../bf33c79` | 15 | 2.804.697.923 |
| `frontend-lb004d-working` | copia activa frontend, preservar | `jose-valencia/lb-004d-f1-support-binding/a33876b` | 2 | 1.464.376.329 |
| `w` | estado local con cambios, preservar | `codex/local-lb004d-stabilization/b9d5d30` | 2 | 6.660.829 |
| `.workspace` | estado local sin autoridad, no consolidar automáticamente | misma rama backend | 15 | 1.117.907.233 |
| `lb004d-backend` | candidato PR B1, preservar | `jose-valencia/lb-004d-b1-support-binding/f45a521` | 0 | 7.683.266 |
| `be-stab` / `reconcile-backend-2026-10-18` | ramas backend limpias, preservar | `0d8fa9f` / `53fc48d` | 0 / 0 | 235.572.800 / 235.268.350 |
| `AsistenciasUCO-Frontend-p0b` | rama frontend con cambios, preservar | `b4368d1` | 3 | 2.224.723 |
| `AsistenciasUCO-Frontend-p0d` | rama frontend limpia, preservar | `e33ca3b` | 0 | 2.224.832 |
| `AsistenciasUCO-Frontend-p0d-develop` | rama frontend limpia, preservar | `0366fc5` | 0 | 1.135.807.243 |
| `lb004d-frontend` | candidato F1 apilado, preservar | `06f70c4` | 0 | 1.209.446.503 |
| `fe-stab` / `reconcile-frontend-2026-10-18` | ramas frontend limpias, preservar | `06f70c4` / `0366fc5` | 0 / 0 | 317.903.670 / 327.157.860 |
| `AsistenciasUCO-Infra` / `-p0d` | infra oficial y rama de validación, preservar | `a544c49` / `05530ec` | 0 / 0 | 71.153 / 3.309.363 |
| `gestion-asistencia-db` / `db-stab` / `lb004d-db` / `reconcile-db-2026-10-18` | DB oficial y ramas limpias, preservar | `25cf2e8` / `bdb2935` / `b698c76` / `034e6e8` | 0 | 2.399 / 2.601.968 / 2.586.042 / 2.428.333 |
| `db-backups` | backup no Git, preservar | 7 archivos | — | 86.159.360 |

Existen remotos/owners distintos y ramas con trabajo apilado; no se calculó
exclusividad destructiva ni se retiró ninguna ruta. Espacio recuperado: `0 B`.

## Limitaciones de cierre

- No se declara PASS para student/admin funcionales ni para el E2E UTC completo:
  faltan credenciales locales válidas, aunque los usuarios/roles existen.
- El perfil de integración completo sigue rojo por deriva de MinIO/ClamAV y
  por la variable de activación UTC del `.env`; requiere decisión del owner de
  entorno, no una modificación especulativa al backend.
- No se ejecutó Azure real: no hay evidencia local de identidad/endpoints
  Azure; permanece `NOT_RUN`.
