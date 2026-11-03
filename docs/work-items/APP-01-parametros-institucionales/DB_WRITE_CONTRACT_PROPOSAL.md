# APP-01 — Propuesta no aprobada de contrato de escritura DB

Estado: DRAFT PARA DICTAMEN DEL OWNER DB. No es una firma desplegable, no nombra un procedimiento definitivo y no autoriza SQL, migración, permisos ni cambios de datos.

## Evidencia DB

Snapshot: johnjduque/gestion-asistencia-db, develop, commit fe1ccd710aed4f84416bdfe03da36976797ce7df, 2026-11-02.

- DB_BASELINE_CONTRACT.md incluye uv_parametro como fuente de lectura.
- schema/views/uv_parametro.sql lee dbo.CatalogoParametro y filtra estaActivo = 1.
- dbo.CatalogoParametro declara id UNIQUEIDENTIFIER, valor NVARCHAR(MAX), tipoDato VARCHAR(20), estaActivo BIT y fechaModificacion DATETIME.
- No se encontró un procedimiento almacenado de escritura de parámetros ni un contrato asociado en el inventario leído.
- El runtime DB permite EXECUTE en SP públicos y SELECT en vistas, no permisos directos sobre tablas base, según AGENTS.md.

## Necesidad funcional

Actualizar el campo valor de un parámetro institucional existente y activo, validar el valor contra el tipoDato y devolver resultado trazable. Backend debe obtener el actor del principal autenticado y ejecutar por el contrato DB aprobado. La interfaz debe consultar nuevamente por GET después del éxito.

## Firma lógica candidata

Los nombres SQL finales y la forma pública/interna los define el owner DB. Para poder evaluar el alcance, la operación necesitaría como mínimo:

| Parámetro lógico | Tipo candidato | Regla pendiente |
|---|---|---|
| Identificador del parámetro | UNIQUEIDENTIFIER NOT NULL | Debe resolver una fila activa. Nombre SQL y status para inexistente/inactivo pendientes. |
| Nuevo valor | NVARCHAR(MAX) NOT NULL | Validar según tipoDato; longitud máxima funcional pendiente. |
| Correlación | UNIQUEIDENTIFIER NOT NULL | Integrar con la correlación HTTP conforme al estándar DB. |
| Actor autenticado | UNIQUEIDENTIFIER NOT NULL | Lo resuelve Backend; no aceptar identidad enviada por Angular. Definir registro de auditoría. |

No se propone un nombre dbo.usp_* concreto ni se envía tipoDato desde Angular: la DB debe leer el tipo vigente para validar el valor. El actor/correlación solo se incluirán en la firma final si el owner confirma la estrategia de auditoría/trazabilidad.

## Salida candidata

Si el owner DB confirma el estándar vigente de SP públicos, el resultset de control sería: idCorrelacion UNIQUEIDENTIFIER, mensajeUsuarioResultado NVARCHAR(4000), mensajeTecnicoResultado NVARCHAR(4000), estadoResultado BIT. Los SP públicos no llevan parámetros OUTPUT conforme a la documentación DB leída.

El Backend haría después GET de lectura para obtener el estado persistido. La respuesta HTTP (mensaje vacío vs DTO actualizado) queda pendiente del API owner y no se infiere de este resultset.

## Validaciones que debe resolver el owner

- id válido, fila existente y estaActivo = 1.
- valor no nulo y validación según el catálogo cerrado de tipoDato.
- definición de tipos aceptados y restricciones por parámetro, sin permitir cambios a claves sensibles no autorizadas.
- comportamiento para valor idéntico, actualización concurrente, modificación de fecha y auditoría del actor.
- transacción única: si la mutación o validación falla, ningún dato cambia.
- error catalogado para valor incompatible, parámetro inexistente/inactivo y error inesperado.
- privilegios de runtime limitados a EXECUTE del contrato público; ningún UPDATE directo desde Backend.

## Decisión solicitada

Owner DB: confirmar si autoriza un work item DB contractual para crear la operación; aprobar la matriz de claves/tipos/validaciones, auditoría y concurrencia; proponer la firma y las pruebas SQL oficiales; versionar addendum/work item y aprobación antes de implementar.

API owner Backend: congelar método, request, response, códigos de error y tratamiento compatible del PUT legado en OpenAPI antes del backend.

Hasta registrar ambas decisiones, la persistencia y validación de escritura permanecen NOT_READY. No hay rama DB ni cambios en el repositorio DB para APP-01.
