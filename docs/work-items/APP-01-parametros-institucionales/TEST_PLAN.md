# TEST_PLAN — APP-01 parámetros institucionales

## Alcance y autoridad

Fase A únicamente: conservar lectura y evitar que la interfaz intente una escritura sin contrato. Contrato AS-IS en CONTRACT_MATRIX.md. La persistencia no tiene contrato aprobado; sus pruebas quedan pendientes de la decisión de los owners.

## Behavioral Matrix — Fase A

| ID | Requirement | Scenario | Input/State | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|
| APP01-FE-01 | La consulta existente continúa | Administrador abre la pantalla | Respuesta GET exitosa con DTO real: id, grupo, clave, valor, tipoDato, valorDefecto, estaActivo | La señal y el card conservan grupo, clave, valor, tipo y estado | Se muestran los campos recibidos; no se inventan nombre o categoría | Eliminar la carga o reemplazar la respuesta con modelo ficticio | Service/component |
| APP01-FE-02 | No ofrecer una edición no disponible | Se muestra la pestaña de parámetros | Cualquier parámetro retornado | Aviso visible de modo consulta; campo de valor de solo lectura; no hay botón Guardar ni controles booleanos accionables | Edición claramente no disponible | Dejar CTA habilitado con toast o mutación local | Component |
| APP01-FE-03 | Ningún éxito falso ni solicitud de escritura | Usuario intenta interactuar con la card | Pantalla en modo consulta | Ninguna llamada PATCH/PUT; valor de señal sin cambios; sin toast de éxito | No se presenta el valor como guardado | Enviar request, editar señal optimísticamente o emitir éxito antes de respuesta | Component/service |
| APP01-FE-04 | Error de lectura no reemplaza el estado | GET termina con error | Estado previo o lista vacía | No hay cambio a un valor enviado por el usuario ni mensaje de guardado | La lectura fallida no se presenta como actualización | Convertir un error de carga en éxito o mutación | Service/component, si la implementación actual ofrece observación del error |

Los detalles de APP01-FE-04 se limitan a las capacidades observables existentes; no se inventa catálogo de errores del backend.

## Matriz futura — escritura (NOT_READY)

Cuando se apruebe el contrato: método/ruta/body; validación de id y valor por tipo; RBAC ADMINISTRADOR; id inexistente/inactivo; código/error real; ausencia de mutación ante fallo; rollback DB; refresh GET; idempotencia y concurrencia si el owner los congela; prueba SQL oficial segura; persistencia tras recarga.

## RED_SNAPSHOT

Base: Frontend commit 00b60f0877288d950753dbedc9e843e0a3400145, rama fix/app01-parametros-contrato.

| Archivo RED | SHA-256 |
|---|---|
| src/app/features/admin/admin-system/admin-system.component.spec.ts | 608972E8ECED26CE3CED32BBC80FC4228E533951BC8E84104AB0BF31C1DDA7B5 |
| src/app/core/services/admin-management.service.spec.ts | 3FCAD66F2A90FDC20BE3F7A1D70F6F9356FDA701696CCF47B495A84A052E83AD |

Comando: npm test -- --watch=false --browsers=ChromeHeadlessCI --include=src/app/features/admin/admin-system/admin-system.component.spec.ts --include=src/app/core/services/admin-management.service.spec.ts. Fecha: 2026-11-02. Exit code: 1. Resultado: 30 tests ejecutados, 28 PASS, 2 FAIL por la causa esperada. La pantalla no muestra aviso/grupo/tipo, deja editable el input y mantiene Guardar; AdminManagementService sigue exponiendo actualizarParametro. No hubo errores de compilación ni fallos no relacionados. El implementador conservará estos tests RED sin cambios durante la implementación.

Antes del GREEN se corrigió solamente el tipo del fixture del componente de ParametroInstitucionalItem simulado a ParametroAdminDto observado. Las expectativas de comportamiento no cambiaron. Hash actual del spec de componente: 5CBF89ED1408A90E6ABD9ADEF315668AF828EB8B326E28AA364F926346DFADBE. El spec del servicio conserva SHA-256 3FCAD66F2A90FDC20BE3F7A1D70F6F9356FDA701696CCF47B495A84A052E83AD. La diferencia del fixture se justifica en VALIDATION.md.

## Validación manual

Obligatoria en navegador integrado. No automatizar login ni leer credenciales. El recorrido de modificación/persistencia está NOT_RUN hasta la aprobación del contrato y una selección de parámetro autorizada; fase A permite comprobar únicamente lectura y mensaje de no disponibilidad.
