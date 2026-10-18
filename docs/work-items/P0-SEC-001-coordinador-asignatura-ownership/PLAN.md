# PLAN — P0-SEC-001: Ownership de mutaciones de asignatura del Coordinador

## Identidad y objetivo

- Fecha / rol: 2026-10-18 / auditoría de seguridad y aplicación.
- Objetivo: impedir que un usuario con rol HTTP `COORDINADOR` modifique o altere el estado de una asignatura fuera de su programa autenticado.
- Evidencia inicial: `CoordinadorPortalController` pasa actor solo al alta; actualización usa `null` y toggle no transporta actor. En DB `usp_crear_asignatura` valida rol, mientras `usp_actualizar_asignatura` y `usp_toggle_estado_asignatura` reciben solo identificador/correlación y actualizan por id sin titularidad observable en sus scripts públicos.
- Criterio de aceptación: alta, actualización y toggle validan el programa del actor y la pertenencia de plan/asignatura antes de invocar command; actor siempre proviene del resolver JWT; rutas HTTP y respuestas no cambian; el SQL público no cambia.
- Fuentes: `AGENTS.md`, `API_DESIGN_RULES.md`, OpenAPI canónico, `HTTP_AS_IS_MATRIX.md`, `InstitutionalScopePort`, `AsignaturaQueryPort`, `PlanEstudioQueryPort`, scripts DB oficiales en `develop` (`e248619`), ADR-003/004.

## AS-IS y evidencia

| Hecho | Archivo / símbolo | Evidencia |
|---|---|---|
| Controller protegido por rol HTTP | `CoordinadorPortalController` | `/api/v1/coordinador/**` usa `AuthenticatedUserResolver`; matcher exige COORDINADOR/ADMINISTRADOR según SecurityConfig. |
| Alta transporta actor | `crearAsignaturaPlan` | Construye `GuardarAsignaturaDTO` con `requireAuthenticatedUserId()`. |
| Actualización pierde actor | `actualizarAsignaturaPlan` | Construye el DTO con `usuarioEjecutor = null`. |
| Toggle pierde actor | `toggleEstadoAsignatura` + input/use case | El método solo recibe `asignaturaId`; no hay actor en la cadena. |
| Scope disponible | `InstitutionalScopePort` | `findProgramaIdByCoordinadorUsuario` y `canCoordinadorAccessPrograma` ya existen. |
| Ownership de datos consultable | `AsignaturaQueryPort` / `PlanEstudioQueryPort` | Las proyecciones contienen `idPrograma`; los puertos permiten comprobar pertenencia antes del command. |
| Provider DB | `usp_actualizar_asignatura.sql`, `usp_toggle_estado_asignatura.sql` en DB oficial | No hay parámetro de ejecutor ni llamada observable a titularidad; scripts son del owner DB y están fuera de rutas modificables. |

## TARGET

La capa Application obtiene el actor desde el JWT ya resuelto por el adapter primario, resuelve su programa, verifica que el plan solicitado y la asignatura objetivo pertenecen a ese programa y solo entonces llama al command. La defensa Application no reemplaza una futura defensa DB; el owner DB debe evaluar agregar titularidad al contrato de esos providers en un work item separado.

## Clase de cambio y alcance de rutas

- Change class: `BEHAVIOR_CHANGE` / security hardening sin cambio de wire HTTP.
- PRIMARY_VARIABLE: `COORDINATOR_SUBJECT_MUTATION_SCOPE_ENFORCED`.
- ALLOWED_PATHS: `src/main/java/co/edu/uco/asistenciasuco/application/features/coordinador/gestionarasignatura/`, `src/main/java/co/edu/uco/asistenciasuco/application/secondaryports/academic/`, `src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/controller/coordinador/CoordinadorPortalController.java`, `src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/wiring/CoordinadorWiringConfiguration.java`, tests equivalentes y esta carpeta de work item.
- FORBIDDEN_PATHS: `schema/`, repositorio DB externo, OpenAPI canónico, nombres/status HTTP públicos, `SecurityConfig`, PEP, frontend, `.env*`, secretos y cualquier endpoint nuevo.
- CONTRACTS: rutas existentes de Coordinador y sus estados/respuestas; ports internos pueden recibir el actor autenticado.
- PROVIDERS: `InstitutionalScopePort`, `AsignaturaQueryPort`, `PlanEstudioQueryPort`, `AsignaturaCommandPort`.
- EXTERNAL_ENVIRONMENT: SQL Server real requerido para confirmar paridad/defensa DB; si no está disponible, los IT quedan `NOT_RUN`.
- SECURITY_IMPACT: reduce mutación cross-program; actor no confiable del body nunca se usa.
- OBSERVABILITY_IMPACT: sin cambio; errores deben seguir el handler existente.
- TEST_LEVEL_REQUIRED: unit tests Application + controller mapping + wiring/ArchUnit; integración SQL Server para confirmar provider y no-escritura si el ambiente está disponible.
- ROLLBACK: revertir solo el commit propio de este work item; no descartar cambios de usuario.
- STOP_CONDITIONS: contradicción de contrato, test RED que demuestre que `ADMINISTRADOR` debe mutar asignaturas por contrato, query provider incapaz de resolver pertenencia, o DB owner no aprueba evolución del provider.

## Alcance

- Propagar actor autenticado en update/toggle.
- Aplicar guard de programa y pertenencia de plan/asignatura en Application.
- Mantener endpoints, payloads, códigos HTTP y provider SQL sin cambios.
- Añadir tests RED/GREEN y documentar limitaciones.

## No alcance

- Cambiar stored procedures, crear índices o modificar esquema.
- Añadir paginación, filtros, CRUD de docentes, grupos o periodos.
- Migrar a PEP o MinIO.

## Archivos afectados

EXISTENTES: controller, input/use case/interactor de gestión de asignatura, wiring, puertos y tests correspondientes. NUEVOS: solo tests adicionales si el caso no tiene cobertura y documentos de este work item. RETIRAR: ninguno.

## Definition of Ready

`NOT_READY` hasta tener exit code del gate base actual y snapshot RED aprobado. El requisito, consumidores, rutas y provider están identificados; la aprobación del owner DB para cambiar sus scripts no existe, por eso este plan no autoriza cambios SQL.

## Addendum de ejecución — 2026-10-18

- El snapshot RED quedó capturado antes de la implementación: `test-compile` terminó con exit code 1 por los contratos ausentes de actor/puertos/wiring descritos en `TEST_PLAN.md`.
- El gate base terminó con exit code 0 usando Java 25 de Maven y la combinación JaCoCo + Mockito documentada en `VALIDATION.md`.
- La mitigación Application quedó implementada y probada. El work item permanece sin cierre funcional porque la defensa dentro de `usp_actualizar_asignatura` y `usp_toggle_estado_asignatura` no pertenece a este alcance y la prueba SQL Server no se ejecutó.
