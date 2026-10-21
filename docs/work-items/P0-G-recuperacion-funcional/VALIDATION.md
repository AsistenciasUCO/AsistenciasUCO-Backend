# VALIDATION — P0-G

Fecha: 2026-10-20. Alcance: recuperación y evidencia; no cierre funcional.

| Check | Comando/medio | Resultado | Limitación |
|---|---|---|---|
| Backend gate actual | `.\mvnw.cmd -B -ntp verify` con Java 25 | PASS; 1544 tests, 0 fallos, 0 errores, 0 skips; JaCoCo cumplió gates | Es la rama actual `befbdbf5`, no el candidato B1. |
| Frontend checkout actual | `npm.cmd run test:ci -- --no-progress` | PASS; 341/341 ChromeHeadless | Cobertura observada 64.97% statements, 46.51% branches, 51.17% functions, 66.76% lines; no certifica F1 candidato. |
| Frontend evidencia histórica | `P0-C` en commit `95a202a` | Versionado: 807/807 y cobertura documentada | No se repitió sobre ese commit en P0-G. |
| Stack preflight | `.\scripts\start-local-stack.ps1 -Check` | PASS de rutas/puertos; 1433, 8081, 9000, 3310, 8080, 4200 escuchando | `infra/files/.env` MISSING; servicios estaban ya activos. |
| Providers locales | `docker ps` sanitizado | ClamAV, MinIO, Keycloak, SQL Server y observabilidad activos | Salud actual no equivale a arranque limpio reproducible. |
| DB catalog/metadata | Consulta read-only a SQL Server local | PASS de presencia: W1/W2/R1/R2 presentes; W1/W2 4 cols; R1 10; R2 25 | Contrato/autoridad DB no aprobados. |
| E2E paginación histórico | P0-E | PASS documentado para teacher/students/sessions/directory y ownership | No es el flujo mutante de reclamo/soporte. |
| E2E completo claims/support | No ejecutado | NOT_RUN | Falta aprobación contractual, fixture y rollback autorizados. |
| P0-F original | Búsqueda en `docs/work-items` | BLOCKED_BY_MISSING_EVIDENCE | No existe el artefacto original en este checkout; se usan referencias preservadas. |

## Resultado de validación

La recuperación es reproducible como conjunto de referencias, pero no está lista para integración. Estado: `NOT_READY`; bloqueo principal `CONTRACT_CONFLICT: P0-G-DB-001` y evidencia faltante `P0-G-EVIDENCE-001`.

## Ramas/ref creadas por P0-G

Quedaron creadas sin checkout, force-update, merge ni push:

- `jose-valencia/backend-functional-recovery` → `f45a52137674415871882043771676b6592fb72c`.
- `jose-valencia/frontend-functional-recovery` → `95a202a6b767e537f519012a847cffe3a7c100ab`.
- `jose-valencia/db-functional-recovery` → `b698c76112490b91298d16ccd1fb6a793648adec`.

Las refs `codex/*` existentes permanecen intactas.

## Comandos de operación local

```powershell
.\scripts\start-local-stack.ps1 -Check
.\scripts\start-local-stack.ps1
.\scripts\start-local-stack.ps1 -Stop
```

Para un arranque limpio de archivos se requiere que un operador autorizado proporcione el archivo local ignorado `infra/files/.env`; P0-G no crea ni registra credenciales.
