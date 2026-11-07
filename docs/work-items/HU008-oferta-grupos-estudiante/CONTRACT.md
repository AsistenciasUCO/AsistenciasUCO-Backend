---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CONTRACT — HU008: oferta de grupos del estudiante

> **CULM-001 A.1 (2026-11-07):** HU053-HU055 se descartan. La respuesta pierde `idSolicitudAbierta` y `estadoSolicitud` (19 campos) y el resultset de `usp_consultar_oferta_grupos_estudiante` pasa de 21 a 19 columnas (`codigoComponente` en 17, `totalRegistros` en 18; guarda `COLUMNAS_OFERTA = 19`). El boton "Solicitar ingreso" y `POST /estudiante/solicitudes-matricula` se retiran.

## 1. HTTP

`GET /api/v1/estudiante/oferta-academica`

- **Rol:** `ESTUDIANTE` (`SecurityConfig` `/api/v1/estudiante/**`). Tiene una fila nueva en el catálogo de acceso.
- **Query:**
  - `busqueda`: opcional, hasta 50 caracteres. Busca en el nombre o código de la asignatura o en el nombre del grupo.
  - `pagina`: por defecto 1; mínimo 1.
  - `tamano`: por defecto 10; entre 1 y 100.
- **200:** `ApiListResponse<GrupoOfertaDTO>`.

```json
{
  "exitoso": true,
  "total": 9,
  "datos": [{
    "idGrupo": "uuid", "codigoGrupo": 7, "nombreGrupo": "Grupo 7",
    "idAsignatura": "uuid", "codigoAsignatura": "MAT1", "nombreAsignatura": "Algebra", "nombreDocente": "Ana Perez",
    "idPeriodoAcademico": "uuid", "nombrePeriodoAcademico": "2026-2",
    "fechaInicioPeriodo": "2026-07-20", "fechaFinPeriodo": "2026-12-10", "estadoPeriodo": "VIGENTE|PROXIMO",
    "horario": "Lunes 08:00-10:00; Martes 10:00-12:00",
    "capacidadMaxima": 30, "estudiantesActivos": 25, "cuposDisponibles": 5,
    "matriculado": false, "idSolicitudAbierta": "uuid|null", "estadoSolicitud": "PENDIENTE|EN_ESPERA|null",
    "tipoAsignatura": "ESPECIFICA|BASICA|INSTITUCIONAL", "transversal": false
  }]
}
```

| Situación | HTTP | Origen |
|---|---|---|
| `pagina < 1`, `tamano` fuera de 1..100, búsqueda mayor de 50 caracteres | 400 | Application (`ValidationException`) |
| Usuario sin estudiante institucional | 403 | Application (`ForbiddenException`) |

El botón "Solicitar ingreso" usa el contrato existente `POST /api/v1/estudiante/solicitudes-matricula` `{grupoId}` (HU053), sin cambios.

## 2. Base de datos

### `dbo.usp_consultar_oferta_grupos_estudiante` (READ_QUERY_RESULTSET)

- **Parámetros:** `@busqueda`, `@numeroPagina`, `@tamanoPagina`, `@idCorrelacion`, `@idUsuarioEjecutor`. Si el ejecutor es nulo, lanza `THROW` con `VAL_001`.
- **Ámbito:** el estudiante ejecutor (`Estudiante.usuario`). Un ejecutor que no es estudiante obtiene 0 filas.
- **Filtro:** períodos con `fechaFin >= hoy local`, y además una de estas dos condiciones:
  - asignatura `ESP` de un programa del estudiante (`EstudiantePrograma`);
  - si `OFERTA.INCLUIR_TRANSVERSALES = 1`, asignatura `BAS` o `INS` de cualquier programa de la institución de sus programas.
- **Columnas**, en este orden:
  - `idGrupo`, `codigoGrupo`, `nombreGrupo`;
  - `idAsignatura`, `codigoAsignatura`, `nombreAsignatura`, `nombreDocente`;
  - `idPeriodoAcademico`, `nombrePeriodoAcademico`, `fechaInicioPeriodo`, `fechaFinPeriodo`;
  - `estadoPeriodo` (VIGENTE o PROXIMO);
  - `horario` (días y horas, ordenados de lunes a domingo, separados por `; `; NULL si no hay horario);
  - `capacidadMaxima`, `estudiantesActivos`, `cuposDisponibles` (mínimo 0);
  - `matriculado` (BIT), `idSolicitudAbierta`, `codigoEstadoSolicitud` (P o E);
  - `codigoComponente` (ESP, BAS o INS);
  - `totalRegistros`.
- **Orden:** asignatura, inicio del período, código e id.

### Parámetro `OFERTA.INCLUIR_TRANSVERSALES`

Tipo INT, 1 por defecto. Seed en `24_parametro_oferta_grupos.sql`. Para ocultar las transversales:

```sql
UPDATE dbo.CatalogoParametro SET valor = '0' WHERE grupo = 'OFERTA' AND clave = 'INCLUIR_TRANSVERSALES';
```

### Regla de inscripción en el programa

Aplica en `usp_registrar_estudiante_en_grupo_interno` y `usp_registrar_estudiante_en_grupo_autonomo`. La fila `EstudiantePrograma` del programa del grupo solo se crea si la asignatura es ESP. Firma, resultset y mensajes no cambian.

## 3. Puertos de Application

`OfertaGruposEstudianteQueryPort.consultarOferta(usuarioEjecutor, busqueda, pagina, tamano)` devuelve `OfertaGruposEstudiantePagina` (los grupos y el total).

## 4. Compatibilidad

- El cambio es aditivo para HTTP.
- Cambia el comportamiento de la matrícula: una transversal ya no inscribe en el programa (regla J4).
