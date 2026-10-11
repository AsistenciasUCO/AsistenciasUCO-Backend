# MAINT-003K — Evidencia de validación

Fecha de ejecución: 2026-10-11. Host Windows, Java 25 (`C:\Program Files\Java\jdk-25`), Maven Wrapper 3.9.15.
Rama: `jose-valencia/maint-003k-backend-only-release-gate`.
Base reconciliada: `origin/develop` en `108937cdb2cd0ca1a35b7349fbd0696721b39499`.
Merge normal: `0114939d877f8a6a91b7118af98bf4f7c29c1796`.
Código certificado: `8ac519ae` (`test(v2): restore Sonar new-code coverage gate`).
Arnés E2E certificado: `5ab8e8f` (`test(e2e): report connection failures safely`).

## 1. Estados independientes

| Estado | Resultado | Evidencia |
|---|---|---|
| `BACKEND_LOCAL_GREEN` | **PASS** | Maven, ArchUnit, OpenAPI, JaCoCo, SQL Server y E2E real §§2–5 sobre `8ac519ae`/`5ab8e8f` |
| `DEV_DB_CONSOLIDATED` | **PASS local** | `sql_server_asistencias` / `gestionasistenciadb`; detalle en `DB_DEV_ADDENDUM.md` y work item SQL |
| `DB_PR_READY` | **Certificación técnica observada; merge pendiente** | Contrato PR #36 comprobado en la DB local; no se mezcló ni se usó como autoridad de merge de PR #22 |
| `BACKEND_CI_GREEN` | **PENDING** | Requiere comprobar los checks de GitHub sobre el SHA final publicado |

No se creó ningún contenedor ni base temporal durante esta certificación. Se utilizó exclusivamente el contenedor existente `sql_server_asistencias` y la base `gestionasistenciadb`. No se hizo merge a `develop`, no se borraron ramas, no se modificó frontend ni se desplegó a producción.

## 2. Reconciliación de `develop`

`git fetch origin --prune` se ejecutó antes del merge. El worktree estaba limpio y la rama apuntaba inicialmente a `6814e87`; `origin/develop` apuntaba a `108937c`.
Se integró mediante `git merge origin/develop`, sin `reset`, `ours`, `theirs` ni force-push.

| Conflicto / zona | Resolución aplicada |
|---|---|
| `openapi-golden-path.yaml` | Se conservaron las 10 operaciones v1, incluida la consulta paginada de estudiantes, y las 4 operaciones v2 UTC. |
| `openapi-golden-path.sha256` | Se regeneró sobre el YAML reconciliado: `3107f69049d50ad6de7b0b0ce6c5ab093769f2c8c4b2a11966f7fb69fed08a18`. |
| `OpenApiGoldenPathConformanceTest.java` | Se combinaron las aserciones v2 y las de estudiantes paginados; no se eliminaron pruebas. |
| `CoreViewQueriesJpaParityIT.java` | Se mantuvo el recorrido de todas las páginas y la igualdad JDBC/JPA, incluyendo el volumen de 5.000 estudiantes. |
| `HttpUtcInstantCodec.java` | Se conservó la implementación estricta de `develop`: offset válido, precisión `datetime2(7)`, rangos y rechazo de `-00:00`. |
| `SesionUtcActivationGuardTest.java` | Se fusionó automáticamente sin perder la guardia del contrato UTC-D06. |
| `VALIDATION.md` histórico | Se preservó la evidencia anterior y se añadió esta certificación final; el detalle se separa por SHA. |
| `HttpUtcInstantCodecStrictProfileRedTest` | El test RED histórico esperaba aceptar `-00:00`, contrario al contrato actual; se conservó el test y se actualizó su expectativa a rechazo explícito. No se desactivó ni eliminó. |

El merge `0114939` mantiene las correcciones de paginación del PR #18, caché del #19, correcciones Sonar del #20, documentación del #21 y el trabajo UTC v2 existente.

## 3. Maven, arquitectura y contratos

Comandos exactos ejecutados con Java 25 y `-B -ntp`:

```text
.\mvnw.cmd -B -ntp clean verify
.\mvnw.cmd -B -ntp -Pintegration clean verify
```

El primer intento de `clean verify` sin elevación quedó en **FAIL de entorno** por permisos de loopback de Windows/JDK25 para los servidores fake de Keycloak/ClamAV; no se contó como PASS. La misma orden se repitió con el entorno de ejecución autorizado y terminó en `BUILD SUCCESS`.

| Fase | Resultado real |
|---|---|
| Surefire | **1.534** tests, 0 failures, 0 errors, 0 skipped |
| Failsafe `-Pintegration` | **198** ITs, 0 failures, 0 errors, 0 skipped |
| JaCoCo final | **92,78 % líneas**, **80,96 % ramas**; gates 80/70 cumplidos |
| ArchUnit | `CleanArchitectureRulesTest` 20/20; `JpaRepositoryArchitectureRulesTest` 3/3; suite arquitectónica verde |
| OpenAPI | Golden path 9/9; directorio de estudiantes 6/6; validación 2/2; contrato sesiones v2 4/4; hash consistente |
| SQL/SP/JPA | Contratos públicos, procedimientos, paridad JDBC/JPA, paginación y consultas académicas verdes |

El cambio adicional de wire v1 queda en Infrastructure: `JacksonTemporalConfiguration` selecciona el conversor MVC Jackson 3 y serializa `LocalDateTime` con ISO local, preservando el contrato legacy sin cambiar Domain/Application ni el contrato SQL.

La recuperación de Sonar se certificó primero localmente en `8ac519ae`: los dos dominios v2 se expresaron como records conservando sus getters públicos, se cubrieron las ramas/DTOs/repositorios/interactors/controladores v2 sin relajar los umbrales y se documentó la decisión de seguridad de CSRF stateless con `@SuppressWarnings("java:S4502")`. El arnés quedó endurecido en `5ab8e8f` para reportar la causa real cuando una conexión no alcanza a producir respuesta HTTP.

## 4. Regresión UTC, seguridad y HTTP real

El arnés `scripts/e2e/utc-real-jwt-e2e.ps1` ejecutó 28 comprobaciones por zona, contra el backend empaquetado con `app.sesiones.v2.enabled=true` y el JWT real del Keycloak local:

| JVM | Resultado |
|---|---|
| `UTC` | `E2E_SUMMARY tz=UTC total=28 pass=28 fail=0` |
| `America/Bogota` | `E2E_SUMMARY tz=America/Bogota total=28 pass=28 fail=0` |
| `Europe/Berlin` | `E2E_SUMMARY tz=Europe/Berlin total=28 pass=28 fail=0` |

La matriz cubrió sesiones v1 y v2, POST/GET/PATCH, `datetime2(7)` con precisión de 100 ns, equivalencia de instantes Bogotá/Berlín, flag ON/OFF, 401/403, ownership, actor únicamente desde JWT, entradas inválidas, compatibilidad v1, PUT 405, reportes y consultas académicas.

La respuesta v1 se comprobó directamente como ISO (`2026-08-17T13:00:00`); el harness usa `ConvertFrom-Json -DateKind String` para no convertir la fecha a `DateTime` local y reimprimirla con la cultura del host. La repetición final esperó el puerto HTTP en estado `LISTEN` antes de cada zona para evitar una carrera de arranque.

La consulta final de residuos ejecutó sobre SQL Server:

```text
SELECT 'E2E-sesiones', COUNT(*) FROM dbo.Sesion WHERE nombre LIKE 'E2E-%'
E2E-sesiones,0
```

No se dejaron sesiones E2E. Los ITs de archivos usan el bucket de desarrollo existente como parte de su comportamiento previo; no se alteró configuración de MinIO.

## 5. Contrato DB / PR #36

La certificación técnica se realizó contra la base local que expone el contrato UTC-D06 y CC-003G-01, incluyendo procedimientos públicos, permisos runtime, vistas y paridad JPA/JDBC. Esa evidencia demuestra compatibilidad técnica del backend con la DB disponible.

El PR #36 de la DB mantiene estado independiente: su merge a `develop` sigue pendiente y no se declara como realizado por esta tarea. El contrato SQL pertenece al owner de DB; PR #22 no modifica esquema, procedimientos ni permisos.

## 6. Correcciones funcionales y limitaciones

- Se resolvió el problema de serialización MVC de `LocalDateTime` que hacía que el arnés PowerShell mostrara fechas localizadas, manteniendo ISO en el wire real.
- No se amplió funcionalidad exclusiva de Keycloak; se conservaron JWT, roles institucionales, ownership y RBAC actuales.
- No hay secretos en código, documentación, logs ni evidencia.
- `AzureCloudIntegrationIT` permanece `NOT_RUN` porque requiere Azure real; no es una regresión del alcance local.
- En el commit documental previo `c597a237`, SonarCloud reportó objetivamente `new_coverage=52,3 %` y `new_security_rating=ERROR` por `S4502`; `8ac519ae` añadió la cobertura y la decisión auditada sin bajar gates. SonarCloud, Backend Quality Gate, CodeQL, Dependency Review, Trivy y el estado de ArchUnit en GitHub quedan **PENDING** hasta comprobar el SHA final publicado. Las pruebas locales no sustituyen esos checks.
- El cierre final de MAINT-003K queda condicionado a que GitHub reporte verde sobre el último commit publicado; no se declara `BACKEND_CI_GREEN` antes de esa evidencia.
