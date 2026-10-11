---
status: FIX_PROPOSED_TESTS_NOT_EXECUTED
date: 2026-10-10
base: jose-valencia/maint-003h-provider-contract-impl@4a52b36
scope: session-read-compatibility-and-release-review
---

# MAINT-003I: UTC v2 cierre rapido y riesgos de proveedores (revisión independiente)

## Evidencia consultada
En origin existen backend `jose-valencia/maint-003h-provider-contract-impl@4a52b36` y DB `feat/cc-003g-01-public-user-plan-providers@3f6afec`, cada uno adelantado respecto de su rama base. Se revisaron sus SP públicos, permissions, JPA, casos de uso y evidencias documentales. **No se ejecutó ninguna prueba Maven, SQL Server o Keycloak en esta auditoría:** 1499/1499 Surefire, 196/196 Failsafe, 219 SQL y HTTP 20/20, 21/21 son los resultados acreditados por los autores en VALIDATION, no nuevos resultados ejecutados aquí.

## P1: SQL datetime2 sin offset se desplazaba +5h en v1
Evidencia de código: `UvSesionEntity` mapeaba `uv_sesion.fechaHoraInicio/Fin` como `java.util.Date`; `CoreViewJpaProjectionMapper.toSesion` llamaba `toUtcLocalDateTime(Date)`, que calcula `value.toInstant().atZone(UTC)`. En la JVM Bogota, ya fue observado GET v1 = 19:00 para `datetime2 14:00`. UTC v2 usa `LocalDateTimeJdbcType` sin ese problema.

Esta rama cambia **solo el mapeo de lectura v1**: `UvSesionEntity` recibe `LocalDateTime` + `@JdbcType(LocalDateTimeJdbcType.class)`, mapper entrega el literal intacto y el oráculo JDBC de `CoreViewQueriesJpaParityIT` usa `ResultSet.getObject(column,LocalDateTime.class)` sobre el mismo SQL DATETIME2, no Timestamp->Instant con timezone JVM. Nuevo `SesionLocalClockProjectionTest` exige conservar nanosegundos y NULL. La modificación **no** afirma que filas históricas con marca NULL sean UTC: solo preserva el valor almacenado y el wire v1 ingenuo ya existente. No cambia POST/PATCH v1 ni v2.

**Gates para Codex antes de cherry-pick/merge:** Java25 `clean verify`, `-Pintegration` en SQL Server 2022, paridad de consultas y reporting en JVM `-Duser.timezone=UTC`, `America/Bogota`, `Europe/Berlin`; HTTP real GET v1 debe devolver exactamente datetime2 almacenado sin sumar 5h; GET v2 Z debe ser invariante. Confirmar que los consumidores académicos/Excel no dependen de `toUtcLocalDateTime(Date)` por otros caminos. No modificar pruebas congeladas de terceros sin especificar diferencia de contrato.

## P1: cuenta institucional — reintento DB no prueba identidad del solicitante
`POST /api/v1/usuarios` exige sólo autenticación HTTP, y `usp_sincronizar_usuario` acepta como idempotente una coincidencia exacta de documento/correo/nombres sin comparar titularidad de la identidad autenticada. El use case `ProvisionarUsuarioUseCaseImpl` toma el ID DB devuelto y solicita a `KeycloakIdentityProviderAdapter` resolver o crear la cuenta/rol. El adaptador verifica vínculo estable en Keycloak, pero el SP por sí solo no verifica que quien envía los datos sea el titular. Esto es un **riesgo de vinculación/reprovisionamiento** (no se afirma explotación): un solicitante autenticado podría repetir datos personales de otro usuario. Antes del despliegue: restringir POST a actor autorizado, o verificar ownership/identidad e impedir que un reintento relacione otro sujeto con el usuario recuperado; ejecutar pruebas negativas con dos actores y fallo intermedio DB↔IdP. No publicar ni usar cuentas reales para estas pruebas. El estado final de esta corrección pertenece a un work item SECURITY separado; no degradar seguridad por acelerar frontend.

## P1/P2: unicidad de usuario sin restricción global
`usp_sincronizar_usuario` serializa consultas mediante COUNT_BIG + UPDLOCK,HOLDLOCK y bloqueos hasta commit, pero **otros SP de alta** (`usp_crear_coordinador`, `usp_crear_decano`, matricula) no necesariamente emplean el mismo protocolo. Falta garantía de unicidad de documento/correo transversal a escritores. Recomendación: estudiar índices únicos sobre expresiones normalizadas o columnas computadas persistidas para documento y correo, tras inventario/limpieza de duplicados en DEV; complementar con test de concurrencia cruzada de SP. Sin prueba ni datos auditados no aplicar índice en DB actual.

## P2: privilegios y superficie de planes
`usp_registrar_o_actualizar_plan_estudio` ahora verifica perfil coordinador y titularidad por ID de usuario. `rol_asistencias_runtime_cc003g01.sql` concede EXEC solo en dos SP públicos y DENY DML en Usuario/PlanEstudio; verificar que el rol final de D06 no conserve GRANT EXECUTE amplio de esquema, pues grants anteriores pueden sobrevivir si no se revocan explícitamente. Certificar conexión no privilegiada real y las demás rutas backend. El programa puede estar inactivo; existe validación de existencia pero no de actividad (decisión funcional pendiente); probar antes de dejar modificar planes de programas inactivos.

## Datos 100% desarrollo: simplificación acordada
El usuario autoriza **borrar datos temporales no útiles** de ambientes de desarrollo. Es seguro proponer limpieza de sesiones legacy y dependientes en un **work item DEV-RESET**, no migración/interpretación histórica fila a fila, siempre que no sea una DB de staging compartida ni contenga datos de terceros. NO borrar en esta rama. Preflight de servidor/nombre de DB/entorno, cero usuarios reales, dependencias y claves foráneas, snapshot/backup local verificable; script parametrizado para DEV con allowlist y `dry-run` (conteos e IDs), operación transaccional + oráculos post-borrado, nuevos fixtures UTC y JWT. Si hay auditoría append-only, no destruir ni modificar registros de auditoría; recrear DB de desarrollo totalmente desechable desde scripts versionados puede ser más simple que DELETE selectivo. Ninguna ejecución por defecto ni en producción.

## Ruta rápida para completar módulo y pasar a frontend / LB-004 Blob
1. Codex revisa MAINT-003I y valida mapeo v1 contra SQL/JWT con 3 timezones. Si pasa, cherry-pick controlado a MAINT-003H; si falla, conservar rama aislada y devolver causa.
2. Crear PR Draft backend MAINT-003F y DB D06 y PR incremental MAINT-003H / CC-003G-01 según sus bases. Confirmar que cada CI de Sonar/CodeQL/Trivy realmente corra sobre SHA final; corregir fails reales.
3. Con DB y backend en DEV combinados y login SQL mínimo, smoke: creación/listado/edición v2, histórico indeterminado o reset DEV, JWT, DST, generación y asistencia; v1 estudiantes/Excel (wire intacto). **No eliminar rutas v1** mientras existan consumidores.
4. Frontend Angular en rama aislada: cliente v2, zona IANA al cargar, selector si existe, offset real por fecha, DST gap/overlap, fecha cruzando medianoche, recarga y lista mixta. Activar sólo en DEV tras humo backend.
5. LB-004 Blob Storage sólo después del smoke funcional de sesiones y permisos + plan de deployment; no mezclar cambio de almacenamiento con estas PR.

## Certificación
`UTC_V2_BACKEND_SQL_LOCAL_PASS=REPORTED`; `CI_REMOTE=NOT_VERIFIED`; `FRONTEND=NOT_STARTED`; `DEV_RESET=NOT_EXECUTED`; `OWNER_APPROVAL=OUTSTANDING`; `MAINT003I_FIX=PROPOSED_UNTESTED`.
