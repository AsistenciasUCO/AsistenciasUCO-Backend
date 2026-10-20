# MAINT-003L — Validación E2E local de docentes de paginación

## Dictamen

`VALIDATION_BLOCKED_BY_ENVIRONMENT` para el gate visual Angular; el alcance backend + SQL Server + Keycloak + HTTP real está PASS. No se declara cierre funcional completo mientras no exista el frontend AsistenciasUCO real.

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado | Evidencia sanitizada |
|---|---|---:|---|---|
| 2026-10-11 / backend checkout | Parser PowerShell de `prepare-pagination-fixture.ps1`, `validate-pagination-e2e.ps1`, `seed-pagination-e2e-users.ps1`, `get-test-token.ps1`, `diagnose-idusuario.ps1` | 0 | PASS | Todos `PARSE_OK` |
| 2026-10-11 / SQL Server local | `scripts/e2e/prepare-pagination-fixture.ps1 -Action Prepare` | 0 | PASS | Backup `C:\Users\josev\AsistenciasUCO\db-backups\maint003l_pagination_e2e_pre_20261011_183021.bak` verificado; 2 horarios creados |
| 2026-10-11 / SQL Server local | `scripts/e2e/prepare-pagination-fixture.ps1 -Action Prepare` segunda corrida | 0 | PASS | `createdOrDeleted=0`; sin cambio del snapshot |
| 2026-10-11 / SQL Server local | `scripts/e2e/prepare-pagination-fixture.ps1 -Action Cleanup` | 0 | PASS | 2 horarios reservados eliminados; snapshot sin cambios |
| 2026-10-11 / SQL Server local | `scripts/e2e/prepare-pagination-fixture.ps1 -Action Prepare` final | 0 | PASS | 2 horarios repuestos; estado final preparado |
| 2026-10-11 / SQL Server local | `scripts/e2e/prepare-pagination-fixture.ps1 -Action Inspect` final | 0 | PASS | 300/1000/5000 con 1 horario cada uno; snapshot protegido idéntico |
| 2026-10-11 / Keycloak local | `seed-pagination-e2e-users.ps1` | 0 | PASS | 3 usuarios creados/verificados; solo client role `DOCENTE`; no bootstrap |
| 2026-10-11 / Keycloak local | `get-test-token.ps1 -Username docente.paginacion300/1000/5000` | 0 | PASS | issuer exacto, `aud=asistencias-api`, claim `idUsuario`, role `DOCENTE`; JWT no impreso |
| 2026-10-11 / backend 8082 | `validate-pagination-e2e.ps1` | 0 | PASS | Horarios 200/1; estudiantes 200/300, 200/1000, 200/5000; V2: 4 `UTC_V2` en cada grupo y 1 sesión histórica indeterminada adicional en 300; ownership 403; directorio coordinador 200/20/5003; docente 403 |
| 2026-10-11 / backend | `mvnw.cmd spring-boot:run -DskipTests`, `SERVER_PORT=8082`, `APP_SESIONES_V2_ENABLED=true` | 0 | PASS | Arranque verificó UTC-D06-POST-FREEZE; proceso detenido al terminar |

## Checks

| Nivel | Check | Resultado | Evidencia/motivo |
|---|---|---|---|
| Alcance | Plan/criterios | PASS | `PLAN.md` y DoR delimitan scripts DEV y datos sintéticos |
| Arquitectura | Código productivo/ArchUnit | NO APLICA | No se modificó `src/main`, Domain/Application ni wiring |
| Contratos | HTTP/DB/security | PASS | Solo consumo de contratos vigentes; no cambió OpenAPI/RBAC |
| Tests | Scripts/negativos | PASS | Parseo, idempotencia, cleanup, ownership cruzado y directorio DOCENTE 403 |
| Coverage | JaCoCo | NO APLICA | No se agregó código Java productivo |
| Integración | SQL Server, Keycloak, backend | PASS | Proveedores reales locales; sin contenedores nuevos |
| Build | Maven | INTERRUPTED | `spring-boot:run -DskipTests` arrancó con Java 25; `mvnw.cmd -B -ntp verify` llegó a `CrearDecanoUseCaseImplTest`, quedó sin salida por más de 90 s y se interrumpió con exit 1. No se declara PASS global. |
| Seguridad | Secretos/RBAC | PASS | No se imprimieron passwords/JWT; tres cuentas solo `DOCENTE`; `docente.prueba` intacto |
| Documentos | Evidencia | PASS | Este work item contiene trazabilidad y limitaciones |
| Angular | Pantallas reales | NOT_RUN | El checkout disponible `C:\Users\josev\Front-eatup\eatup-frontend` es EatUp y no contiene rutas/servicios de docente AsistenciasUCO |

## Integridad de RED

NO APLICA: no se cambiaron tests RED ni código productivo.

## Evidencia de datos

- Estado protegido antes/después: `5000 estudiantes`, `6300 matrículas`, `13 sesiones`, `6350 asistencias`.
- Hash de estudiantes sintéticos: `DC238D0841BCD674C7804914024D0BCA45DC33796BA50D1DC61EF250AFEAF0CA` antes y después.
- Hash de matrículas de los tres grupos: `85A924BCC8BED57B702B16107021AAD5493C1D59913C2D05A8A9D4CA2D679D88` antes y después.
- Horario reutilizado de `FIX-PAG-300`: `351B49D9-D652-4897-AF84-4EAE7CE60F33`, lunes 05:00–05:30; no se reasignó.
- Horarios creados: `DE00000B-0000-4000-8000-000000000002` y `DE00000B-0000-4000-8000-000000000003`, lunes 08:00–10:00.
- Backup: `COPY_ONLY + CHECKSUM` y `RESTORE VERIFYONLY` PASS.

## Bloqueantes y limitaciones

El gate Angular requiere el repositorio/servidor frontend de AsistenciasUCO. El puerto 8080 estaba ocupado; la validación backend se ejecutó en 8082 y el proceso temporal fue detenido. No se hizo merge, despliegue ni reconstrucción de DB.

## Evidencias remotas/manuales pendientes

No aplica CI remoto. La prueba visual Angular queda pendiente de un frontend compatible.
