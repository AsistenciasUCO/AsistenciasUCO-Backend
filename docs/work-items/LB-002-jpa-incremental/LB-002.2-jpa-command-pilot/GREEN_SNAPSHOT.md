---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-28
---

# LB-002.2C — GREEN_SNAPSHOT (evidencia reproducible)

Evidencia de la validación final de LB-002.2C (implementación candidata JPA de `registrarAsistenciasSesion`). Complementa [RED_SNAPSHOT](RED_SNAPSHOT.md) y [BASELINE](BASELINE.md). Solo registra lo ejecutado; no certifica paridad JDBC↔JPA ni E2E.

```text
FECHA/HORA:           2026-09-28 (clean verify finalizado ~20:37 -05:00)
BASE_GIT:             0bfc02a58a8abc894841cc1e9b31c002a716554e
RAMA:                 jose-valencia/lb-002.2a-jpa-command-plan
ESTADO GIT:           cambios SIN commit (working tree); sin push, PR, merge, rebase ni tag
JAVA (mvnw.cmd):      25 (JAVA_HOME = jdk-25; el `java` del PATH del shell es 17 y NO se usa para Maven)
MAVEN ENFORCER:       require-jdk-25 → RequireJavaVersion passed
FROZEN_RED_HASHES_UNCHANGED: YES
```

## Hashes RED (SHA-256 del contenido normalizado a LF: `sed 's/\r$//' | sha256sum`)

Recalculados **después** del `clean verify` final; idénticos a [RED_SNAPSHOT](RED_SNAPSHOT.md).

| Archivo | SHA-256 |
|---|---|
| `AsistenciaCommandProviderAcceptanceRedTest.java` | `d19d00e289d9f58ad273af003ecd77d002f8b924b96e8740345675685eefe7b6` |
| `AsistenciaCommandProviderCharacterizationTest.java` | `a83b5740164544da1081c6d4a4dd7033387f49d5ba262c09c72c1bb3c8c524bb` |
| `EntityManagerFactoryDefinitionProbe.java` | `a08a76a07a5137094580af7f3187a50ea860bc049f18562d9d34b02e38a452c8` |
| `JpaAttendanceStoredProcedureFeasibilityIT.java` | `b96b944c0c85801b690ec2c79f3d3df9894ffceeda4951938523f3830bb94be2` |
| `AttendanceCommandFixture.java` | `c2d92ceb97f7715fa423203a9c251bc33299379864b391b1f7cd16890a882a99` |

Ningún test tracked existente fue modificado (`git ls-files -m src/test` = 0).

## Comandos y resultados

| # | Comando | Exit | Resultado |
|---|---|---|---|
| 1 | Verificación de hashes RED (5 archivos) | 0 | 5/5 coinciden |
| 2 | Surefire dirigido: `AsistenciaCommandProviderAcceptanceRedTest` + `AsistenciaCommandProviderCharacterizationTest` | 0 | 9 tests, 0 failures, 0 errors, 0 skipped (6 RED-A..D ahora GREEN + 3 caracterización) |
| 3 | Suite dirigida 2.2C (todos los tests de command/selector/híbrido/validador/condición/ArchUnit nuevos y existentes afectados) | 0 | 168 tests, 0 failures, 0 errors, 0 skipped |
| 4 | `.\mvnw.cmd clean verify` (sin `-Pintegration`, sin `-Pazure-integration`, sin `-Dmaven.test.failure.ignore`) | 0 | `BUILD SUCCESS`, ~1.9 min |

### `clean verify` final

```text
TOTAL_TESTS: 1426     FAILURES: 0     ERRORS: 0     SKIPPED: 0
Fases ejecutadas: enforcer(require-jdk-25) · jacoco:prepare-agent · resources/copy-canonical-openapi ·
                  compile · testCompile · surefire:test · jar · spring-boot:repackage · jacoco:report · jacoco:check
Failsafe: no forma parte del `verify` normal (hermético); los *IT solo corren con -Pintegration / -Pazure-integration (NO ejecutados)
JaCoCo check: "All coverage checks have been met."
```

Evolución de tests: 1291 (baseline `0bfc02a`) → 1300 (+9 RED/caracterización de 2.2B) → **1426** (+126 tests nuevos de 2.2C).

Tests nuevos de 2.2C (Surefire, todos 0 fallos): `AsistenciaJpaCommandPersistenceTest` 41 · `JpaCapabilityRequiredConditionTest` 29 · `AsistenciaCommandProviderCompositionRootTest` 21 · `CanonicalProcedureResultValidatorTest` 20 · `JpaCommandIsolationRulesTest` 9 (ArchUnit) · `AsistenciaRepositoryHybridCommandRoutingTest` 6 = 126.

### Cobertura JaCoCo (global, `target/site/jacoco/jacoco.xml` del `clean verify` final)

| Métrica | Cubiertas | Perdidas | % | Umbral | Resultado |
|---|---|---|---|---|---|
| LINE | 7556 | 781 | **90.63 %** | ≥ 80.00 % | PASS |
| BRANCH | 1746 | 405 | **81.17 %** | ≥ 70.00 % | PASS |
| INSTRUCTION | 31029 | 3415 | 90.09 % | — | informativo |

Baseline previo (informativo): LINE 90.53 % / BRANCH 80.81 %. No se bajó ningún umbral ni se excluyó código de cobertura.

### ArchUnit y OpenAPI (dentro del mismo `clean verify`)

- ArchUnit: todas las clases `architecture.*` en verde, incluidas `JpaIsolationRulesTest` (6), `JpaCommandIsolationRulesTest` (9, nueva), `CleanArchitectureRulesTest` (20), `ApplicationShouldNotDependOnInfrastructureTest` (3), `AdapterCompositionRootRulesTest` (8). Grep independiente: 0 tipos `jakarta.persistence`/`org.hibernate`/`org.springframework.data` en Domain/Application.
- OpenAPI: `OpenApiGoldenPathConformanceTest` 9, `OpenApiGoldenPathValidationTest` 2, `SwaggerUiDisabledRuntimeTest` 1, `SwaggerUiEnabledRuntimeTest` 4 → 16/16 PASS.

## Hashes de contratos (sin cambios)

```text
openapi-golden-path.yaml          72a3097bbae2a296ed6690bf89c239f56697764f25100b930d941a699736da54
BACKEND_GOLDEN_PATH_CONTRACT.md   9b4830b468b58b49793658454a7b34deec20d2d9681943aa8f1e3942b15d8c62
DB_BASELINE_CONTRACT.md           45e48c5a0ab321d0c8cbffb55ee224e3b6fd29febc39a62ca723b2b209945aec
```

## Estado normativo y banderas de alcance

```text
DEFAULT_COMMAND_PROVIDER:      jdbc  (application.yml)
LOCAL_DEV_COMMAND_PROVIDER:    jdbc  (application-local.yml / application-dev.yml no modificados y sin command-provider)
LOCAL_DEV_QUERY_PROVIDER:      jpa   (sin cambios)
QUERY_COMMAND_MATRIX:          (jdbc,jdbc)→0 EMF · (jpa,jdbc)→1 · (jdbc,jpa)→1 · (jpa,jpa)→1  [tests de Composition Root]
DUAL_WRITE: NO                 JPA_EXTERNAL_TRANSACTION: NO
PORT_CHANGED / DOMAIN_CHANGED / APPLICATION_CHANGED / CONTROLLERS_CHANGED: NO
OPENAPI_CHANGED / SECURITY_CONFIG_CHANGED / POM_CHANGED: NO
DB_CHANGED / STORED_PROCEDURE_CHANGED / FRONTEND_CHANGED: NO
CANONICAL_EXECUTOR_CHANGED / DB_FAILURE_CLASSIFIER_CHANGED: NO
CERTIFIED_JDBC_TESTS_CHANGED: NO
LB-002.1C-B2-DECISION.md: presente, sin modificar
TD-043: OPEN / DEFERRED
GLOBAL_INTEGRATION_PROFILE: NOT_GREEN_TD043   (-Pintegration NO ejecutado)
SQL_SERVER_PARITY: NOT_RUN — LB-002.2D
E2E: NOT_RUN — LB-002.2E
```

## Higiene de seguridad

- Grep de `password|secret|bearer|client-secret|vault|api-key|jdbc:sqlserver://…` sobre archivos modificados/nuevos: solo placeholders (`.env.example`: `CHANGE_ME`, línea comentada nueva sin valores) y referencias preexistentes en `application.yml`; el IT de feasibility lee credenciales del `Environment` (no literales). Ningún secreto nuevo.
- `.env`, `.idea/`, `target/`, `.maven/`, `logs/` están ignorados por `.gitignore`; ninguno queda para versionarse.
- `git diff --check`: sin errores de whitespace (solo el aviso LF→CRLF de `.env.example`).

## Reproducción

```powershell
.\mvnw.cmd clean verify
```

Requiere `JAVA_HOME` apuntando a JDK 25. Sin DB ni Azure.
