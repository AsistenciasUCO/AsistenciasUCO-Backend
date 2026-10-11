---
status: TEST_DESIGN_FINAL_CODE_IMPLEMENTATION_PENDING
---
# Política de pruebas sin esconder RED y sin bloquear los PR de codec

## Dos gates (no mezclarlos)
- **GATE_A_CODEC_GREEN**: rama/PR de codec sin implementación v2; verificar `HttpUtcInstantCodecTest` (7/7), `HttpUtcInstantCodecD02ContractRedTest` (5/5), strict 003C **8/8 tras reparar aserción ".5Z"**, `SesionUtcActivationGuardTest` (3/3), regresión v1, JaCoCo, ArchUnit, Maven Java25. Los 16 assertions de HTTP/OpenAPI v2 son **candidatos RED de otra fase**, no certificado de funcionalidad presente. Recomendar micro-PR aislado en vez de excluir tests arbitrariamente de la suite de release.
- **GATE_B_V2_CONTRACT_RED_THEN_GREEN**: conservar `SesionV2HttpContractRedTest` + `OpenApiSesionesV2ContractRedTest` en rama de contrato y registrar fallo causal mientras no haya implementación. Al activar v2, integrar los tests en suite normal y pasar **todos**. No usar `@Disabled`, assumptions, capturas genéricas ni eliminar aserciones.
- **GATE_C_SQL_E2E_RELEASE**: después de work item owner DB y despliegue de ensayo SQL real + Keycloak, verificar POST/GET/PATCH v2, inexistente/indeterminada, 401/403, licencia de propietario, rollback, vista v1 exacta y RLS/ownership real. Cualquier TEST focal skip = NOT_CERTIFIED. Un skip heredado no UTC debe quedar nombrado/explicado; no presentar totalidad 191/191 si 1 fue skip.

## Contrato de decimales: comportamiento esperado
| Entrada con fecha-hora válida | Resultado |
|---|---|
| `2026-07-15T14:00:00Z` | 14:00 UTC |
| `2026-07-15T14:00:00.5Z` | 14:00:00.5000000 UTC |
| `2026-07-15T14:00:00.0000001Z` | 14:00:00.0000001 UTC |
| `2026-07-15T14:00:00.1234567Z` | 14:00:00.1234567 UTC |
| `2026-07-15T14:00:00.12345678Z` | 400 FIELD_INVALID_FORMAT |
| `2026-07-15T14:00:00.123456789Z` | 400 FIELD_INVALID_FORMAT |
| `2026-07-15T14:00Z` | 400 FIELD_INVALID_FORMAT |
| `2026-07-15T14:00:00+02` | 400 FIELD_INVALID_FORMAT |
| `2026-07-15T14:00:00+02:00:30` | 400 FIELD_INVALID_FORMAT |

## Matriz de escenarios E2E obligatorios — SQL Server 2022
| ID | Setup y operación | Oráculo |
|---|---|---|
| D06-01 | Column nueva en DB fresca vs existente | NULL para históricos, bytes de fechas sin alterar |
| D06-02 | v2 POST con Bogotá -05 (20:00 a 22:00) | SQL UTC 01:00→03:00 día siguiente, marca UTC_V2 MISMO INSERT |
| D06-03 | v2 PATCH de sesión confirmada | SQL horas y marca UTC_V2 mismo UPDATE y actor JWT |
| D06-04 | POST/PATCH v1 en sesión confirmada | marca NULL en mismo INSERT/UPDATE, wire v1 intacto |
| D06-05 | generador sobre horario institucional | UTC_GENERADOR solo si cálculo real comprobado, incluyendo DST |
| D06-06 | DML directo UPDATE 08:00→18:00 (runtime role) | DENEGADO, rollback, marca no obsoleta |
| D06-07 | clasificar ID/ambiente distinto | solo ID aprobado cambia a UTC_OWNER; otros NULL |
| D06-08 | Vista legacy SELECT * | EXACTAMENTE mismos nombres, tipos y orden |
| D06-09 | Vista nueva v2 SELECT | 10 columnas legacy + marca de esa misma fila |
| D06-10 | SQL error tras UPDATE intento | sin modificación parcial de horas ni marca |
| D06-11 | Sesion con DetalleAsistencia | sigue política explícita snapshot o sincronización, sin alteración accidental |
| HTTP-01 | naive, offsets malformados, precisión>7, 24:00, 29 febrero no bisiesto | 400 VALIDATION_ERROR + FIELD_INVALID_FORMAT y 0 calls a port |
| HTTP-02 | instante fin <= inicio post offset | SES_004 sin cambio SQL |
| HTTP-03 | dueño DOCENTE vs externo, estudiante, admin sin permiso, sin JWT | 2xx/403/401 según contrato, no 500/501 |
| READ-01 | GET confirmado UTC_V2, UTC_GENERADOR, UTC_OWNER | Z exacta en horas, estado CONFIRMADA |
| READ-02 | GET no clasificado | 200 horas NULL, estado INDETERMINADA, ID y datos no temporales |
| READ-03 | GET grupo mixto | sin filas ocultas, v1 independiente, orden estable |
| FRONT-01 | sesión Bogotá 20–22 julio, navegador Berlin recarga | 03–05 siguiente día, mismo ID e instant |
| FRONT-02 | Europe/Berlin 2026-03-29 02:30 (gap) | UI impide crear/editar |
| FRONT-03 | Europe/Berlin 2026-10-25 02:30 (overlap) | selección explícita +02/+01, instantes distintos |

## Captura de resultados
Documentar command exacto, SHA de 3 repositorios, perfil activo, hardware/versión SQL/JDK, fecha, test count, fallos, errores, skipped con nombres; detalles de 400 y SQL SELECT desensibilizado; correlación y permisos. No compartir JWT ni secretos. Si no hay owner DB/test SQL posterior, marcar BLOCKED_EXTERNAL_GATE y seguir con tareas unitarias, no inventar GREEN.

## Contrato de integración y rollback
No desplegar v2 con tabla/proyección ausente: respuestas mixtas o Z no verificados serían información falsamente confirmada. DB cambios backward-compatible → backend v2 → frontend v2. Revert backend/frontend primero, preservar columna con marcas antes de removerla; rollback de DB solo con respaldo/owner.
