---
status: active
type: work-item
scope: backend-frontend-db-contract-analysis
owner: backend-team
last-reviewed: 2026-11-02
---

# APP-01 — Parámetros institucionales

## Identidad y objetivo

- Fecha / autor / base Git: 2026-11-02 / Codex, implementador / Backend bd1ecd396ea04e27fc386cace847720c1c4cf0c5; Frontend 00b60f0877288d950753dbedc9e843e0a3400145; DB fe1ccd710aed4f84416bdfe03da36976797ce7df.
- Objetivo: eliminar el mensaje falso de guardado y la solicitud de escritura no contractual; conservar la lectura real de parámetros y mostrar con claridad que editar aún no está disponible.
- Criterio de aceptación de la fase A: la pantalla conserva los valores obtenidos por GET, no envía PATCH/PUT, no cambia señales locales ni anuncia éxito, y explica que los parámetros están en modo consulta.
- Alcance futuro de APP-01: implementar escritura transaccional real después de aprobar y congelar el contrato HTTP y el contrato DB.
- Skills/fuentes: uco-contratos, uco-arquitectura, uco-persistencia, uco-testing; API_DESIGN_RULES, OPENAPI_STANDARD, HTTP_AS_IS_MATRIX, DB_BASELINE_CONTRACT y CONTRACT_ALIGNMENT_PROTOCOL.

## AS-IS y evidencia

| Hecho | Archivo + símbolo / contrato + versión | Evidencia y límites |
|---|---|---|
| La lectura del administrador existe en Backend. | Backend, AdminPortalController.consultarParametros; ParametroDTO; HTTP_AS_IS_MATRIX, commit bd1ecd3. | GET responde ApiListResponse y delega en ConsultarParametrosInputPort. |
| El controlador también registra un PUT sin cuerpo para actualizar, pero lanza FeatureUnavailableException. | Backend, AdminPortalController.actualizarParametro, commit bd1ecd3. | La ruta no realiza escritura. La matriz HTTP la enumera como AS-IS. |
| El OpenAPI canónico no contiene una operación para parámetros. | Backend, docs/contracts/openapi/openapi-golden-path.yaml, commit bd1ecd3. | No hay contrato congelado para GET ni para actualización en este archivo. |
| Angular realiza GET y PATCH; el servicio se suscribe internamente y devuelve void. | Frontend, AdminManagementService.cargarParametros/actualizarParametro, commit 00b60f0. | PATCH envía {valor}; el consumidor no puede observar resultado/error. |
| Angular muestra toast de éxito inmediatamente después de llamar el servicio. | Frontend, AdminSystemComponent.actualizarValorParam, commit 00b60f0. | No espera la respuesta HTTP. |
| El tipo de escritura no coincide entre el consumidor y el provider. | Frontend PATCH frente a Backend PUT/501; OpenAPI sin operación. | MISMATCH / DECISION_REQUIRED. No elegir método por suposición. |
| DB ofrece lectura de parámetros activos mediante uv_parametro. | DB, DB_BASELINE_CONTRACT y schema/views/uv_parametro.sql, commit fe1ccd7. | La vista lee CatalogoParametro. No se encontró procedimiento de escritura de parámetros ni puerto/adaptador de escritura en Backend. |
| El registro DB contiene id UNIQUEIDENTIFIER, valor NVARCHAR(MAX), tipoDato VARCHAR(20), estaActivo BIT y fechas; el DTO de lectura no expone todas las columnas de la vista. | DB CatalogoParametro/uv_parametro; Backend ParametroDTO. | La columna disponible no autoriza escritura directa. La validación de tipo y reglas de edición no están congeladas. |
| El modelo Angular de parámetros no corresponde al DTO de lectura del Backend. | Frontend ParametroInstitucionalItem frente a Backend ParametroDTO. | MISMATCH AS-IS. La fase A usa solamente clave y valor existentes; no inventa nombres, categorías o equivalencias de tipo. |

## TARGET

### Fase A — estado seguro de la interfaz

La pantalla conserva la consulta y los valores observados. El tipo del consumidor refleja los campos que el DTO backend realmente entrega: id, grupo, clave, valor, tipoDato, valorDefecto y estaActivo. Los controles de edición dejan de ser accionables y se muestra un aviso claro de modo consulta. No existe llamada de escritura ni actualización optimista de estado. Esta fase no cambia el contrato HTTP ni DB.

### Fase B — escritura institucional

Pendiente de decisión/versionado del método, payload, response/error contract, validaciones por tipo, conjunto de parámetros editables, actor/auditoría, transacción y concurrencia DB. No se implementa mientras la DoR de esta fase sea NOT_READY.

## Clase de cambio y alcance de rutas

- Change class: BEHAVIOR_CHANGE para fase A; la fase B requerirá su propio dictamen DoR.
- PRIMARY_VARIABLE: comportamiento observable de edición de parámetros en la pantalla admin.
- ALLOWED_PATHS:
  - Frontend: src/app/core/models/role-management.model.ts; src/app/core/services/admin-management.service.ts y su spec; src/app/features/admin/admin-system/admin-system.component.ts y su spec.
  - Backend: este work item, docs/work-items/README.md y docs/baseline/LINEA_BASE.md.
- FORBIDDEN_PATHS: Backend src/**, OpenAPI y contratos activos; Frontend rutas fuera de los cuatro archivos permitidos; todo archivo DB y dato persistente.
- CONTRACTS: GET existente documentado en HTTP_AS_IS_MATRIX; la fase A no publica ni consume contrato de escritura.
- PROVIDERS: lectura existente por uv_parametro; no hay provider de escritura identificado.
- EXTERNAL_ENVIRONMENT: navegador integrado y servicios oficiales existentes, solo lectura durante la validación manual.
- SECURITY_IMPACT: conservar las guardas/RBAC existentes. No cambiar autorización.
- OBSERVABILITY_IMPACT: ninguno en fase A.
- TEST_LEVEL_REQUIRED: tests unitarios/component de Frontend focalizados y recorrido manual de lectura en navegador. Persistencia de escritura: NOT_RUN hasta contrato aprobado.
- ROLLBACK: revertir los cambios de Frontend de la fase A; no hay migración, dato, servicio o proceso que restaurar.
- CONSUMERS: AdminSystemComponent consume AdminManagementService; no se encontraron otros consumidores de actualizarParametro.
- STOP_CONDITIONS: cualquier requisito de enviar PATCH/PUT, ampliar campos públicos, cambiar RBAC o mutar DB detiene la fase A y vuelve a contrato/owner.

## Alcance

- Retirar la acción de escritura no respaldada y el toast prematuro.
- Mantener la consulta GET y mostrar el valor actual como lectura.
- Dejar documentada la desalineación HTTP/DB y preparar la propuesta no vinculante de contrato DB.

## No alcance

- Implementar PUT/PATCH, crear puertos/casos de uso/adaptadores de escritura o editar OpenAPI.
- Cambiar esquema, vistas, procedimientos, semillas o datos DB.
- Modificar otros hallazgos, auditar el sistema, repetir validaciones de APP-02, crear infraestructura o datos de prueba.
- Declarar APP-01 resuelto de extremo a extremo.

## Archivos afectados

EXISTENTES permitidos: los cinco archivos frontend del plan y los dos índices de work items/baseline.

NUEVOS: PLAN.md, TEST_PLAN.md, CONTRACT_MATRIX.md, DB_WRITE_CONTRACT_PROPOSAL.md y VALIDATION.md dentro de este work item. No se crea CLOSURE.md mientras la fase B permanezca NOT_READY.

RETIRAR: ninguno.

## Contratos y consumidores afectados

- DOMAIN: no aplica a fase A.
- HTTP: GET se conserva; fase A no realiza escritura. OpenAPI omite actualmente la operación y requiere decisión antes de congelar un TARGET.
- PERSISTENCE: GET por uv_parametro. El owner DB no ofrece hoy un contrato de escritura encontrado.
- SECURITY: conservar la protección vigente para ADMINISTRADOR; la fase A no modifica permisos.
- REALTIME: no aplica.
- Consumidor: Angular AdminSystemComponent; el servicio deja de exponer la llamada PATCH sin contrato.

## Riesgos y dependencias

- La interfaz queda en modo consulta mientras el owner API y el owner DB resuelven contratos.
- No se sabe qué claves o tipos de parámetros son editables ni sus validaciones.
- El DTO actual no contiene nombre, descripción, categoría ni fechaModificacion; la fase A muestra los campos existentes y no inventa estos valores.
- Sin integración de escritura aprobada, no se puede comprobar persistencia ni declarar corrección completa.

## Test plan

Ver TEST_PLAN.md. Flujo fase A: requisito → contrato AS-IS → matriz → RED → implementación → GREEN → VALIDATE. El TEST_PLAN no autoriza tests de escritura con rutas o reglas inventadas.

## Rollback y stop conditions

No hay cambios DB ni backend de producción. Si el contrato HTTP/DB se contradice o falta una decisión de owner, clasificar DECISION_REQUIRED/BLOCKED_BY_MISSING_EVIDENCE y no implementar fase B.

## Deuda conocida y validación manual

- TD: no asignado; deuda funcional APP-01 sigue abierta hasta habilitar escritura real.
- MV: recorrido autenticado de lectura en navegador completado para fase A; persistencia de escritura será NOT_RUN mientras no exista contrato aprobado.

## Definition of Ready

- Fase A (interfaz honesta de solo lectura): READY. El requerimiento de no simular escritura proviene de la instrucción del usuario; se conserva solo GET, no se cambia el contrato backend. El CI de Frontend para la base exacta 00b60f0 reporta build-test-verify success (2026-11-02). Tests RED focalizados se congelarán antes del cambio de producción.
- Fase B (actualización persistente): NOT_READY. OpenAPI no define la operación; Frontend PATCH y Backend PUT/501 difieren; no hay puerto/adaptador ni procedimiento DB de escritura encontrado; faltan decisión de API owner y aprobación/versionado del owner DB, reglas de tipos/valores y criterios de auditoría/concurrencia. Véanse CONTRACT_MATRIX.md y DB_WRITE_CONTRACT_PROPOSAL.md.

## Estado

APP-01 parcialmente bloqueado. La fase A quedó implementada y validada mediante tests focalizados y recorrido de lectura en navegador. El resultado end-to-end sigue pendiente hasta que la fase B pase su propia DoR.
