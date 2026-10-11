---
status: DRAFT_FOR_DB_OWNER_REVIEW_NOT_EXECUTABLE
target-repository: johnjduque/gestion-asistencia-db
target-base: develop@f2871a9564d6c4cc5abc3745854414243bfda238
freeze: DB-GP-001C
requested-work-item: UTC-D06-POST-FREEZE
---
# Propuesta entregable al owner DB — integridad de origen de horas

**Autoría**: propuesta técnica de backend. **Autorización**: solo el owner DB puede abrir/aprobar work item, editar shape congelado, migrar/firmar. No reemplazar esas firmas por nuestro documento. Intento de crear una rama en el repo DB mediante conexión GitHub devolvió 403 "Resource not accessible by integration": este borrador se publica exclusivamente en backend para revisión del owner.

## Problema y evidencia
`dbo.Sesion` tiene dos `datetime2` sin offset y el baseline indica que representan instantes target UTC; el dato histórico no permite probar si cada valor se escribió UTC. Hay inserts por `usp_crear_sesion`, `usp_generar_sesiones_grupo`, MERGE de semillas; update por `usp_actualizar_sesion` y scripts privilegiados. El error demostrado: puede cambiarse 08:00→18:00 sin registro de procedencia. Un CHECK de los 3 valores es necesario, pero insuficiente; **no** detecta modificación silenciosa de horas.

## Diseño de migración solicitado
1. Nueva columna `Sesion.procedenciaTemporal NVARCHAR(24) NULL` sin default y CHECK con valores `UTC_V2`, `UTC_GENERADOR`, `UTC_OWNER` o NULL; preservar todos los valores temporales existentes byte por byte. SQL Server CHECK permite NULL (UNKNOWN), que corresponde a INDETERMINADA.
2. Cambiar **tres procedimientos** `usp_crear_sesion`, `usp_actualizar_sesion`, `usp_generar_sesiones_grupo` manteniendo las cuatro columnas de retorno y parámetros existentes nombrados. Un parámetro interno opcional, en último lugar y default NULL, puede permitir registrar UTC_V2 a través del backend v2 si es la opción autorizada. Asegurar que v1 recibe default NULL y mantiene semántica. **No aceptar libremente un campo de procedencia desde el JSON del cliente**.
3. En el propio INSERT/UPDATE escribir `fechaHoraInicio`, `fechaHoraFin` y `procedenciaTemporal` juntos. v2 => UTC_V2; v1 => NULL (incluso si el valor temporal es idéntico); generador => UTC_GENERADOR cuando la conversión IANA/Windows para fecha concreta es validada. Si falla generación/conversión, ninguna fila con marca positiva debe persistirse.
4. Añadir nueva **vista** `dbo.uv_sesion_v2` con 10 columnas canónicas de `uv_sesion` + `procedenciaTemporal` desde `Sesion`; proyectar marca en la misma SELECT, sin otra consulta temporal ni inconsistencia de snapshot. **No alterar** `uv_sesion` ni `uv_auth_sesion` porque el baseline exige shape exacto de la primera y los clientes v1 dependen de ambas. Si hace falta una variante autorizada para RBAC, crear `uv_auth_sesion_v2` adicional sin tocar contrato anterior, tras inventario de consumidores/permiso.
5. Un procedimiento administrativo de clasificación controlado (`usp_clasificar_procedencia_sesion` o migración transaccional firmada) será el **único** camino de owner; exigir manifiesto por ambiente/ID/estado anterior y posterior/fuente/zona/acción/autor/aprobador/hash, y auditar mismo commit. NULL si no hay evidencia; no actualizar 3 filas locales automáticamente.
6. Restringir DML directo en `dbo.Sesion` para principals runtime y seed; permitir EXEC a SP autorizados conservando RBAC y ownership. Probar permisos **efectivos** usando misma identidad SQL runtime; no confiar solo en permisos declarados y no conceder `db_owner`. Evaluar módulo firmado, trigger de rechazo y auditoría con dueño DB: un trigger no puede verificar que `SET procedenciaTemporal` aparecía en la sentencia si asigna el mismo valor; tampoco `SESSION_CONTEXT` es una prueba fiable ante un usuario capaz de escribirlo. Prueba obligatoria: UPDATE directo 08→18 conservando etiqueta confirmada es denegado/revertido. Los privilegiados `sysadmin` quedan dentro de régimen de auditoría DBA, no cobertura criptográfica del esquema.
7. Decidir qué representan las horas duplicadas en `DetalleAsistencia` (snapshot al tomar asistencia o valor sincronizado). Si snapshot, mantenerlas inmutables y documentarlo; si valor vigente, implementar mantenimiento transaccional y tests. No asumir sincronía ni hacer UPDATE automático sin política.

## Diseño lógico del SP (PSEUDOCÓDIGO — NO EJECUTAR)
```text
usp_crear_sesion(existing named params, @origenInterno [opt] = NULL):
  autenticar usuario/rol/titularidad como antes
  validar inicio y fin UTC; validar fin > inicio
  determinar marca SOLO desde entrada confiable del adaptador protegido
  INSERT Sesion (..., fechaHoraInicio, fechaHoraFin, procedenciaTemporal)
       VALUES (..., inicio, fin, marca)
  preservar respuesta canónica de cuatro columnas

usp_actualizar_sesion(existing named params, @origenInterno [opt] = NULL):
  validar autorizaciones, existencia, fechas y rango
  UPDATE Sesion SET fechaHoraInicio = inicio, fechaHoraFin = fin,
                    procedenciaTemporal = marca WHERE id = idSesion
  preservar respuesta de cuatro columnas

usp_generar_sesiones_grupo:
  resolver TIEMPO/ZONA_HORARIA_SQLSERVER y conversión real a UTC
  INSERT Sesion (..., fechaHoraInicio, fechaHoraFin, procedenciaTemporal)
       VALUES (..., inicioUtc, finUtc, UTC_GENERADOR)
```
Nota: distinguir v1/v2 por parámetros de SP no constituye autenticación DB por sí misma si ambas versiones usan el mismo principal SQL. El owner debe definir threat model y mecanismo de confianza; una invocación arbitraria de SP por actor DB privilegiado nunca debe conceder falsamente procedencia si se exige protección end-to-end. Alternativa de seguridad más fuerte: SPs v2 separados con permisos por principal de servicio y validación de token en backend, decisión explícita del owner.

## Compatibilidad y prueba de no-regresión
- `SELECT * FROM dbo.uv_sesion` devuelve EXACTAMENTE columnas 10 y mismo orden; igual en `uv_auth_sesion` para su contrato actual.
- Rutas v1 no cambian; Entity `UvSesionEntity` no gana campo por defecto; consulta JPA v2 mapea nueva `UvSesionV2Entity`.
- SP públicos mantienen `idCorrelacion, mensajeUsuarioResultado, mensajeTecnicoResultado, estadoResultado` en ese orden.
- `usp_crear_sesion` respeta transacciones y savepoints y `usp_generar_sesiones_grupo` lock correlativo `UPDLOCK,HOLDLOCK`; no introducir cambios a atomicidad o generación de código/numero.

## Plan de ejecución del owner y rollback
0. Backup restaurado de ensayo, manifiesto de ambiente aprobado, permisos, locks y tamaño, prueba de restauración; preparar bitácora DB de rollout/rollback y migración inversa de código.
1. Esquema compatible y NULL para históricos; no atribuir marcas retroactivas por defecto.
2. Procedimientos, vista nueva, permisos de DML directo/EXEC y auditoría; correr v1 golden path y snapshot de vistas exactas.
3. Pruebas en SQL Server real (incluidas negativas): direct UPDATE sin permiso, UPDATE que altera horas manteniendo marca, fin <= inicio, rollback de SP, 2 sesiones simultáneas del mismo grupo, generador idempotente, perfil docente ajeno, seed.
4. No clasificar históricos hasta manifiesto firmado del owner funcional; fila por fila y entorno por separado.
5. Desplegar DB primero; desplegar backend v2 solo tras paridad; observar v1 coexistente. Rollback backend primero, luego DB bajo supervisión. Antes de quitar columna exportar `id/fechaHoras/procedenciaTemporal/evidence` y verificar respaldo; de otro modo la eliminación pierde marcas irrecuperables.

## Obligaciones de aceptación
| Elemento | Responsable | Evidencia necesaria |
|---|---|---|
| Freeze y nuevo ID work item | owner DB | rama/PR y aprobación formal |
| Cambio tabla + CHECK y 3 SP | owner DB | diff, SQL real y regresión |
| Vista v2 nueva; v1 intacta | owner DB + contratos | snapshot SELECT * pre/post |
| Control de DML directo y auditoría | owner DB/seguridad | prueba con runtime login |
| Clasificación de datos históricos | owner funcional | manifiesto firmado por ID y ambiente |
| HTTP errores/rutas/proyección | owner contratos + backend | versión OpenAPI aprobada |
| QA entrega | tester/backend | IT sin skips focales, rollback certificado |
