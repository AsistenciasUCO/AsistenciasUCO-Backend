# Trivy findings — Backend PR #1

Fuente: artifacts SARIF del workflow `Backend Security`, run `39178504372`,
head `5249051e733d0eae2f5b95c15c2ec1a6bcd43e5f`. La acción tiene
`exit-code: 0`, por lo que esta tabla es un inventario y no una certificación de
ausencia de vulnerabilidades.

| Componente/artefacto | Severidad | CVE/regla | Dependencia/versión | Fix disponible | Estado P0-D |
|---|---:|---|---|---|---|
| Imagen `asistencias-uco-security-scan`, paquete `libexpat` | HIGH | CVE-2026-77214 | `libexpat 2.8.5-r0` | `2.9.0-r0` | `OPEN / inventario` |
| Imagen `asistencias-uco-security-scan`, paquete `libexpat` | MEDIUM | CVE-2026-102633 | `libexpat 2.8.5-r0` | `2.9.0-r0` | `OPEN / inventario` |
| `app/app.jar/BOOT-INF/lib/mssql-jdbc-13.2.1.jre11.jar` | HIGH | CVE-2025-59250 | `com.microsoft.sqlserver:mssql-jdbc 13.2.1` | `13.2.1.jre11` (también ramas soportadas indicadas por Trivy) | `OPEN / inventario` |
| `Dockerfile` | LOW | DS-0026 | Dockerfile sin `HEALTHCHECK` | Añadir HEALTHCHECK si el owner lo aprueba | `OPEN / misconfiguration` |
| `infra/files/minio/Dockerfile` | LOW | DS-0026 | Dockerfile sin `HEALTHCHECK` | Añadir HEALTHCHECK si el owner lo aprueba | `OPEN / misconfiguration` |

No se asigna un SLA ni se eleva `exit-code` hasta que el owner apruebe la
clasificación de baseline, el tratamiento del JDBC y la política para Dockerfile.
El detalle completo permanece en los artifacts SARIF del run; no se copian logs
con secretos al repositorio.

## Otros repositorios oficiales

- Frontend: workflow preparado para inventario Trivy de filesystem e imagen;
  ejecución remota P0-D `NOT_RUN`.
- DB: no se ejecutó Trivy en esta fase; no se afirma ausencia de findings.
- Infra: workflow preparado para inventario Trivy de configuración/filesystem;
  ejecución remota P0-D `NOT_RUN`.
