---
status: closed
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-17
---

# TEST_PLAN — HU050: Matrícula Manual de Estudiantes por Docente

## Fuentes

- [PLAN](PLAN.md)
- [CONTRACT](CONTRACT.md)
- Endpoint canónico: `POST /api/v1/grupos/{grupoId}/estudiantes`
- Endpoint auxiliar de consulta de identidad: `GET /api/v1/estudiantes`
- Procedimiento almacenado canónico: `dbo.usp_registrar_estudiante_en_grupo`

---

## Behavioral Matrix

| ID | Requisito / Escenario | Precondición | Acción / Entrada | Observable | Resultado Esperado | Nivel |
|---|---|---|---|---|---|---|
| T-HU050-01 | Matrícula de nuevo estudiante con datos completos | Usuario no existe en BD institucional | `POST /grupos/{id}/estudiantes` con DTO completo (tipoId, doc, nombres, apellidos, correo, password) | Status HTTP, body y llamada a caso de uso | `201 Created`, `exitoso=true`, delega al caso de uso con domain completo | Controller + Interactor |
| T-HU050-02 | Matrícula de estudiante existente por `estudianteId` | Estudiante registrado previamente en el sistema | `POST /grupos/{id}/estudiantes` con solo `estudianteId` | Resolución de datos en `EstudianteRepositoryPort` y ejecución | `201 Created`, resuelve nombres/correo y delega al caso de uso | Controller + Interactor |
| T-HU050-03 | Matrícula de estudiante existente por documento | Usuario institucional existe en BD (`UsuarioRepositoryPort`) | `POST /grupos/{id}/estudiantes` con solo `tipoIdentificacionId` y `numeroIdentificacion` | Resolución en `UsuarioRepositoryPort` y mapping a domain | `201 Created`, resuelve identidad institucional y delega a caso de uso | Controller + Interactor |
| T-HU050-04 | Error al matricular por documento inexistente | Documento no existe en BD institucional | `POST /grupos/{id}/estudiantes` con documento desconocido y sin datos personales | Excepción capturada en interactor | `ValidationException` exigiendo datos completos para nuevo registro | Interactor Slice |
| T-HU050-05 | Error al matricular por `estudianteId` inexistente | ID no existe en BD | `POST /grupos/{id}/estudiantes` con `estudianteId` inexistente | Excepción capturada en interactor | `ResourceNotFoundException` | Interactor Slice |
| T-HU050-06 | Validación de autorización RBAC docente en `/api/v1/estudiantes/**` | JWT con rol `DOCENTE` | `GET /api/v1/estudiantes?tipoIdentificacionId=...` | HTTP Status en SecurityFilterChain | `200 OK` (No 403 Forbidden) | Security Slice (`RbacSecurityFilterChainTest`) |
| T-HU050-07 | Validación de autorización RBAC en `POST /api/v1/grupos/{id}/estudiantes` | JWT con rol `DOCENTE` o `COORDINADOR` | `POST /api/v1/grupos/{id}/estudiantes` | HTTP Status en SecurityFilterChain | Permitido (`201 Created` o validación de negocio) | Security Slice (`RbacSecurityFilterChainTest`) |
| T-HU050-08 | Wiring canónico de Clean Architecture en IoC Spring | Contexto de Spring / Configuration | Creación del bean `RegistrarEstudianteInputPort` | Inyección de puertos secundarios | Bean creado con `RegistrarEstudianteUseCase`, `UsuarioRepositoryPort` y `EstudianteRepositoryPort` | Wiring Unit Test (`GrupoWiringConfigurationTest`) |

---

## Estrategia de Pruebas

1. **Unitarias / Slice (MockMvc y Mockito):**
   - `RegistrarEstudianteInteractorTest`: Verifica los 5 caminos de resolución de identidad (datos completos, `estudianteId`, solo documento, documento no encontrado, estudianteId no encontrado).
   - `GrupoControllerWriteEndpointsTest`: Verifica la aceptación del request en el controlador REST sin lanzar `FeatureUnavailableException`.
   - `RbacSecurityFilterChainTest`: Verifica que la cadena de filtros de Spring Security permita a `DOCENTE` consultar el directorio de estudiantes y registrar en el grupo.
   - `GrupoWiringConfigurationTest`: Valida la inyección de dependencias y el ensamblado arquitectónico hexagonal sin violaciones de inversión de control.

2. **Integración con Base de Datos:**
   - La persistencia se delega a `GrupoJpaRepository` invocando el SP `dbo.usp_registrar_estudiante_en_grupo`, cubierto por la suite de integración de JPA y pruebas E2E.
