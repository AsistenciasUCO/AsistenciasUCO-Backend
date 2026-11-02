---
status: active
type: evidence
scope: backend+frontend+db+infra
owner: backend-team
last-reviewed: 2026-11-01
---

# Integración de la instalación ORIGINAL — evidencia 2026-11-01

Sin secretos, tokens ni enlaces con token en este documento. Los valores viven en `.env` / `lab.env` (fuera de Git).

## A. Git

| Repo | Rama local | SHA (completo) | Remoto usado para el fast-forward |
|---|---|---|---|
| Backend | `develop` = `b288ca5192ab487ef8bd31389cea32060c598c56`; cambios en `fix/ident001-confirmar-correo-baseline-20261101` (sin commit) | b288ca51 | `origin` (alejovh12) |
| Frontend | `develop` | `350bf1206c7e4d65fc90b8bd64c9a1aa6dcdb519` | `legacy-frontend` (shesho22) — `origin` (organización) está atrasado |
| DB | `develop` | `d1e0af85581276737889254cf81080f71312d9fc` | `legacy-db` (johnjduque) — `origin` atrasado |

Los tres `develop` locales eran ancestros directos del remoto: `merge --ff-only`. Sin push. `.codex/` y las evidencias FIX-003 intactas.

## C. Backups (fuera del volumen SQL): `C:\Users\josev\AsistenciasUCO-backups\20261101\`

- `gestionasistenciadb_pre_f1_20261101_212814.bak` (sha256 9372ac9a…), `gestionasistenciadb_pre_apply_*.bak` (d5b752bc…, justo antes de aplicar) y `master_pre_f1_*.bak` (logins). `COPY_ONLY, CHECKSUM, COMPRESSION`.
- `RESTORE VERIFYONLY` OK. Restauración real en un contenedor desechable: `DBCC CHECKDB` sin errores y 34.657 filas = base viva.
- `live_module_definitions_pre.txt` (definición de los 223 módulos antes), `backend.env.pre`, `vol_*surrealdb*.tgz`, `vol_*keycloak*.tgz`.

## D/E. SQL aplicado a `sql_server_asistencias` / `gestionasistenciadb`

La secuencia (125 archivos) se probó primero en la copia restaurada; `deploy_schema.ps1` completo NO se ejecutó en la base viva (re-ejecuta `03_parametro.sql`, que pisa `CatalogoParametro.valor`, y los seeds 11-15 son fixtures).

- Migraciones: 06 (índices únicos TipoPrograma; Area ya estaba), 07 NOTIF-001, 08 NOTIF-002A, 10 IDENT-001. Tabla `SolicitudIngresoGrupo` (HU053-055; no tenía migración, 68 módulos la referenciaban) + `zz_foreign_keys.sql` idempotente.
- Tablas nuevas: EstadoNotificacion, NotificacionSaliente, PlantillaNotificacion, PlantillaNotificacionMarcador, TokenVerificacionCorreo, SolicitudIngresoGrupo (42 → 48).
- 68 módulos con `CREATE OR ALTER` (24 distintos, 44 nuevos), incl. outbox, plantillas/contacto, IDENT-001, `usp_guardar_horarios_grupo(_interno)`, `uv_horario`/`uv_dia`.
- Retiro de código sin uso (solo SP/funciones, ninguna tabla/dato): migraciones 07_hu025, 08_eliminar_codigo_sin_uso (19 objetos) y 09. Backend y Frontend no los referencian; se aplicaron porque la suite DB exige `PUBLIC_CANONICAL_RESULTSET`. Reversión: `*_down.sql` o la definición previa.
- Grants (19 scripts de `schema/security`) y seeds de catálogo (01,02,04-10,16-28). `03_parametro` no se aplicó.
- Resultado: 247/247 módulos de la base viva idénticos al repo, 0 sobrantes. Datos: Usuario 5014, Grupo 5, Horario 3, EstudianteGrupo 6302 sin cambio; CatalogoParametro 30→34, mensajes 103→143, Estado 3→5.

## F. Security Baseline (0.1.7, proyecto `asistencias-security-lab`, sin renombrar)

- `provision_asistencias.py pdp` (aditivo, sin `--prune`): +2 recursos y +4 concesiones de IDENT-001 (`GET /configuracion/registro-estudiante`, `POST …/reenviar-verificacion`). Respaldo previo de volúmenes SurrealDB y Keycloak.
- Pendiente de decisión: 1 concesión ESTUDIANTE sobre `…/prerrequisitos` (HU025 descartada) y 3 recursos huérfanos sin endpoint; inertes, solo se revocan con `--prune`.
- `uco-docente` y `uco-estudiante` no iniciaban sesión con la clave de `lab.env`: se resincronizaron con `provision_asistencias.py users`.
- `verify-decisions` del repo FALLA por una limitación del verificador (obtiene los tokens una vez y expiran durante los 973 casos: 144 × 401). Con refresco de token: 973 decisiones, 0 fallos.
- Keycloak legacy (8081) y su volumen PostgreSQL: intactos y detenidos.

## G. NOTIF / IDENT-001 (E2E contra la base original, SMTP GreenMail, modo de pruebas ACTIVO)

Config: `.env` backend (backup `backend.env.pre`) con outbox+smtp+catálogos `sqlserver`, `SMTP_NOTIFICATION_PLATFORM_URL=http://localhost:4200`, `TEST_MODE_ENABLED=true` redirigiendo a un buzón de pruebas. GreenMail: `infra/mail/compose.yaml` (3025/8085, healthy).

| Caso | Resultado |
|---|---|
| Alta de estudiante → cuenta Keycloak deshabilitada, `emailVerified=false` | PASS |
| Correo `[PRUEBA]` entregado a GreenMail (no al estudiante); outbox `ENTREGADA` | PASS |
| Login antes de verificar | PASS (rechazado) |
| Token válido (enlace `#token=` en fragmento) → cuenta habilitada | PASS (tras corrección J1) |
| Token de un solo uso (reuso → VCE_002); token inválido | PASS |
| Token vencido → VCE_002 | PASS |
| Reenvío inmediato → 409 VCE_003; tras vencer → 200, token viejo revocado, nuevo válido; ya verificado → 409 VCE_005 | PASS |
| Logs: sin token, sin contraseña, sin correo ni documento de los estudiantes de prueba | PASS |
| Login posterior con la cuenta activada | FAIL/BLOQUEADO: Keycloak exige cambio de clave inicial y, además, el PDP no tiene perfil para cuentas nuevas (`SECURITY_BASELINE_PDP_PROFILE_SYNC_ENABLED=false`, política documentada: requiere identidad de servicio aprobada por Seguridad UCO). `auth/me` → 401 |

## J. Problemas detectados y corregidos

1. **IDENT-001 roto en modo baseline**: `ProfileSyncingIdentityProvider` no delegaba `confirmarCorreoCuenta` (cae en el `default` que lanza "no soportado") → `POST /public/verificar-correo` devolvía 500. Test RED añadido y corrección de una línea (`ProfileSyncingIdentityProvider.java`, `ProfileSyncingIdentityProviderTest.java`).
2. El launcher del backend escribía los logs en `%TEMP%`, mientras Alloy lee `infra/observability/logs`: Loki quedaba vacío. `start-backend-baseline.ps1` y `start-local-stack.ps1` apuntan ahora a `infra/observability/logs` (ignorado por Git). Loki recibe `service=asistencias-uco`.
3. `start-local-stack.ps1` arranca GreenMail (puerto 3025); `infra/README.md` documenta el componente.
4. GreenMail: el healthcheck inicial usaba `wget` (inexistente en la imagen); ahora comprueba el puerto SMTP.

## I. Pruebas

| Ámbito | Resultado |
|---|---|
| Backend `mvnw verify` (unit + ArchUnit + JaCoCo LINE≥80/BRANCH≥70) | PASS — 2988 pruebas, 0 fallos (tras J1); antes 2987 |
| Frontend `npm run verify` | PASS — 1356/1356 Karma, cobertura realtime 95,06 %/84,52 %, build de producción |
| DB, pruebas dirigidas en copia migrada: ident001 (20), notif002a (15), migración de catálogo (6), guardar_horarios_grupo (6), cruce horario (6), solicitud_ingreso (13), HU051 (10), runtime grants, public_objects (9), sec_baseline | PASS |
| DB, suite completa `test_summary.ps1` | INCOMPLETO. Despliegue limpio de `develop`: `deploy_schema.ps1` rc=0, 125 PASS / 5 FAIL, aborta en `test_usp_retirar_estudiante_grupo` (fixture HU051 ausente por orden de seeds; el mismo test pasa solo en la copia migrada). Copia migrada: aborta en `PROGRAM_UPSERT_CREATE` (la suite supone una base sembrada limpia). Hallazgo para el dueño de DB, no atribuible a esta integración |
| Backend ITs `-Pintegration` | NOT_RUN (escriben datos; requieren base desechable) |
| SonarCloud Quality Gate / checks remotos | NOT_RUN |
| F1 horarios: HTTP de escritura de franjas | NOT_RUN (lectura: `franjas` presente en `GET /grupos`; SP/vistas PASS) |
| FIX-002 titularidad, subida/descarga de archivos por HTTP | NOT_RUN (MinIO `live` 200, ClamAV `PING→PONG`, bucket conservado) |
| Métricas / logs / trazas | PASS (Prometheus target up, Loki con logs del servicio, Tempo con trazas de `verificar-correo`) |

## H. Docker (medido; ninguna imagen sustituida)

| Componente | Imagen | `image ls` | Contenido real | Decisión |
|---|---|---|---|---|
| SQL Server | mssql/server:2022-latest | 2,34 GB | — | Mantener (no hay variante ligera) |
| Backend | JRE 25 alpine multietapa, usuario 10001 | 421-585 MB | 130-208 MB | Ya óptimo; mantener |
| Frontend | nginx alpine multietapa | 104 MB | 29 MB | Ya óptimo |
| MinIO | imagen propia alpine (minio+mc compilados) | 406 MB | 107 MB | Mantener (más pequeña que la oficial) |
| ClamAV | 1.5.4-debian13-slim | 552 MB | 193 MB comprimido | Alpine 1.5.4 = 156 MB comprimido (−19 %): no justifica el cambio |
| Observabilidad | grafana 11.5.2 744 MB, prometheus 415, alloy 546, tempo 179, loki 151 | — | — | Sin variantes menores; mantener |
| Security Baseline 0.1.7 | pdp 659, pep 586, keycloak 756, opa 82, redis 57, surrealdb 481 | — | — | Mantener (0.1.6 queda como rollback) |
| GreenMail (nuevo) | greenmail/standalone:2.1.3 | 452 MB | — | Añadido (sin volumen) |

Docker `system df` antes → después: imágenes 36,34 → 36,79 GB (+GreenMail), volúmenes 1,697 → 1,727 GB, build cache 10,61 GB sin cambio. Reducción real posible, no ejecutada: 10,61 GB de build cache (regenerable) y ~12 GB de imágenes de otros stacks (kafka, pulsar, mongo, debezium…) y ~2 GB de 0.1.6 que requieren decisión explícita.

## K. Riesgos residuales y reversión

- 5 estudiantes de prueba permanecen en SQL y matriculados en "Grupo 001" (documentos 987601101-987601105, apellido "Evidencia Integracion", correos `prueba.e2e.1101a..e@uco.edu.co`); sus cuentas Keycloak quedaron deshabilitadas. No se borraron (regla: no borrar usuarios ni matrículas); la limpieza queda para revisión individual.
- Cuentas nuevas sin acceso hasta aprobar la sincronización de perfiles del PDP.
- Reversión SQL: parar el backend, `RESTORE DATABASE gestionasistenciadb FROM DISK='…pre_apply….bak' WITH REPLACE` (copiar el .bak al contenedor), o ejecutar los `*_down.sql` y re-aplicar las definiciones previas. `.env`: `backend.env.pre`. Keycloak/SurrealDB: volumen `.tgz`.
- `deploy_schema.ps1` no debe ejecutarse completo contra la base viva.

## L. Estado de arranque

SQL 1433, Keycloak 19090, PDP 18080, PEP 18081, Redis/OPA/SurrealDB, MinIO 9000, ClamAV 3310, GreenMail 3025/8085, Grafana 3000, Loki 3100, Tempo 3200, Prometheus 9090, Alloy 12345, Backend 8080 (baseline, outbox+smtp), Frontend 4200 (ng serve, CORS verificado): todos arriba.

**Dictamen: READY_FOR_MANUAL_TESTING** (con los hallazgos anteriores abiertos).
