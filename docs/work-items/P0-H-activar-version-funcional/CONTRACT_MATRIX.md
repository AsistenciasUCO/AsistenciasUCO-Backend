# CONTRACT_MATRIX — P0-H

| Componente | Línea activa | Candidato integrado/local | Estado contractual |
|---|---|---|---|
| Backend | `6893f4d4` | `f45a52137674415871882043771676b6592fb72c` | Activación local; B1 requiere validación sobre la línea actual. |
| Frontend | `b4368d1e` + tres cambios locales | `95a202a6b767e537f519012a847cffe3a7c100ab` | Candidato incluye `fac2f640`; cambios locales se preservan y comparan. |
| DB | `32b70ebe` / `origin/develop` | `b698c76112490b91298d16ccd1fb6a793648adec` | D1/R2 experimental; owner DB pendiente. |
| Infra | stack local vigente | scripts actuales | Sin cambio contractual; `infra/files/.env` nunca se copia a evidencia. |

## DB D1/R2 observado

| Operación | Tipo | Resultado candidateado | Consumidor backend | Dictamen |
|---|---|---:|---|---|
| `usp_registrar_archivo_soporte` | W1 mutation | 4 columnas | metadata/archivo | compatible con baseline de mutación; aprobación pendiente |
| `usp_radicar_solicitud_revision_asistencia_v2` | W2 mutation | 4 columnas | reclamo/soporte | compatible con baseline de mutación; aprobación pendiente |
| `usp_consultar_archivo_soporte_autorizado` | R1 read query | 10 columnas | descarga autorizada | requiere confirmación como query pública |
| `usp_consultar_solicitudes_revision_docente` | R2 read query | 25 columnas | listado docente paginado | requiere confirmación como query pública |

No se fuerza R1/R2 a cuatro columnas. Esta matriz no sustituye el contrato aprobado ni autoriza producción.
