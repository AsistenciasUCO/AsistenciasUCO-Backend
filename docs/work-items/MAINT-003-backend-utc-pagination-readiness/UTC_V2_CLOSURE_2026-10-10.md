---
status: active
type: closure-readiness
scope: backend-and-db-coordination
date: 2026-10-10
timezone: America/Bogota
---

# UTC v2 — acta de preparación de cierre

## Resultado ejecutivo

La rama de backend conserva el trabajo local `jose-valencia/maint-003f-utc-v2-implementation` en `2199321871aaa024aae2506d1d807c5a34f7fed3`. La rama local que materializa UTC-D06 en DB es `feat/utc-d06-post-freeze` en `6946a73`; no existe una rama literal llamada `UTC-D06` en ese repositorio.

El cierre funcional todavía queda `NOT_READY`: el build unitario de backend y el quality gate SQL D06/global pasan, pero la integración Java completa contra el SQL Server aislado termina con 11 fallos y 1 error por contratos/fixtures del freeze. No se hizo merge, despliegue en la base principal ni activación productiva de v2.

## Evidencia de pruebas

| Área | Comando/alcance | Resultado | Lectura correcta |
|---|---|---|---|
| Backend unitario, ArchUnit y JaCoCo | Java 25, `clean verify` | `PASS`, 1492 tests, 0 failures, 0 errors, 0 skips; línea 88.80 %, ramas 78.31 % | Supera los gates configurados LINE >=80 % / BRANCH >=70 %. |
| JWT y permisos HTTP | Suite unitaria/contractual incluida en `clean verify` | `PASS` dentro del build | Certifica validators, converter, cadena y aislamiento; no sustituye una prueba contra un IdP/Keycloak real para v2. |
| SQL global aislado | `test_summary.ps1 -ContainerName utc_d06_sqltest` | `PASS`, 179 ejecutados, 178 pass, 0 fail, 1 skip explícito | Incluye catálogo, ownership, atomicidad, inventario público, secretos y concurrencia. |
| UTC-D06 focal | `test/test_utc_d06_procedencia_temporal.sql` | `PASS`, exit 0 | Pasaron columna nullable, históricos, vista, firmas, v1/v2, ownership, permisos, clasificación, DST y rechazo de DML directo. |
| Integración Java real con SQL Server | `clean verify -Pintegration` contra `127.0.0.1:14333` | `BLOCKED`, 191 tests: 11 failures, 1 error, 3 skips | No es PASS: faltan `usp_sincronizar_usuario` y `usp_registrar_o_actualizar_plan_estudio`, hay una excepción de hash, dos discrepancias RAD/RES, seis fixtures de asistencia ausentes y falta `APP_DATABASE_EXPECTED_NAME`. |
| HTTP v2 end-to-end con JWT de IdP real | No hay runner configurado en esta rama | `NOT_RUN` | Requiere entorno controlado con IdP, usuarios, roles DOCENTE/no-DOCENTE, ownership y backend+DB v2. |

Los fallos de integración no se resolvieron modificando tests RED ni cambiando el contrato. Deben volver al owner de DB/fixtures y al tester para dictamen antes de declarar el cierre.

## DB: incrementalidad y preservación

La evidencia observada en `schema/tables/Sesion.sql` es compatible con una migración incremental:

1. La tabla existente no se recrea; la columna `procedenciaTemporal` se agrega solo si no existe mediante `ALTER TABLE` condicional.
2. La columna es `NVARCHAR(24) NULL`, sin `DEFAULT`; por diseño, las filas históricas conservan sus valores `DATETIME2(7)` y quedan con procedencia indeterminada (`NULL`).
3. El `CHECK` solo permite `NULL`, `UTC_V2`, `UTC_GENERADOR` o `UTC_OWNER`.
4. El test sobre `utc_d06_sqltest` dejó 5 sesiones antes y después, las mismas identidades y los mismos timestamps; las 5 quedaron con procedencia `NULL`. No hubo borrado ni reinterpretación.
5. v1 conserva la escritura sin procedencia confirmada; v2 marca `UTC_V2`. El rollback de aplicación debe apagar v2 y conservar la columna/objetos aditivos; no debe convertir históricos ni borrar la marca.

Esto no autoriza aplicar el runner universal directamente sobre la base principal. Antes de un despliegue controlado se requiere backup verificable, snapshot de conteos/identidades/timestamps, preflight de servidor/base/edición, ventana aprobada, aplicación en staging, ejecución del quality gate SQL y aprobación del owner DB. `deploy_schema.ps1` ordena objetos y valida errores, pero no constituye por sí solo una transacción global, backup ni rollback automático.

## `utc_d06_sqltest`: alcance y dependencia

La inspección documentada encontró el contenedor `utc_d06_sqltest` como SQL Server 2022 aislado, publicado en `127.0.0.1:14333`, sin volumen montado y con política de reinicio `no`. La búsqueda de referencias en backend y DB no encontró `utc_d06_sqltest`, `14333` ni `sqltest` en source, compose o workflows. Por tanto, no se identificó dependencia permanente del contenedor; es un entorno de pruebas y no se ha eliminado para preservar evidencia.

Limitación: el test de permisos usa el modelo de roles del script D06, pero el backend de la integración ejecutada se conectó con el usuario administrativo del contenedor. La pertenencia efectiva del principal de runtime y la prohibición de DML bajo ese principal deben validarse en staging con el login/rol real. En particular, una conexión `sysadmin`/`db_owner` no certifica el límite RBAC de runtime.

`SECURITY_FINDING`: una inspección local del entorno del contenedor expuso en la salida de herramienta una variable de secreto. El valor no se copia a este documento ni al repositorio. Si representa una credencial vigente, debe rotarse y retirarse de variables persistentes; futuras evidencias deben mostrar únicamente nombre, tipo y archivo/línea, nunca el valor.

## Plan de despliegue y rollback

1. **Preparar:** aprobar contrato DB-D06 y matriz de consumidores; congelar los SHAs; asegurar backup restaurable y snapshot de `Sesion` (id, inicio, fin, procedencia, conteo).
2. **Ensayar:** restaurar una copia no productiva; aplicar objetos D06 en el orden del runner; ejecutar `test_summary.ps1`, el test focal D06 y smoke read-only; comprobar que históricos no cambian.
3. **Canario:** desplegar backend con `utc.v2.enabled=false`; la columna y SP quedan disponibles pero ninguna ruta v2 se activa. Verificar v1, JWT y ownership.
4. **Habilitar controladamente:** activar v2 solo en un ambiente/canario con DB compatible, ejecutar POST→GET→PATCH→GET con JWT DOCENTE titular y casos 401/403/no-owner; observar errores, auditoría y conteos.
5. **Avanzar:** promover por ambiente solo con evidencia de tests, métricas y aprobación de backend, DB y frontend. No mezclar el retiro de v1 con el primer despliegue v2.
6. **Rollback:** desactivar la bandera v2 y revertir el artefacto backend; mantener la columna/objetos aditivos y la compatibilidad v1. Restaurar la copia solo si el owner DB lo ordena tras verificar impacto; nunca ejecutar `DROP`, rellenar históricos ni reinterpretar zonas como acción automática.

## Retiro gradual de endpoints v1

El backend no elimina v1 en esta fase. La siguiente matriz se basa en búsquedas de solo lectura del frontend `AsistenciasUCO-Frontend` y debe confirmarse con sus responsables antes de cambiar rutas:

| Superficie v1 observada | Consumidor observado | Migración propuesta |
|---|---|---|
| `GET /api/v1/sesiones/grupo/{grupoId}` | `src/app/core/services/session.service.ts`; teacher grupos, control de asistencia y dean faculty llaman `getSessionsByGroup` | Cambiar el servicio a v2 con DTO UTC; validar recarga de lista y permisos por grupo; mantener fallback/telemetría durante la convivencia. |
| `POST /api/v1/sesiones` y `PUT/PATCH /api/v1/sesiones/{id}` | `session.service.ts`; `teacher-grupos.component.ts` y `attendance-control.component.ts` usan `createSession`/`updateSession` | Migrar primero creación/edición a v2 con offset explícito; probar DST, validación de rango, 401/403 y respuesta void/mensaje según contrato congelado. |
| `GET /api/v1/estudiante/materias/{materiaId}/sesiones` | `core/services/attendance-claim.service.ts`; lo consumen `student-courses`, `teacher-schedule`, `teacher-grupos`, `attendance-control` y vistas de overview/claims | Separar la consulta de estudiante de la sesión docente; publicar contrato v2 equivalente solo después de comprobar campos y ownership. No asumir que el listado docente reemplaza esta consulta. |
| `GET /api/v1/grupos/{grupoId}/reportes/asistencia-excel` | `core/services/group.service.ts`; `attendance-control.component.ts` descarga la planilla | Mantener endpoint de reporte hasta validar que el reporte usa las mismas sesiones/instantes y que el Excel conserva la semántica; migrar consumidor y contrato en una fase independiente. |

Secuencia propuesta: (a) instrumentar consumo por ruta y versión; (b) migrar lectura docente; (c) migrar escritura docente; (d) migrar consultas de estudiante; (e) validar Excel; (f) declarar cero consumidores v1 durante una ventana acordada; (g) anunciar deprecación con fecha; (h) retirar v1 en un PR posterior con rollback y compatibilidad documentados. Mientras tanto, v1 permanece operativo y sus fechas no se reinterpretan.

## Gate de continuación

No activar v2 en producción ni desplegar DDL en la base principal. Antes de continuar con frontend deben resolverse los fallos de integración Java, ejecutarse HTTP v2 real con JWT/ownership, acordarse el principal SQL runtime y aprobarse el plan de staging/canario. Los PR backend y DB pueden abrirse para revisión como cambios separados, pero deben mostrar este estado condicionado y no presentarse como listos para merge.
