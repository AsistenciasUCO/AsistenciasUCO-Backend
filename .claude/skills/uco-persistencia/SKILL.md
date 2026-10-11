---
name: uco-persistencia
description: Revisar persistencia y planificar o ejecutar la migración JPA-only de LB-008 cuando esté autorizada.
---

# uco-persistencia

## Cuándo usar

Revisar persistencia JDBC/JPA y planificar o ejecutar la migración JPA-only de LB-008 cuando esté autorizada.

## Fuentes autoritativas

Lee [AGENTS](../../../AGENTS.md) y la [precedencia](../../../docs/governance/SOURCE_OF_TRUTH.md). Luego carga solo las fuentes pertinentes:

- [JDBC_TO_JPA.md](../../../docs/persistence/JDBC_TO_JPA.md)
- [ADR-003-jpa-only-persistence.md](../../../docs/adr/ADR-003-jpa-only-persistence.md)
- [ADR-002-jpa-incremental.md](../../../docs/adr/ADR-002-jpa-incremental.md)
- [repository-mock-inventory.md](../../../docs/integration/repository-mock-inventory.md)
- [GOLDEN_PATH_ASISTENCIA.md](../../../docs/baseline/GOLDEN_PATH_ASISTENCIA.md)
- [sqlserver-connection.md](../../../docs/integration/sqlserver-connection.md)
- [CONTRACT_ALIGNMENT_PROTOCOL.md](../../../docs/integration/CONTRACT_ALIGNMENT_PROTOCOL.md)

## Modos de trabajo

**A. CONTRACT ANALYSIS.** Puede inspeccionar `RepositoryPort`, adapter JDBC, SQL embebido, IT y schema/vistas/SP del repositorio DB, siguiendo el protocolo de alineación. No migra a JPA ni modifica código, tests ni SQL.

**B. PERSISTENCE MIGRATION.** Solo con contrato DB congelado, DoR READY, work item `PERSISTENCE_MIGRATION` autorizado y tests definidos.

Regla obligatoria: no iniciar JDBC → JPA sobre un contrato DB con `MISMATCH`, `DECISION_REQUIRED` o `BLOCKED_BY_MISSING_EVIDENCE` en la parte migrada.

## LB-002 / HISTÓRICO

- LB-002 usó una migración incremental y JDBC/JPA coexistieron temporalmente.
- Los selectores `jdbc|jpa` y el fallback JDBC fueron mecanismos de certificación y rollback del piloto.
- Ese patrón se conserva como evidencia histórica; ya no es el TARGET.

## LB-008 / TARGET ACTUAL

`PERSISTENCE_PROVIDER = JPA_ONLY`, conforme a [ADR-003](../../../docs/adr/ADR-003-jpa-only-persistence.md):

- no crear nuevos adapters híbridos ni nuevos selectores `jdbc|jpa`;
- no introducir fallback JDBC productivo;
- la coexistencia JDBC/JPA solo puede existir temporalmente dentro de una microfase para comparar paridad;
- al cerrar una capacidad migrada, su runtime debe ser JPA;
- `DIRECT_JDBC_IN_SRC_MAIN = 0` al cierre de LB-008;
- los commands de SP nuevos convergen en `EntityManager + createNativeQuery("EXEC ...") + getResultList()`;
- `StoredProcedureQuery` queda como mecanismo histórico del piloto LB-002, no como segundo patrón regular.

## Reglas obligatorias

Carga puerto y adapter JDBC afectados, contrato DB liberado e IT. Migra por microfases sin cambiar el RepositoryPort; `@Entity` solo en Infrastructure. JDBC/JPA pueden coexistir únicamente durante la comparación controlada de una microfase. Sin dual-write; paridad antes del retiro y runtime JPA-only al cerrar cada capacidad. Hibernate validate/none y open-in-view=false. Evitar N+1, EAGER como parche y cascadas generales; justificar fronteras transaccionales.

La paridad es obligatoria antes del switch: mismo fixture, semántica y proyección; baseline JDBC y candidato JPA comparados field-by-field. Commands y queries pueden migrar por separado. H2, repository mock o `EntityManager` mock no certifican SQL Server/JPA. El baseline JDBC puede permanecer durante la comparación, pero se elimina de la capacidad al certificarla; no se agrega selector ni fallback nuevo.

Piloto de query (LB-002.1, ver [LB-002.0-DECISION](../../../docs/work-items/LB-002-jpa-incremental/LB-002.0-DECISION.md)): migración de tecnología, no de autorización. Usa las vistas base `uv_detalle_asistencia`/`uv_asistencia`/`uv_estudiante_grupo`; no `uv_auth_*`, no `SESSION_CONTEXT`, no `usp_consultar_grupos_paginado`/paginación; `InstitutionalScopePort` y roles/ownership intactos. Paridad obligatoria en SQL Server real con el freeze DB desplegado.

## Archivos y cambios prohibidos

No modificar schema/SQL DB; no @Entity/JpaRepository/EntityManager en Domain/Application; no exponer entidades por HTTP; no cambiar contrato/RED ni agregar JPA/pom.xml fuera de un work item `PERSISTENCE_MIGRATION` READY.

## Quality gates

Integración real SQL Server, paridad query/command/errores/rollback y correlation; ArchUnit/verify/coverage. Mocks no certifican JPA real.

## Evidencia esperada

Firma DB con versión, fixtures, resultados observables equivalentes, configuración segura, rollback y selección por Composition Root.

Registrar resultados en el [work item](../../../docs/work-items/README.md). Ante evidencia necesaria ausente o contradicción autoritativa, aplicar los protocolos de AGENTS y no implementar el alcance bloqueado.

## Paginación JPA sobre vistas físicas (MAINT-001, 2026-10-09)

En SQL Server, vistas `dbo.uv_*` que representan entidades de lectura se mapean con `@Entity @Immutable @Table(schema="dbo")`; paginar y filtrar sobre ellas mediante JPA/SQL (Pageable o EntityManager + setFirstResult/setMaxResults, según contrato), nunca traer la vista completa a memoria para subList. Utilizar fuente de identidad 1:1 para totales de estudiantes y `EXISTS` para filtros en vistas de contexto 1:N. ORDER BY debe ser estable e incluir desempate por ID. Validar page/size y multiplicación con long en HTTP y secondary adapter; no exponer Spring Data `Pageable` desde Application sin decisión. Ver [análisis Arquisoft](../../../docs/work-items/MAINT-001-maintainability-audit/PAGINATION_REFERENCE_ANALYSIS.md). El ejemplo Arquisoft usa Pageable/Specification y `@Subselect` PostgreSQL; **no** copiar LATERAL ni reemplazar vistas SQL congeladas. Integración `-Pintegration` real y fixture multigrupo/11+ son necesarias para demostrar separación de páginas.
