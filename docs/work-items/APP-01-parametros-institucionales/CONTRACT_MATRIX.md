# CONTRACT MATRIX — APP-01

Aplicación del protocolo de alineación contractual. Snapshot capturado el 2026-11-02. Contrato congelado: DRAFT.

## Evidencia

| Sistema | Repo/versión | Archivo/objeto | Hash/commit | Autoridad |
|---|---|---|---|---|
| Backend | alejovh12/AsistenciasUCO, develop | openapi-golden-path.yaml | bd1ecd396ea04e27fc386cace847720c1c4cf0c5 | OWNER HTTP; no declara parámetros |
| Backend | alejovh12/AsistenciasUCO, develop | HTTP_AS_IS_MATRIX.md; AdminPortalController; ParametroDTO | bd1ecd396ea04e27fc386cace847720c1c4cf0c5 | snapshot AS-IS; no sustituye OpenAPI |
| Frontend | shesho22/AsistenciasUCO-Frontend, develop | AdminManagementService; AdminSystemComponent; FRONTEND_GOLDEN_PATH_CONTRACT.md | 00b60f0877288d950753dbedc9e843e0a3400145 | CONSUMER; el documento Golden Path no cubre parámetros |
| DB | johnjduque/gestion-asistencia-db, develop | DB_BASELINE_CONTRACT.md; CatalogoParametro; uv_parametro | fe1ccd710aed4f84416bdfe03da36976797ce7df | OWNER persistencia |

## Matriz

| ID | Capacidad | Provider contract | Backend expectation | Status | Evidence | Impact | Owner action |
|---|---|---|---|---|---|---|---|
| C01 | Consulta de parámetros | DB uv_parametro expone parámetros activos; Backend GET documentado en HTTP_AS_IS_MATRIX y AdminPortalController | Frontend GET /api/v1/admin/parametros | BLOCKED_BY_MISSING_EVIDENCE | DB view, matriz HTTP, controller, servicio Angular; operación ausente del OpenAPI canónico | GET funciona como AS-IS, pero falta contrato canónico de fields/compatibilidad | API owner debe registrar/aprobar operación y response antes de ampliar su consumo |
| C02 | Forma del DTO de lectura | Backend ParametroDTO: id, grupo, clave, valor, tipoDato, valorDefecto, estaActivo | Frontend ParametroInstitucionalItem espera id, clave, nombre, descripcion, valor, tipo, categoria, ultimaModificacion | MISMATCH | símbolos de ambos repositorios y commits de snapshot | nombre/categoría/fecha y equivalencias de tipo no están en response API | API y Frontend owners deciden si ampliar DTO o adaptar un consumidor con mapping contractual |
| C03 | Método HTTP de escritura | OpenAPI no define la operación; Backend declara PUT sin body y retorna 501 | Frontend envía PATCH con {valor} y supone un DTO actualizado | MISMATCH | AdminPortalController, AdminManagementService, API_DESIGN_RULES | El usuario no puede conocer resultado real; endpoint consumidor no tiene provider | API owner congela método, payload, status, response, errores y compatibilidad del PUT legado |
| C04 | Persistencia de escritura | DB publica uv_parametro de lectura; no hay SP/contrato de mutación de parámetro encontrado | Backend no tiene command port ni adapter para parámetros | MISSING_IN_PROVIDER | DB_BASELINE_CONTRACT, schema/views/uv_parametro, inventario de stored-procedures, Backend src/main | No existe persistencia autorizada; acceso directo a tabla no está aprobado | Owner DB define/autoriza contrato y work item; luego Backend integra por capas |
| C05 | Reglas de valor | DB columna valor NVARCHAR(MAX), tipoDato VARCHAR(20) | Frontend manda valor como string; Backend no valida ni procesa | BLOCKED_BY_MISSING_EVIDENCE | CatalogoParametro y DTO | No se conoce conjunto de claves editables, valores aceptados, longitudes ni casos sensibles | Owners aprueban matriz de tipos y restricciones antes de RED |

## Tipos y campos

| Campo | DB type/nullability | Java type/nullability | Semántica | Status |
|---|---|---|---|---|
| id | UNIQUEIDENTIFIER NOT NULL | UUID | Identificador del parámetro | MATCH para lectura |
| grupo | VARCHAR(100) NOT NULL | String | Grupo de catálogo | Backend lo expone; Frontend no lo tipa |
| clave | VARCHAR(100) NOT NULL | String | Clave estable | MATCH para lectura |
| valor | NVARCHAR(MAX) NOT NULL | String | Valor configurado | Campo que el Frontend intenta modificar; reglas por tipo no definidas |
| tipoDato | VARCHAR(20) NOT NULL | String | Tipo declarativo; no se encontró constraint de valores en el esquema leído | DECISION_REQUIRED |
| valorDefecto | NVARCHAR(MAX) NOT NULL | String | Valor por defecto | Solo lectura; el DTO lo expone |
| estaActivo | BIT NOT NULL | boolean | La vista solo incluye filas activas | MATCH para lectura |
| fechaModificacion | DATETIME NOT NULL | No expuesto por ParametroDTO | La vista y baseline describen semántica UTC | MISSING_IN_CONSUMER |

## Códigos/enums

No se congela una lista de tipoDato ni valores permitidos. Debe aprobarla el owner DB/producto; no inferirla de una pantalla o semilla.

## Error semantics

La actualización no tiene status, código ni response aprobado. No se agregan códigos HTTP por convención. El endpoint PUT actual se traduce como funcionalidad no disponible.

## Transacción/atomicidad

No existe contrato de escritura. La propuesta exige cambio atómico, rollback ante validación/fallo y lectura posterior; detalles de bloqueo/concurrencia y auditoría requieren aprobación DB.

## Tiempo/UTC

DB baseline documenta fechaModificacion UTC target. La respuesta actual ParametroDTO no la expone. No hay cambio de tiempo propuesto para fase A.

## Decisiones requeridas

| ID mismatch | OWNER | CONSUMER | Opciones | Decisión / aprobador / referencia |
|---|---|---|---|---|
| C01-C03 | API owner Backend | Frontend y consumidor institucional | Publicar PATCH parcial con body valor y response acordada; o migrar PUT legado con contrato explícito | PENDIENTE; no aprobado |
| C02 | API owner Backend | Frontend | Extender DTO público o consumir solo campos existentes mediante mapping aprobado | PENDIENTE |
| C04-C05 | Owner DB | Backend | Crear contrato de mutación público autorizado con validación/rollback/auditoría; sin escritura directa desde Backend | PENDIENTE |

## Bloqueos

- BLOCKED_BY_MISSING_EVIDENCE: firma de persistencia, conjunto editable, validación por tipo, auditoría/concurrencia y catálogo de errores.
- DECISION_REQUIRED: método/payload/response HTTP y compatibilidad.

## Contrato congelado

DRAFT. Ninguna operación de escritura se implementará desde esta matriz.
