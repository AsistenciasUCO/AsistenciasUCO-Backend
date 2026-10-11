# MAINT-003K — Addendum: consolidación SQL local de desarrollo

Fecha original: 2026-10-10. Actualización de estado: 2026-10-11.

**Nota de procedencia.** El prompt de la fase refiere un `DB_DEV_ADDENDUM.md` «adjunto» a este directorio. Ese archivo **no estaba disponible** en el repositorio ni en las rutas de descargas/adjuntos de la máquina al ejecutar.
Este documento recoge el alcance **tal como lo especifica el prompt de ejecución** (BLOCKED_BY_MISSING_EVIDENCE parcial, resuelto con el texto del prompt); si el addendum original difiere, prevalece el original y debe cargarse en este directorio.

## Alcance añadido a MAINT-003K

1. Inventario y respaldo verificado de la DB principal; confirmar la conexión real del backend (`.env`: `localhost:1433`, `gestionasistenciadb`).
2. Rama SQL de consolidación `feat/maint-003k-db-dev-consolidation` sobre `feat/cc-003g-01-public-user-plan-providers` (incluye UTC-D06 y CC-003G-01), con corrección del seed `14_grupos_sesiones.sql`.
3. Reconstrucción de `gestionasistenciadb` en `sql_server_asistencias` **solo desde scripts versionados** (nunca copiando una base temporal), con segundo despliegue para idempotencia.
4. Fixtures sintéticos reproducibles (300/1000/5000 estudiantes, UTC `DATETIME2(7)` con procedencia).
5. Certificación del backend (MAINT-003I incluido) contra esa DB.
6. Retiro de los artefactos SQL temporales (sin tocar MinIO, Keycloak ni observabilidad).
7. Publicación de la rama DB y PR Draft hacia `develop` (sin merge); publicación de los commits backend sin merge ni borrado de ramas.

## Fuera de alcance (sin cambios)
Frontend, Redis/MinIO, proveedor de identidad, merge a `develop`, borrado de ramas, producción.

## Resultado
Ver [PLAN.md](PLAN.md) (estados), [TEST_PLAN.md](TEST_PLAN.md) y [VALIDATION.md](VALIDATION.md); detalle SQL en el repo DB: `docs/work-items/MAINT-003K-DB-DEV-CONSOLIDATION/`.

## Estado del PR DB al 2026-10-11

El PR #36 del repositorio `johnjduque/gestion-asistencia-db` ya está cerrado/merged en `develop`, con merge commit `e2486194d42c6c7a649882a6957273742aa26536`. Por tanto, la redacción histórica de este addendum que decía “sin merge” ya no describe el estado actual; la autoridad vigente es el SHA merged y la compatibilidad comprobada en `sql_server_asistencias / gestionasistenciadb`. El backend no administró el merge ni modificó el esquema.

El diagnóstico funcional posterior, incluido el hallazgo de `usp_crear_facultad`, está en [BACKLOG_ADMINISTRATIVO.md](BACKLOG_ADMINISTRATIVO.md).

## Estado final del entorno local
- `sql_server_asistencias`: única base de usuario `gestionasistenciadb` (36 tablas, 56 vistas, 64 SP, 2 roles, 1 trigger) + fixtures de paginación (`DE0000…`).
- Retirados: contenedor `utc_d06_sqltest` y la base `gestionasistenciadb_fase1` (respaldados en `C:\Users\josev\AsistenciasUCO\db-backups\`).
- El «segundo contenedor temporal» mencionado en el prompt **no existía**: el segundo artefacto temporal era la base `gestionasistenciadb_fase1` dentro del contenedor principal.
- Procesos huérfanos de sesiones previas aún presentes (no iniciados en esta ejecución, **no** detenidos salvo el backend con conexiones al temporal): Maven wrapper PIDs 6232/31468 y forks surefire/byte-buddy 26048, 6016, 1864, 25456 (revisar y cerrar manualmente).
