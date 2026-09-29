---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-28
---

# LB-002.2C — REPORT

```text
MICROFASE:                     LB-002.2C — JPA COMMAND IMPLEMENTATION + GREEN + validación final
BASE_GIT:                      0bfc02a58a8abc894841cc1e9b31c002a716554e
RAMA:                          jose-valencia/lb-002.2a-jpa-command-plan
ESTADO:                        READY_FOR_HUMAN_CLOSURE_REVIEW (sin commit)

COMMAND_JPA_IMPLEMENTED:       YES
JPA_COMMAND_FULLY_VALIDATED:   NO      (falta paridad SQL Server real — LB-002.2D — y E2E — LB-002.2E)
JPA_COMMAND_FEASIBILITY:       PASS — LB-002.2B
DEFAULT_COMMAND_PROVIDER:      jdbc
LOCAL_DEV_COMMAND_PROVIDER:    jdbc    (local/dev: query=jpa, command=jdbc)
JDBC_JPA_SQL_PARITY:           NOT_RUN — LB-002.2D
E2E:                           NOT_RUN — LB-002.2E
TD-043:                        OPEN / DEFERRED
GLOBAL_INTEGRATION_PROFILE:    NOT_GREEN_TD043
```

## Resultado

Existe un candidato JPA (`EntityManager` + `StoredProcedureQuery`) para `registrarAsistenciasSesion`, seleccionable con `app.adapters.persistence.asistencia-command-provider=jpa`, reversible a `jdbc` con variable de entorno + reinicio, e inerte por defecto. El `clean verify` final es verde: 1426 tests, 0 failures/errors/skipped, LINE 90.63 % / BRANCH 81.17 %, ArchUnit y OpenAPI verdes, Enforcer Java 25. El RED congelado no fue alterado (5/5 hashes). Detalle en [GREEN_SNAPSHOT](GREEN_SNAPSHOT.md); interpretación, rollback y riesgos en [VALIDATION](VALIDATION.md).

## Alcance

Cinco archivos tracked modificados (`.env.example`, `application.yml`, `AsistenciaRepositoryHybridSqlServerAdapter`, `SqlServerCoreRepositoryAdapterConfiguration`, `SqlServerJpaAsistenciaQueryAdapterConfiguration`), cuatro clases de producción nuevas (`AsistenciaCommandPersistence`, `AsistenciaJpaCommandPersistence`, `CanonicalProcedureResultValidator`, `JpaCapabilityRequiredCondition`) y 11 archivos de test nuevos (6 clases con tests de 2.2C; 2 clases congeladas de RED/caracterización; 3 congelados de soporte: sondeo EMF, IT y fixture de feasibility). Sin cambios en Domain, Application, `AsistenciaRepositoryPort`, controllers, OpenAPI, SecurityConfig, pom.xml, SP/DB, frontend, YAML local/dev, tests JDBC certificados, `CanonicalStoredProcedureExecutor` ni `DbFailureClassifier`.

## Correcciones durante la validación final

Ninguna. No apareció ninguna regresión real de 2.2C.

## Lo que este informe NO afirma

Que el candidato sea equivalente al baseline JDBC contra SQL Server real, que el perfil global de integración esté verde, ni que el command JPA pueda activarse en local/dev/producción. Esas conclusiones requieren 2.2D/2.2E y decisión humana explícita.
