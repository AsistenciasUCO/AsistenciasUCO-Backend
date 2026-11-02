---
status: active
type: operational
scope: backend
owner: backend-team
last-reviewed: 2026-10-29
---

# Infraestructura local cotidiana

La infraestructura del Backend vive en `infra/`; SQL Server se administra desde el repositorio DB de John. Cada carpeta es un proyecto Compose independiente con volumenes externos o
nombrados que **se conservan**. El repositorio hermano `AsistenciasUCO-Infra` (CLI `infra.ps1`) se mantiene aparte y
no se usa para publicar. Los secretos viven fuera de Git (`.env` por carpeta y `%USERPROFILE%\.asistencias-security-lab\lab.env`).

**Prohibido** en el entorno cotidiano: `docker compose down -v`, `docker volume prune`, `docker system prune -a`.

| Carpeta | Proyecto Compose | Servicios y puertos (solo 127.0.0.1) |
|---|---|---|
| Repositorio DB de John | `gestion-asistencia-db` | SQL Server `sql_server_asistencias` 1433, base `gestionasistenciadb` (volumen `sql_server_asistencias_data`) |
| `security-baseline/` | `asistencias-security-lab` | Keycloak 19090, PDP 18080, PEP 18081, Redis, OPA, SurrealDB (Security Baseline 0.1.7) |
| `files/` | `asistencias-files` | MinIO 9000, ClamAV 3310 |
| `mail/` | `asistencias-mail` | SMTP de pruebas GreenMail 3025 (entrega nada a destinatarios reales) e inspeccion API 8085 |
| `observability/` | `asistencias-uco-observability` | Grafana 3000, Loki 3100, Tempo 3200, Prometheus 9090, Alloy 4318 |
| `keycloak/` | `asistencias-uco-security` | Keycloak **legacy** (alternativa explicita; no arranca por defecto) |

## Rutas oficiales

Solo existen tres copias de trabajo: Backend (`%USERPROFILE%\AsistenciasUCO\AsistenciasUCO`), Frontend
(`%USERPROFILE%\OneDrive\Documentos\Front_Asistecias\AsistenciasUCO-Frontend`) y DB
(`%USERPROFILE%\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db`). No se crean clones, worktrees,
carpetas de cuarentena/laboratorio ni bases duplicadas; `scripts\start-local-stack.ps1` rechaza rutas que lo parezcan.

## Seguridad: Security Baseline (predeterminado) y legacy (alternativa explicita)

| Modo | Servicios | Backend | Frontend |
|---|---|---|---|
| `baseline` (predeterminado) | Keycloak 19090, PDP 18080, PEP 18081 (`infra/security-baseline`) | `APP_ADAPTERS_SECURITY_PROVIDER=baseline` (valor por defecto de `application.yml`) | `SECURITY_PROVIDER=baseline` |
| `legacy` | Keycloak 8081 (`infra/keycloak`) | `-Provider legacy` | `SECURITY_PROVIDER=legacy` |

Legacy nunca es un fallback automatico: se elige explicitamente con `-Provider legacy`. El stack integrado se arranca con
`.\scripts\start-local-stack.ps1 [-Provider baseline|legacy]` (`-Check` solo diagnostica; `-Stop` detiene los procesos que inicio).

## Arrancar (orden: datos -> seguridad -> archivos -> backend -> frontend)

```powershell
docker start sql_server_asistencias
docker compose --env-file "$env:USERPROFILE\.asistencias-security-lab\lab.env" -f infra\security-baseline\compose.yaml up -d
docker compose --env-file infra\files\.env -f infra\files\compose.yaml up -d
docker compose -f infra\mail\compose.yaml up -d                 # SMTP de pruebas (outbox NOTIF/IDENT-001)
docker compose -f infra\observability\compose.yaml up -d   # opcional
.\mvnw.cmd -B -DskipTests package
.\infra\security-baseline\scripts\start-backend-baseline.ps1          # Provider baseline (predeterminado), puerto 8080
# Frontend (repositorio AsistenciasUCO-Frontend): npm ci; npm start   -> http://localhost:4200
```

`start-backend-baseline.ps1` carga el `.env` del backend y `lab.env` en el proceso sin imprimirlos; el backend usa el
login de runtime `uco_dev_runtime` (nunca `sa`). Legacy es una alternativa explicita: `-Provider legacy`
(`APP_ADAPTERS_SECURITY_PROVIDER=legacy`); nunca es un fallback automatico.

## Verificar

```powershell
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
python -I infra\security-baseline\scripts\provision_asistencias.py verify          # catalogo de acceso vs PDP
python -I infra\security-baseline\scripts\provision_asistencias.py verify-decisions
curl.exe -s http://127.0.0.1:8080/actuator/health
```

## Detener (sin perder datos)

```powershell
docker compose -f infra\observability\compose.yaml down
docker compose -f infra\mail\compose.yaml down
docker compose --env-file infra\files\.env -f infra\files\compose.yaml down
docker compose --env-file "$env:USERPROFILE\.asistencias-security-lab\lab.env" -f infra\security-baseline\compose.yaml down
docker stop sql_server_asistencias
```

## Recuperar

- Contenedor caido: repetir el `up -d` correspondiente (los volumenes se reutilizan).
- `minio-init` termina con codigo 0 (idempotente); relanzar con `up -d minio-init` si falta el bucket.
- SQL Server: el Compose oficial esta en el repositorio DB de John. No ejecutar `docker compose up`
  sin `--no-recreate` mientras se conserva el contenedor original.
- Pruebas que mutan estado (E2E de identidad, aprovisionamiento, perfiles) solo corren contra una base desechable
  (`asistencias-e2e-sqlserver`, 14333); `e2e_safety.py` bloquea `sql_server_asistencias`/1433.

## Sincronizacion de perfiles del PDP

`SECURITY_BASELINE_PDP_PROFILE_SYNC_ENABLED=false` (predeterminado). No cambiarlo a `true` hasta que Seguridad UCO
apruebe una identidad de servicio con autorizacion efectiva sobre la API administrativa del PDP
(ver `docs/security/runtime-security-provider-architecture.md`). Mientras tanto, las cuentas nuevas quedan
`PENDIENTE_SINCRONIZACION` y la SPA lo muestra; no hay ruta alternativa que conceda privilegios.

## Notificaciones y verificacion de correo (NOTIF-001/002A, IDENT-001)

El `.env` local del backend (fuera de Git) fija `APP_ADAPTERS_NOTIFICATION_PROVIDER=outbox`,
`OUTBOX_NOTIFICATION_SENDER=smtp`, catalogos `sqlserver`, `SMTP_NOTIFICATION_HOST=127.0.0.1`/`PORT=3025`,
`SMTP_NOTIFICATION_PLATFORM_URL=http://localhost:4200` y `SMTP_NOTIFICATION_TEST_MODE_ENABLED=true` (D15: todo
se redirige a `SMTP_NOTIFICATION_TEST_MODE_REDIRECT_TO`). GreenMail no reenvia: los correos se inspeccionan en
`http://127.0.0.1:8085/api/user/<correo>/messages`. Los enlaces de verificacion contienen un token: no se registran
en logs ni en evidencias. Un SMTP institucional o una cuenta de pruebas autorizada se evalua aparte; cambiar de
GreenMail a un SMTP real exige revisar primero `SMTP_NOTIFICATION_TEST_MODE_*`.
