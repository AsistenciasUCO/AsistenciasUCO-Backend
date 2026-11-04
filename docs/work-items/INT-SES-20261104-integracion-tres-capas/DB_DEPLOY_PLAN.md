---
status: active
type: evidence
scope: integration
owner: backend-team
last-reviewed: 2026-11-04
---

# Alineación de `sql_server_asistencias` con develop DB (2026-11-04)

## Diagnóstico (solo lectura, catálogo `sys.sql_modules`)

- Instancia: contenedor `sql_server_asistencias` (hostname `2f41e9118416`), base `gestionasistenciadb`, SQL Server 16.0.4265.3.
- Estado previo: módulos idénticos a DB `d1e0af85` (integración del 01/11). Comparación de 247 módulos contra
  `develop` `11d7d50` ignorando comentarios: 242 iguales, 5 distintos, 11 ausentes. Tablas sin cambios desde `d1e0af85`.
- 18 módulos difieren solo en comentarios (REC4 `e69deb1`, «solo comentarios»): fuera del alcance mínimo.

| PR DB | Fusionado por | Revisión formal owner DB | Paquete |
|---|---|---|---|
| #76 F3 docente crea grupo | johnjduque | merge del owner | A |
| #77/#78 F2 precarga + Sonar | johnjduque | merge del owner | A |
| #79/#80 HU112–HU157 | johnjduque | merge del owner | A |
| #81 SES-01 ventana estricta | alejovh12 | **sin review** | B |
| #82 SES-04 reclamo → EX | alejovh12 | **sin review** | B |
| #83 SES-02 cobertura F2 | alejovh12 | **sin review** | B |

`DB_DEVELOP_FREEZE_ADDENDUM.md` (§SES-01) exige la revisión formal del owner DB (John Duque) para aplicar en bases
compartidas el cambio de SP congelados. La autorización del responsable del proyecto **no** sustituye esa revisión.

## Paquete A — APLICADO 2026-11-04 02:21 (-05:00)

Fuente: `git show d82ad2f:<ruta>` (no el working tree). 16 archivos, una transacción `XACT_ABORT`, `ROLLBACK` ante error.

- 10 SP nuevos: `usp_validar_gestion_grupo_coordinador_o_titular_interno`, `usp_validar_consulta_paginada_interno`,
  `usp_precargar_sesiones_grupo_interno`, `usp_precargar_sesiones_grupo`, `usp_consultar_oferta_creacion_grupo_docente`,
  `usp_buscar_asignaturas_paginado`, `usp_buscar_estudiantes_institucion_paginado`, `usp_consultar_docentes_facultad_paginado`,
  `usp_consultar_historial_estudiante`, `usp_consultar_programas_creditos_facultad_paginado`.
- 2 SP modificados: `usp_crear_grupo`, `usp_guardar_horarios_grupo`.
- Grants: `rol_asistencias_runtime_f2_precarga_sesiones`, `_f3_docente_crea_grupo`, `_hu112_hu157` (7 GRANT, DENY al interno).
- Seed `04_mensaje.sql` (MERGE): agrega `HOR_005`; el resto de filas ya coincidía con el repo.
- Backup previo: `BACKUP ... COPY_ONLY, CHECKSUM` + `RESTORE VERIFYONLY`, copia en
  `%USERPROFILE%\AsistenciasUCO-backups\20261104\gestionasistenciadb_pre_alignA_20261104_022158.bak`.
- Autorización: responsable del proyecto (2026-11-04) sobre PRs ya fusionados por el owner DB.
- Verificación: 253/257 módulos iguales a develop; restan exactamente los 5 objetos del paquete B; grants y mensajes OK.

## Paquete B — PENDIENTE DE APROBACIÓN DEL OWNER DB

Fuente `11d7d50`: `usp_validar_sesion_ventana_registro_interno` (nuevo), `usp_precargar_sesiones_grupo_interno`
(guardia `@fechaDesde <= @fechaFin`), `usp_registrar_asistencias_sesion`, `usp_registrar_asistencia_estudiante_autonomo`
(congelados, SES-01), `usp_resolver_solicitud_revision_asistencia` (SES-04) y `04_mensaje.sql` (SES_007–009, REV_001–005).

```powershell
.\scripts\align-live-sql-develop.ps1 -Package B -Mode Preflight
.\scripts\align-live-sql-develop.ps1 -Package B -Mode Apply -AuthorizedBy '<responsable>' -ApprovedBy '<John Duque: enlace a la aprobación>' -BackupDir "$env:USERPROFILE\AsistenciasUCO-backups\<fecha>"
.\scripts\align-live-sql-develop.ps1 -Package B -Mode Verify
```

El script rechaza `-Package B -Mode Apply` sin `-ApprovedBy`.

## Rollback

1. Error durante Apply: `ROLLBACK` automático de la transacción; nada queda aplicado.
2. Tras COMMIT: detener el backend y `RESTORE DATABASE gestionasistenciadb FROM DISK = N'/var/opt/mssql/data/<bak>' WITH REPLACE`
   (el .bak sigue dentro del contenedor y tiene copia en `AsistenciasUCO-backups`). Nunca `down -v` ni recrear el contenedor.

## Consecuencia mientras B esté pendiente

- SES-01: el backend valida la ventana con `Clock.systemUTC()` y responde 409 antes de llamar a la DB; falta la segunda
  barrera transaccional en SQL (`SES_007/008/009`). La API directa queda protegida por el backend, no por la DB.
- SES-04: la DB viva aún aplica la resolución anterior (no fija `EX`/`asistio = 0`, sin `REV_00x`): el E2E de aprobación
  no puede validarse contra la instancia.
- SES-02: la precarga F2 funciona; falta la guardia de rango vacío de `c98ab41`.
