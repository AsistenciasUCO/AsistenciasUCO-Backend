---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# MAINT-001 / Paginación por vistas SQL: comparación con Arquisoft

## Fuentes consultadas

1. Archivo de referencia facilitado por el profesor: `arquisoft-backend-develop(1).zip`. No se ha copiado código ni agregado el ZIP al repositorio.
2. Código `alejovh12/AsistenciasUCO`, PR #18, comparado con develop tras merge SEC-002.
3. Fuente del esquema DB `johnjduque/gestion-asistencia-db` rama develop: `schema/views/{uv_estudiante_identidad,uv_estudiante,uv_usuario}.sql`.
4. Contratos: AGENTS, ADR-003/004, DB freeze, MAINT-001; HTTP GET `/api/v1/estudiantes`, `page=0`, `size=20` por defecto, máximo `size=100`.

## Patrón observado en Arquisoft (fuente externa de referencia)

| Responsabilidad | Archivo del ZIP (ruta lógica) | Comportamiento |
|---|---|---|
| Criterios de consulta | `shared/query/QueryCriteria.java` | pagina/tamanio, whitelist sort/filtro, max 100, max profundidad 10; normaliza page negativa a 0 y size no positivo a 10 |
| Conversión a JPA | `shared/jpa/util/PageableMapper.java` | crea `PageRequest.of`, campos de orden traducidos a rutas autorizadas |
| Abstracción repo | `shared/jpa/repository/SpecificationQueryRepository.java` | `Page<T> findAll(Specification<T>, Pageable)`, `count(Specification<T>)` |
| Filtros | `shared/jpa/query/QueryJpaSpecification.java` | convierte árbol de predicados tipados a `Specification<T>` sin concatenar valores |
| Adaptador | `fichas/infrastructure/.../FichaPerfilQueryOutputAdapter.java` | `criteria → Pageable → spec → repository.findAll → mapper → PaginatedResult` |
| Proyección de lectura | `.../FichaPerfilJpaQueryEntity.java` | `@Entity @Immutable @Subselect` sobre PostgreSQL con `JOIN LATERAL`, **no** necesariamente una vista física DB |
| Mapeo respuesta | `shared/jpa/util/PaginationMapper.java` | mapea `Page` a lista, página, tamaño y total |

El esquema concreto de Arquisoft depende de PostgreSQL; `@Subselect` y `LATERAL` no son requisito de AsistenciasUCO SQL Server. El principio reutilizable es paginar/filter/order en SQL mediante JPA sobre la proyección, no en listas Java.

## AS-IS real de AsistenciasUCO

| Capa | Archivo/concepto | Estado |
|---|---|---|
| HTTP | `EstudianteController` y `ConsultarEstudiantesRequestValidator` | GET con filtros y page/size (0/20), page>=0, size 1..100; PR #18 añade validación conjunta del offset (int max) |
| Application | `ConsultarEstudiantes*DTO/UseCase` | puerto y forma del resultado independiente de framework Spring Data |
| Secondary port | `ConsultarEstudiantesRepositoryDTO` y `EstudianteRepositoryPort` | page/size y filtros; sin `Pageable` en el puerto |
| Infra JPA | `EstudianteJpaRepository` | consulta HQL fija sobre `UvEstudianteIdentidadEntity` + `UvUsuarioEntity`, parámetros vinculados; COUNT separado; `setFirstResult(offset)`, `setMaxResults(size)` y ORDER BY determinístico (`e.id` desempate) |
| SQL Server owner | `dbo.uv_estudiante_identidad` | vista lógica una fila por estudiante: `Estudiante` JOIN `uv_usuario`; dimensión apropiada para paginar estudiantes distintos |
| SQL Server owner | `dbo.uv_estudiante` | vista de contextos estudiante-grupo (1:N); usada por `EXISTS` para filtrar institución/facultad/programa/grupo sin duplicar estudiantes |
| Result/Mapper | `EstudiantePaginaRepositoryProjection`, `CoreViewJpaProjectionMapper` | lista paginada + `totalItems,totalPages,page,size` con modelo de aplicación desacoplado |

### Dictamen de arquitectura

**El backend ya pagina del modo indicado por el profesor**: las vistas resuelven la fuente de lectura; JPA/SQL Server calculan los registros de cada página con `setFirstResult/setMaxResults` (limit/offset equivalente), count y orden estable. No se hace `getResultList()` sin paginar para después llamar a `subList()` en memoria.

**Mantener** el diseño actual para el endpoint estudiantes. Cambiar a Spring Data `Pageable` es opcional y no aporta garantía adicional de SQL pagination; migrarlo exigiría revisar dependencias, proyecciones, filtros y consumidores. No trasladar `Pageable` al puerto Application, ni duplicar `@Subselect` donde ya existe vista física.

### Riesgo cardinalidad

`uv_estudiante` tiene varios registros por estudiante según grupo/asignatura. Si se paginase directamente esa vista como lista de estudiantes, aparecerían duplicados y el COUNT no correspondería a estudiantes únicos. El diseño actual pagina `uv_estudiante_identidad`, aplica `EXISTS` contra `uv_estudiante`, evitando N filas por estudiante. Cualquier transición debe preservar este comportamiento.

### Cambios controlados propuestos en PR #18

1. Mantener la validación del offset en el adaptador (ya incluida en PR #18) y repetirla en validación HTTP para obtener el error de validación usual en lugar de propagar un fallo de infraestructura.
2. Añadir tests de valor límite HTTP y sin interacción JPA para offsets inválidos.
3. Añadir IT de paridad SQL Server para páginas 0/1/2 con `COUNT_BIG`, ORDER y `OFFSET ... FETCH NEXT ...` usando las **vistas físicas** como oráculo; no usar mocks para certificar paginado real.
4. Actualizar gobernanza: vistas de lectura `@Immutable`; tabla/comando no mezclado con vista; autorización/filtros/orden en SQL; evitar `N+1`; SQL profiling en fase de volumen.

### Evidencia y limitaciones

- PR #18 antes de estos cambios: Backend CI, Backend Security, Trivy/ArchUnit SUCCESS en SHA `f86cb74e1abf033850cf7550b2152d8023949d80`.
- El nuevo código (validación HTTP y tests de integración) todavía necesita CI del nuevo SHA.
- IT de 3 páginas prueba orden/contadores independientemente del tamaño de fixture; para demostrar páginas llenas/partición sin repeticiones, usar fixture >=11 estudiantes en ambiente de integración controlado y documentar cleanup; no modificar DB ni depender de fixtures aleatorias.
- El IT existente `estudiantes_conservan_paginacion_detalle_contextos_y_not_found` compara `before` sin limitar contra page size=100; asume por tanto <=100 estudiantes. Recomendación siguiente: sustituir esa aserción por comparación paginada, sin relajar chequeos sobre detalles y filtros; requiere microfase de limpieza de fixtures.
- La elección `totalPages` como int y el cálculo con double requiere revisión de dominio si se esperaran conteos extraordinariamente grandes; no inventar requisito de miles de millones de estudiantes.

## Se necesita modificar DB?

**NO** para paginación de estudiantes: ambas vistas físicas ya están disponibles en el contrato DB y se consumen desde entidades JPA `@Immutable`. Cambiar índices, vistas o cardinalidad sería una tarea nueva con aprobación del owner DB. La futura metadata de reclamos LB-004 continúa bloqueada por contrato DB, independiente de MAINT-001.

## Validation gates

Java 25 clean verify, ArchUnit, Sonar (new code), CodeQL, Trivy, SQL Server `CoreViewQueriesJpaParityIT` real -Pintegration (incluyendo dato de muestra >10 estudiantes) y revisión de consumidores GET estudiantes; no merge si se pierden permisos/orden/cuenta.
