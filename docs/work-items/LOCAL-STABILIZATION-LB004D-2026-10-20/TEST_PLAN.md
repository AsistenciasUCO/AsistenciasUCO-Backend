# TEST PLAN — LOCAL-STABILIZATION-LB004D-2026-10-20

## Requisitos verificables

| ID | Requisito | Resultado esperado | Nivel |
|---|---|---|---|
| STAB-GIT-001 | Las refs canónicas deben quedar observables y las ramas LB-004D originales intactas | SHA, ahead/behind y estado limpio registrados; ningún merge a `develop` | Read-only Git |
| STAB-DB-001 | CI debe preparar el fixture LB-004D fuera de seeds productivos | Script idempotente emite `TEST_FIXTURE_READY`; prueba D1 no falla por fixture ausente | SQL Server integration |
| STAB-DB-002 | El fixture no debe cambiar schema ni datos ajenos | Solo filas con namespace de prueba y cleanup acotado | SQL fixture review |
| STAB-BE-001 | No deben quedar bytecode Python versionado | `.pyc` ausente de index y ignore cubre `__pycache__`/`*.py[cod]` | Git/hygiene |
| STAB-FE-001 | El frontend debe conservar sus tests y fallar de forma honesta | `verify`/checks reportan resultado real; specs preexistentes permanecen identificadas | Angular |
| STAB-DOC-001 | MinIO/ClamAV/Keycloak/SQL deben ser operables sin pérdida de datos | health/readiness, puertos, volúmenes, conteos y límites documentados | Docker/manual |
| STAB-E2E-001 | El flujo E2E real no se declara PASS sin login, fixture contractual, storage y scanner reales | `PASS` solo con evidencia; en otro caso `NOT_RUN`/bloqueo explícito | E2E |

## TEST CONTRACT

- No se modifican los tests RED existentes para adecuarlos a una implementación.
- El fixture de DB es un artefacto de prueba explícito, no un seed funcional ni una migración.
- El contrato LB-004 conserva `fileId` opaco, bucket privado, ClamAV fail-closed, ownership real y `DOCENTE_READ_RELATED` bloqueado hasta el contrato DB.

## RED_SNAPSHOT

- `test/test_lb004d_support_binding.sql` observado en D1: requiere al menos dos grupos/estudiantes, dos docentes y usuarios auxiliares; CI no invoca un preparador LB-004D antes del resumen.
- Backend B1 observado: `scripts/e2e/files/__pycache__/files_storage_e2e.cpython-314.pyc` está versionado.
- Frontend F1 / PR #2: mergeable false y el propio PR documenta errores de specs preexistentes; no se reescriben.

## Secuencia RED → GREEN → VALIDATE

1. Ejecutar/registrar RED existente sin cambios.
2. Implementar fixture idempotente y llamada CI en branch DB auxiliar; ejecutar el test D1 contra SQL Server controlado.
3. Eliminar `.pyc` y ejecutar validación de higiene/backend en branch auxiliar.
4. Ejecutar frontend y registrar fallos de entorno o preexistentes por separado.
5. Validar Docker y proveedores reales sin borrar datos; ejecutar E2E solo si todas sus dependencias están READY.

## Cobertura y límites

Los gates JaCoCo del backend (`LINE >= 80%`, `BRANCH >= 70%`) siguen aplicando a `verify`; no se relajan por este work item. La integración externa no ejecutada es `NOT_RUN`, no `PASS`.
