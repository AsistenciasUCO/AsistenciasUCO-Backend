---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-19
---

# LB-004D — Reporte E2E (HTTP real)

```text
STACK: Keycloak local (8081) + backend real (jar, 127.0.0.1:8080) + SQL Server gestionasistenciadb (única base) +
       MinIO y ClamAV del laboratorio nuevo (127.0.0.1:19000 / 13310)
SCRIPT: scripts/e2e/files/support_binding_e2e.py  (fases main | after-restart | clamav-down | minio-down | sql-down | cleanup)
ANGULAR_UI_E2E: NOT_RUN  (no se condujo la interfaz en un navegador; el contrato del frontend se cubre con pruebas unitarias)
LB004D_CONTRACT_APPROVED: NO  (aprobación del owner DB pendiente)
```

Identidades: estudiante A y B del Grupo 001 (mismo grupo), docente titular, docente ajeno, coordinador y administrador, todos
mapeados a personas reales de la DB local. Secretos fuera del repo (`~/.asistencias-files/files.env`); no se imprimen.

| Fase | Resultado |
|---|---|
| `main` | 34/34 PASS: Bearer/roles, subida (ClamAV→MinIO→DRAFT), radicación con soporte (ATTACHED atómico), listado del docente titular, descarga con SHA-256 idéntico, docente ajeno / estudiante del mismo grupo / coordinador / administrador → 404 indistinguible, doble radicación → 404, soporte ajeno/inexistente → mismo 404, radicar sin soporte, EICAR, 5 MiB+1, nombre de 251 caracteres, ejecutable renombrado, sin filas SQL para rechazados |
| `after-restart` (JVM en `Asia/Kolkata`) | 4/4 PASS: mismos instantes UTC, mismo SHA-256, otro estudiante sigue en 404 |
| `clamav-down` | 2/2 PASS: 500 fail-closed, sin fila |
| `minio-down` | 3/3 PASS: subida 500 sin fila; descarga autorizada 500 sin filtrar internals |
| `sql-down` | 2/2 PASS: subida y listado 5xx sin filtrar internals. El objeto MinIO subido durante la caída queda sin fila (huérfano reconciliable; la reconciliación está diferida, Q6) |
| Recuperación tras reiniciar SQL | 4/4 PASS sin reiniciar el backend |
| `cleanup` | PASS: 0 filas de fixture; los objetos MinIO de la corrida permanecen en el bucket del laboratorio |

Hallazgo: `sqlcmd` exige `-I` (QUOTED_IDENTIFIER ON) para DML sobre tablas con índice filtrado; ya lo aplica el script.
