---
status: DB_OWNER_WORK_ITEM_REQUIRED
work-item: MAINT-003D
target-repository: johnjduque/gestion-asistencia-db
freeze: DB-GP-001C
---
# Entrega al owner DB: D06 A+C+E y bloqueo de escrituras huérfanas

**NO es una migración ejecutable.** El owner de DB debe abrir su propio work item posterior al freeze y someter scripts, contrato, respaldos y pruebas a aprobación. No se toca el repo DB desde MAINT-003D.

## Evidencia de código revisada en `develop`
- `schema/tables/Sesion.sql`: `fechaHoraInicio/Fin datetime2`, sin procedencia.
- Escriben: `usp_crear_sesion` (INSERT), `usp_actualizar_sesion` (UPDATE), `usp_generar_sesiones_grupo` (INSERT), `schema/seed/14_grupos_sesiones.sql` (MERGE).
- Vistas `uv_sesion`, `uv_auth_sesion` seleccionan columnas explícitas.
- `DetalleAsistencia` almacena otras copias de inicio/fin: verificar su semántica y sincronización; NO actualizar retrospectivamente sin contrato.
- El generador usa `TIEMPO/ZONA_HORARIA_SQLSERVER` y `AT TIME ZONE`; capturar configuración y reglas DST usadas; no certificar la marca si zona ausente o inválida.

## Modelo propuesto
Agregar `dbo.Sesion.procedenciaTemporal NVARCHAR(24) NULL` y CHECK **permitiendo únicamente** `UTC_V2`, `UTC_GENERADOR`, `UTC_OWNER` o `NULL`. Es un diseño lógico, no SQL para ejecutar. Todos los registros preexistentes quedan `NULL` inicialmente. Nunca usar DEFAULT `UTC_V2`.

Interpretación:
- `NULL`: no sabemos si las horas son UTC; GET v2 no declara instante.
- `UTC_V2`: entrada de HTTP v2 con offset verificado y normalizada UTC.
- `UTC_GENERADOR`: generador autorizado convirtió horario institucional de fecha concreta a UTC y validó zona/reglas.
- `UTC_OWNER`: owner funcional **verificó o convirtió** una fila histórica por ID/ambiente, con acta y evidencia inmutable fuera del único enum.

`UTC_OWNER` no indica por sí solo si fue "ya UTC" o "convertida"; el manifiesto auditable incluye la operación `VERIFICACION` o `CONVERSION`, valores antes/después, ID, entorno, zona original comprobada, fuente, autor, aprobador, timestamp UTC y hash del manifiesto. Si no hay prueba suficiente, la fila queda NULL. El enum no reemplaza auditoría.

## Invariantes atómicas obligatorias
- v2 INSERT / PATCH: hora inicio, hora fin y `procedenciaTemporal='UTC_V2'` en **el mismo INSERT/UPDATE**, no en un segundo statement; 4 columnas canónicas del SP deben permanecer iguales en salida.
- v1 INSERT / PATCH / PUT legado: misma sentencia escribe hora(s) y `procedenciaTemporal=NULL`, aunque la hora coincida. El backend distinguirá caller v1/v2 mediante mecanismo **interno autenticado**; no aceptar bandera de procedencia desde payload libre. Si se reutiliza el mismo SP, agregar parámetro opt-in al final con default v1 NULL y validar quien lo invoca; comprobar compatibilidad JPA con parámetros nombrados, EXEC y default real de SQL Server.
- Generador: mismo INSERT marca `UTC_GENERADOR` solo tras conversión institucional válida, nunca un UPDATE separado.
- Owner clasificación: proceso restringido y transaccional, mismo UPDATE de horas y marca `UTC_OWNER` si convierte; verificación sin cambio de horas también registra decisión y marca en transacción con auditoría. Para reconfirmar una fila no basta conservar una marca vieja.
- Semillas: incluso si futuras semillas solo cambian `nombre`, revisar MERGE y permisos; ninguna semilla nueva debe etiquetar automáticamente históricos. Si cambian horas, anulan marca (`NULL`), salvo proceso expresamente clasificado.

## Defensa contra UPDATE directo
**No basta con CHECK ni con confianza en los SP.** El owner debe:
1. Denegar INSERT/UPDATE/DELETE directos sobre `dbo.Sesion` a principals runtime/seed habituales; aplicación opera mediante EXEC a SP aprobados con propiedad/permisos comprobados bajo la identidad real. Revisar permisos efectivos y cadenas de propiedad sin ampliar roles.
2. Añadir defensa DB (trigger guard, signed modules o mecanismo equivalente aprobado) que **rechace** la modificación de horas sin protocolo de marca y auditoría; no limitarse a conservar valor anterior. Probar explícitamente un UPDATE directo que cambia `08:00`→`18:00` mientras deja procedencia confirmada: debe ser denegado o revertido íntegramente.
3. Evitar confiar únicamente en `SESSION_CONTEXT` que el mismo actor pueda falsificar; los privileged DBAs quedan bajo control/auditoría DBA y no hay garantía criptográfica frente a sysadmin.
4. Añadir prueba de excepción/rollback que garantice que falla todo el UPDATE si se bloquea o viola CHECK.

**Nota de diseño:** no se puede garantizar "se asignó en la misma sentencia" mirando solo `inserted`/`deleted` de un trigger cuando la asignación vuelve a escribir el mismo valor. La garantía exige SP con SET explícito, permisos y pruebas del código, además de observabilidad. El CHECK restringe vocabulario, no demuestra procedencia.

## Impacto y aceptación del owner
Modificar/validar 3 SP de escritura y 2 vistas (`uv_sesion`, `uv_auth_sesion`); inventariar los otros consumidores de `Sesion` (incluidos Excel, estudiantes y asistencia), sin cambiar los wires v1. Cualquier nueva vista o permiso requiere revisión explícita. No asumir que "los otros SP" son inmunes sin búsqueda de código y dependencias actuales.

Matriz de pruebas del work item DB (DB aislada con rollback):
| Caso | Oráculo obligatorio |
|---|---|
| Filas previas | marca NULL tras DDL sin alterar bytes de `fechaHoraInicio/Fin` |
| POST v2 | una sola sentencia INSERT: UTC exacto + UTC_V2 |
| PATCH v2 | misma sentencia UPDATE UTC + UTC_V2 |
| POST/PATCH v1 | marca NULL incluso si antes era UTC_V2 |
| Generador | conversión con zona institucional real, UTC_GENERADOR, idempotencia y rollback |
| Clasificación owner | `UTC_OWNER` únicamente IDs autorizados, trazabilidad antes/después |
| UPDATE directo horas | denegado/revertido; no queda marca obsoleta |
| SET valor fuera de enum | CHECK rechaza + rollback |
| GET vistas | proyectan procedencia, no alteran consulta legacy |
| Permisos y SP | runtime sin DML directo pero con EXEC conforme roles reales |
| DetalleAsistencia | pregunta resuelta: instante histórico vs snapshot de asistencia; test coherencia si aplica |

## Manifest owner funcional (por ambiente, nunca inferir UTC)
```csv
environment,sesion_id,decision,accion,zona_origen_evidenciada,inicio_antes,fin_antes,inicio_utc_aprobado,fin_utc_aprobado,evidence_ref,reviewer,approved_at_utc
```
`decision` = `NO_CLASIFICAR` o `UTC_OWNER`; `accion` = `VERIFICACION`/`CONVERSION`. Cada fila requiere evidencia fechada, firma y entorno. Un `NO_CLASIFICAR` deja NULL; no modificar horas. No ejecutar sobre producción sin backup revisado.

## Plan despliegue/rollback
1. Backup/restauración verificable + snapshot de filas + dependencias, plan de locks, permisos.
2. Migración DB compatible a NULL; SP, vistas y protección DML; pruebas de denegación y golden path con JPA; smoke v1.
3. Desplegar backend v2 solo cuando consumidor de columna esté listo; v1 conserva su wire.
4. Reversión: parar writes v2 y volver backend anterior; recuperar evidencia y `id/procedenciaTemporal` antes de eliminar columna. Deshacer DB solo por migración inversa firmada; el rollback no convierte históricos automáticamente.

**Firmas obligatorias:** owner DB (modelo, work item, protección DML, vistas/SP), owner funcional (IDs/entornos), owner contratos (D01–D09, forma D02/E1).
