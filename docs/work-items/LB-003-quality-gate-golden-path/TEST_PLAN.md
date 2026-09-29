---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# TEST PLAN — LB-003 Quality Gate Golden Path

## Objetivo y variable principal

Certificar el Golden Path actual, sin cambiar comportamiento:

```text
POST lote -> persistencia confirmada -> GET/readback -> estado correcto
          -> realtime solo tras exito -> recuperacion HTTP
```

`PRIMARY_VARIABLE: CERTIFICACION_DEL_GOLDEN_PATH_ACTUAL`.

`PRODUCTION_CODE_CHANGES: NO`. Si una corrida descubre un defecto productivo, la condición de
parada es `QUALITY_GATE_DEFECT`: se registra el fallo y no se corrige dentro de LB-003.

## Autoridad y alcance

- Requisito: [PLAN](PLAN.md) y orden humana de ejecución de LB-003.
- Contrato HTTP/SSE: [OpenAPI canónico](../../contracts/openapi/openapi-golden-path.yaml),
  [BACKEND_GOLDEN_PATH_CONTRACT](../../contracts/BACKEND_GOLDEN_PATH_CONTRACT.md) y
  [REALTIME_EVENT_STANDARD](../../contracts/REALTIME_EVENT_STANDARD.md).
- Persistencia: contrato DB congelado y evidencia LB-002.2D sobre SQL Server real.
- Seguridad: [runtime-security-provider-architecture](../../security/runtime-security-provider-architecture.md).
- Gates: [TESTING_STANDARD](../../testing/TESTING_STANDARD.md),
  [VALIDATION_RUNBOOK](../../testing/VALIDATION_RUNBOOK.md) y
  [DEFINITION_OF_DONE](../../baseline/DEFINITION_OF_DONE.md).

Rutas autorizadas para escritura: `docs/work-items/LB-003-quality-gate-golden-path/**` y, solo si
la evidencia satisface su condición de resolución, la fila/sección TD-008 de
`docs/baseline/TECHNICAL_DEBT.md`. No se modifica `src/main/**`, `src/test/**`, `pom.xml`, DB/SQL,
frontend, runtime config ni workflows.

## Estrategia de reutilización

No se crean tests nuevos: los observables exigidos ya están cubiertos por tests existentes o por
evidencia manual válida. LB-003 vuelve a ejecutar solo:

1. el gate normal completo sobre el estado actual;
2. la suite SQL Server targeted certificada en LB-002.2D.

Se reutilizan sin repetición manual: LB-001 Contract First, LB-002.2C, LB-002.2D, LB-002.2E,
[MV-001](../../baseline/MANUAL_VALIDATION_LEDGER.md) y
[MV-006](../../baseline/MANUAL_VALIDATION_LEDGER.md).

## Behavioral Matrix

| ID | Requirement | Scenario | Input / state | Observable | Expected | Wrong implementation caught | Level / evidence |
|---|---|---|---|---|---|---|---|
| LB3-B01 | Build/arquitectura/contrato actuales | Gate técnico normal | checkout actual, Java 25 | Maven, Surefire, JaCoCo, ArchUnit y OpenAPI | exit 0; 0 F/E; LINE >=80 %; BRANCH >=70 %; ArchUnit/OpenAPI PASS | regresión de código, arquitectura o contrato | `clean verify` |
| LB3-B02 | Contrato SP real | Firma desplegada del Golden Path | `sql_server_asistencias` / `gestionasistenciadb` | parámetros y objetos SQL observados | 16/16, 0 skips | drift de firma u objeto DB | `GoldenPathSqlStoredProcedureContractIT` |
| LB3-B03 | Write/read JDBC | lote válido y readback | fixture real con docente titular y estudiantes | filas y proyección leídas | persistencia y estado coherentes | command que responde éxito sin persistir | `AsistenciaRepositorySqlServerIT` |
| LB3-B04 | Paridad query | mismo fixture JDBC/JPA | SQL Server real | proyección field-by-field | 0 mismatches | mapping JPA divergente | `AsistenciaQueryJpaParityIT` |
| LB3-B05 | Paridad command | AN/SJC/EX, errores, upsert, readback | mismo fixture JDBC/JPA | filas, errores y estado final | 0 mismatches | binding/mapping/semántica JPA divergente | `AsistenciaCommandJpaParityIT` |
| LB3-B06 | Atomicidad | lote mixto o fallo | SQL Server real | estado visto por conexión independiente | rollback total, `@@TRANCOUNT=0` | commit parcial o transacción exterior | `AsistenciaCommandTransactionBoundaryIT` |
| LB3-B07 | Concurrencia | cuatro writers coordinados | SQL Server real | duplicados, errores y convergencia | estado convergente sin excepción cruda | carrera o duplicación | `AsistenciaCommandConcurrencyIT` |
| LB3-B08 | Realtime posterior al commit | command exitoso | use case real + JPA + DB real | evento y filas visibles al publicar | exactamente un evento canónico tras persistir | publicar antes del commit o duplicar | `AsistenciaCommandRealtimeIT` |
| LB3-B09 | Cero realtime en fallo | docente ajeno / estado inválido | fixture real | publisher | cero eventos | publicar aunque falle persistencia/autorización | `AsistenciaCommandRealtimeIT` |
| LB3-S01 | Autenticación | credencial ausente/inválida | request protegido | HTTP | 401 seguro | permitir anónimo o responder 403/500 | `SecurityConfigTest`, `RbacSecurityFilterChainTest` |
| LB3-S02 | RBAC | usuario autenticado sin rol DOCENTE | POST lote | HTTP | 403 | matcher demasiado amplio | `registrar_asistencias_lote_estudiante_recibe_403` |
| LB3-S03 | Ownership | DOCENTE ajeno | sesión/grupo ajeno | excepción, DB y realtime | 403/FORBIDDEN, 0 escrituras, 0 eventos | confiar solo en rol HTTP | use-case test + `AsistenciaRepositorySqlServerIT` + parity/realtime IT |
| LB3-S04 | Acceso válido | DOCENTE titular | lote válido | HTTP/application/DB | acceso permitido y persistencia exitosa | denegar actor legítimo o perder principal | RBAC + use-case + targeted SQL suite + MV-006 |
| LB3-E01 | Errores seguros | `SEC_001`, `SEC_002` | DBCODE formal | excepción y respuesta HTTP | 403/FORBIDDEN, sin DBCODE/detalle técnico | 500 o fuga técnica | `DbExceptionTranslatorTest` + `GlobalExceptionHandlerTest` |
| LB3-E02 | Errores seguros | `ATT_001/2/3`, `GEN_002`, `SES_004` | DBCODE formal | excepción y catálogo HTTP | Validation/400, no 500 | clasificador textual o 500 | `DbExceptionTranslatorTest` + handler/catalog tests |
| LB3-E03 | Error seguro | `SES_003` | DBCODE formal | excepción y catálogo HTTP | FeatureUnavailable/501, no éxito ni 500 | simular éxito o 500 | `DbExceptionTranslatorTest` + catálogo/handler existente |
| LB3-C01 | Correlation request | header válido/ausente/inválido | request HTTP | contexto, MDC y response header | UUID preservado/generado y limpieza | correlación compartida o perdida | `CorrelationIdFilterTest` |
| LB3-C02 | Correlation hacia SP/log/evento | command JPA | contexto establecido | parámetro SP, log y evento | mismo correlation disponible; sin generar otro en Application | UUID nuevo por adapter o pérdida de contexto | command adapter tests + targeted IT + realtime tests |
| LB3-A01 | Auditoría | POST lote anotado | camino HTTP completo | evento de auditoría y persistencia/consulta real por correlation | observable o brecha explícita | afirmar auditoría por mocks/unit únicamente | inspección + evidencia LB-002.2D; no se inventa PASS |
| LB3-R01 | Recuperación realtime | dos clientes, desconexión/reconexión | runtime local JPA + frontend + SQL real | SSE y recarga HTTP | dos clientes PASS; reconvergencia ~25 s; HTTP source of truth | tratar SSE como estado durable | MV-006 (no se repite) |
| LB3-CI01 | CI remoto | workflows/rulesets/checks actuales | repositorio remoto | run URL, checks y rulesets | evidencia identificada o `PENDING` | inferir CI remoto desde Maven local | `.github/CI.md`, workflows y evidencia remota si accesible |

## Matriz de seguridad consolidada

| Caso | Expected | Evidencia prevista | Gate actual |
|---|---:|---|---|
| autenticación ausente o inválida | 401 | `SecurityConfigTest`; `RbacSecurityFilterChainTest` (API stateless protegida) | `clean verify` |
| autenticado sin autorización | 403 | `registrar_asistencias_lote_estudiante_recibe_403` | `clean verify` |
| DOCENTE ajeno / ownership incorrecto | 403 / `FORBIDDEN`; 0 write; 0 realtime | use-case, JDBC IT, command parity y realtime IT | normal + targeted SQL |
| rol y ownership válidos | éxito | RBAC permite DOCENTE; use-case + targeted SQL + MV-006 | normal + targeted + evidencia manual previa |

## Comandos autorizados

Gate técnico, exactamente una vez si pasa y no cambia código/test después:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean verify
```

Gate SQL Server targeted, cargando credenciales locales sin imprimir valores:

```powershell
.\mvnw.cmd -Pintegration verify "-Dit.test=GoldenPathSqlStoredProcedureContractIT,AsistenciaRepositorySqlServerIT,AsistenciaQueryJpaParityIT,AsistenciaCommandJpaParityIT,AsistenciaCommandTransactionBoundaryIT,AsistenciaCommandConcurrencyIT,AsistenciaCommandRealtimeIT"
```

No se ejecuta el perfil global como gate de LB-003. Su estado heredado se conserva como
`NOT_GREEN_TD043`.

## RED_SNAPSHOT

`NO_APLICA`: LB-003 no cambia requisitos, contrato, código ni comportamiento. Crear un RED Java
cosmético violaría el estándar. La evidencia executable ya existe y se vuelve a certificar sobre
el checkout actual.

## Criterios de resultado y stop conditions

- `QUALITY_GATE_DEFECT`: cualquier fallo de producción/contrato/gate targeted; no corregir dentro
  de LB-003.
- `VALIDATION_BLOCKED_BY_ENVIRONMENT`: SQL Server o credenciales no disponibles; no convertir en
  PASS.
- `BLOCKED_BY_MISSING_EVIDENCE`: CI remoto o auditoría obligatoria sin evidencia suficiente.
- LB-003 solo puede quedar `READY_FOR_HUMAN_CLOSURE_REVIEW` si cada check aplicable de la DoD es
  PASS o NO APLICA con justificación normativa.

