# TEST PLAN — P0-C

## RED snapshot

- Backend base: `bf33c79` con `mvnw.cmd -B -ntp verify`; compilación Java 25,
  sin resumen Surefire y `dumpstream` de attach JVM.
- Frontend base: `a33876b`/`fac2f640` con `npm run verify`; bundle generado,
  ChromeHeadlessCI sin captura después de 2 timeouts de 60 s.
- No se modifican los tests funcionales para producir un resultado verde.

## Matriz conductual

| ID | Requisito | Observable | Implementación incorrecta que detecta | Nivel |
|---|---|---|---|---|
| C-SUREFIRE-01 | ejecutar la suite completa en Java 25 | exit code, contadores Surefire y reportes nuevos | omitir tests o dejar fork colgado | build/test |
| C-SUREFIRE-02 | conservar calidad | JaCoCo LINE >=80%, BRANCH >=70% y ArchUnit ejecutado | rebajar umbral o saltar tests | gate |
| C-KARMA-01 | ejecutar frontend completo | Chrome captura y reporta todos los specs | launcher que solo compila | frontend |
| C-KARMA-02 | conservar cobertura | coverage y realtime gate reales | excluir specs o rutas | frontend |
| C-RUNTIME-01 | arranque local seguro | logs centralizados, estado y exit code | escribir logs en raíz o perder errores | smoke |
| C-REPO-01 | preservar estado | refs/SHAs/archivos únicos documentados | borrar worktree sin respaldo | operativo |

## Validación

- `java -version`, `javac -version`, `mvnw.cmd -version` y variables de
  ejecución sin imprimir secretos.
- `mvnw.cmd -B -ntp verify` completo.
- `npm run build -- --configuration=development`.
- `npm run build -- --configuration=production`.
- `npm run test:ci -- --no-progress` y `npm run coverage:realtime:check`.
- `npm run verify` completo.
- Smoke Spring/Angular y E2E solo si SQL Server, Keycloak y Docker están
  disponibles y controlados; si no, `VALIDATION_BLOCKED_BY_ENVIRONMENT`.
