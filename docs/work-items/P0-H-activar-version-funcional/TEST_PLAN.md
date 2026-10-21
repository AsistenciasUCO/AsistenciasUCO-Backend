# TEST_PLAN — P0-H: activar y validar la última versión funcional

## Fuentes

`PLAN.md`, `P0-G/CONTRACT_MATRIX.md`, `DB_BASELINE_CONTRACT.md`, `BACKEND_GOLDEN_PATH_CONTRACT.md`, OpenAPI canónico, `PROFESSOR_DECISION.md`, `TESTING_STANDARD.md` y `VALIDATION_RUNBOOK.md`.

## Behavioral Matrix

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|---|
| P0H-T01 | Preservar trabajo local | backend/frontend con cambios sin commit | snapshots comprobados | cambiar referencias | patch, manifest, status | cero pérdida de cambios | checkout destructivo | Git |
| P0H-T02 | Activar B1 | backend integration branch | merge candidateado | compilar y probar | clases/wiring W1/W2/R1/R2 | build y tests aplicables pasan | referencia Git sin código activo | Backend |
| P0H-T03 | Activar F1+teacher | `95a202a` incluye `fac2f640` | checkout original en rama integration | ejecutar verify | suite Angular y archivos F1/teacher | resultado actual trazable | seleccionar 95a sin fix docente | Frontend |
| P0H-T04 | Respetar DB contracts | SQL Server local y scripts D1 | backup/fixture controlado | inspeccionar/ejecutar W1/W2/R1/R2 | shapes y consumidores | W1/W2 cuatro; R1/R2 query shapes sin forzar mutación | mapear todo a cuatro columnas | DB |
| P0H-T05 | Wiring real | stack oficial | puertos y salud disponibles | iniciar/consultar procesos | PIDs, SHA, backend URL, frontend env | frontend consume 8080 y providers responden | validar con clones/mock | Runtime |
| P0H-T06 | Docente | identidad docente válida y datos existentes | backend/frontend activos | login, grupos, sesiones, control, reclamos | respuestas/UI | Arquitectura de Software y dos sesiones visibles; reclamo aprobado/soporte accesible | evidencia histórica como PASS | Manual E2E |
| P0H-T07 | Flujo mutante | fixture autorizado y rollback | estudiante/docente y sesión válida | crear reclamo, adjuntar, consultar, descargar, aprobar/rechazar | persistencia, bytes y estado bilateral | PASS solo con evidencia completa | mock o estado temporal | E2E |
| P0H-T08 | Seguridad | tokens reales de prueba | roles/ownership configurados | repetir acceso no titular | HTTP 401/403/404 según contrato | sin fuga de existencia/bytes | autenticación sin ownership | Security |

## Integration Requirement

- REAL_PROVIDER_REQUIRED: YES para SQL Server, Keycloak, MinIO, ClamAV y flujo E2E; NO solo para unit tests ya existentes.
- ENVIRONMENT: repositorios originales; SQL Server 1433, Spring Boot 8080, Angular 4200, MinIO 9000, ClamAV 3310, Keycloak 8081 según stack observado.
- MOCK_SUFFICIENT: NO para wiring, SQL, almacenamiento, malware, JWT real o persistencia.
- WHY: la aceptación depende de que el sistema centralizado reproduzca datos, ownership, bytes y estados reales.

## Negative / Boundary Coverage

Ownership ajeno, soporte inexistente, sesión inválida, archivo infectado/no disponible, ausencia de horario regular con grupo asignado, resultset R1/R2 incompatible, login inválido, frontend apuntando a backend incorrecto y procesos duplicados.

## Side Effects / Rollback

El E2E mutante solo se ejecuta con fixture autorizado. Registrar IDs de fixture, hashes de soporte y limpieza/rollback; no borrar datos históricos ni ejecutar scripts destructivos no autorizados.

## Pruebas RED requeridas

No se crea RED funcional nuevo para reactivar código ya candidateado. Los tests existentes de B1/F1 se congelan por commit y se ejecutan sin rebajarlos. Si un test contradice el contrato, registrar `TEST_CONTRACT_CONFLICT` y detener el alcance afectado.

## RED_SNAPSHOT

`NO APLICA` a nueva implementación; se conserva el snapshot de commits candidatos y de los tests existentes en `CONTRACT_MATRIX.md`/`VALIDATION.md`.

## Congelación

Backend `f45a5213`, frontend `95a202a` (incluye ancestro `fac2f640`) y DB `b698c76`; cualquier cambio posterior se reporta como diferencia de integración, no como el candidato original.

## Integración y E2E

Se requiere stack local real, fixture controlado, tokens de prueba no documentados y comprobación de base/bytes. `NOT_RUN` permanece explícito si falta un provider o autorización.

## Falsos positivos que deben evitarse

- Una rama/ref preservada no significa código activo.
- Un mock no certifica SQL Server, MinIO, ClamAV o Keycloak.
- La suite frontend del candidato no certifica compatibilidad con el backend integrado.
- Presencia de SP no equivale a aprobación DB.
- Un reclamo visible sin descarga/persistencia bilateral no es PASS completo.
