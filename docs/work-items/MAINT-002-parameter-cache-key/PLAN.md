---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# MAINT-002 — cache keys estructurales para catálogo JPA

## DoR: READY / alcance microcorrección M02

**Fuente:** `AGENTS.md`, `docs/governance/DEFINITION_OF_READY.md`, `ParameterCatalogPort`, `ParameterCatalogJpaBehaviorTest` y `MAINT-001`. Baseline develop `bfc4fd3fee9fadefbb00fd6f4652ff1dc5423d69`, tres workflows PASS del PR #18 en SHA `39e34c49ab7882696ab491e1441e45e51cbaf6d0`.

**PRIMARY_VARIABLE:** corregir colisión de claves de la caché de parámetros SQL Server.
**Clasificación:** BEHAVIOR_CHANGE de adapter JPA (sin efecto normal en claves no ambiguas).
**ALLOWED:** `src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/jpa/repository/ParameterCatalogJpaRepository.java`, `src/test/java/co/edu/uco/asistenciasuco/infrastructure/adapter/secondary/persistence/sqlserver/jpa/repository/ParameterCatalogJpaBehaviorTest.java`, `docs/work-items/MAINT-002-parameter-cache-key/**`.
**FORBIDDEN:** tablas/vistas/SP, Angular, contratos HTTP/DTO, puertos Application, SQL de consulta, otros repositorios JPA, pom.xml, CI y configuración.

## AS-IS comprobable / defecto

La caché `ConcurrentHashMap<String,String>` usa `group.trim() + "::" + key.trim()`. Par distinto (`a::b`,`c`) y (`a`,`b::c`) produce la misma clave `a::b::c`. El segundo parámetro podría devolver el valor almacenado por el primero sin consultar DB. La existencia de esos valores en producción no está demostrada; es un defecto de integridad del esquema de clave y un riesgo de extensibilidad. El contrato `ParameterCatalogPort` toma dos argumentos independientes y no prohíbe el separador.

## TARGET

Record privado `ParameterKey(group, key)` con igualdad estructural, `Map<ParameterKey,String>`; normalizar/trim una vez. Cache hit obtiene un único valor mediante `get` en vez de `containsKey`+ `get`. `ConcurrentHashMap` mantiene prohibición de valores null. Miss sigue sin cachearse. Sin TTL/invalidador nuevo: política institucional de refresco de catálogos requiere contrato posterior, no mezclarla.

## Contratos e impactos

- HTTP/API: NONE; puerto `getParameter` y `getParameterAs` siguen iguales.
- Query JPA: HQL y `:grupo/:clave` sin cambios; usa `EntityManager` exclusivamente.
- Security: evita intercambio accidental de valores entre dos claves distintas (ej. parámetros de seguridad), sin afirmar explotación.
- Observabilidad: mismo mensaje/excepción para fallos de proveedor, sin log de valores secretos.
- Migration/DB: NONE, DB owner no intervenido.
- Runtime scope: código aislado a `ParameterCatalogJpaRepository`.
- Riesgo: rareza de `::` en claves; no introducir validación prohibitiva que altere datos institucionales.
- STOP: falla RED por fixture en lugar de colisión, conflicto de contrato, pérdida de caché positivo o causa de excepción.
- Rollback: revert de commit; no migraciones, no configuración.

## Gates

Test RED causal para colisión, controles existentes de blank, miss no cacheado, normalización, conversiones y excepciones. Ejecutar con JDK25 `./mvnw -B -ntp "-Dtest=ParameterCatalogJpaBehaviorTest" test`; luego `clean verify`, ArchUnit, Sonar, CodeQL y review humano. `CatalogJpaParityIT` real sigue siendo obligatorio para certificar SQL Server, no simularlo con Mockito.

**Estado de ejecución de RED:** candidato diseñado contra implementación anterior, *no ejecutado* por el autor. Debe comprobarse que falla contra el baseline por valor incorrecto y que el cambio aplicado deja GREEN. La separación física commits RED/GREEN no se produjo; actor independiente debe certificar causalidad con diff/revert temporal en entorno controlado, sin reescribir rama ni alterar tests.

No merge automático, no cambios de frontend, no declarar MAINT-001 cerrado.
