---
status: closed
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-17
---

# VALIDATION — HU050: Matrícula Manual de Estudiantes por Docente

## Evidencias de Ejecución

### 1. Pruebas Unitarias de Interactor y Negocio
- **Comando:** `.\mvnw.cmd test "-Dtest=RegistrarEstudianteInteractorTest,RegistrarEstudianteUseCaseImplTest"`
- **Resultados:**
  - `RegistrarEstudianteInteractorTest`: 5/5 PASS (0 failures, 0 errors, 0 skipped).
    - Escenario 1: Registro con datos completos -> delega al caso de uso.
    - Escenario 2: Matrícula con `estudianteId` -> resuelve datos personales vía `EstudianteRepositoryPort`.
    - Escenario 3: Matrícula por documento de identidad -> resuelve usuario vía `UsuarioRepositoryPort`.
    - Escenario 4: Matrícula por documento inexistente -> lanza `ValidationException`.
    - Escenario 5: Matrícula por `estudianteId` inexistente -> lanza `ResourceNotFoundException`.
  - `RegistrarEstudianteUseCaseImplTest`: 12/12 PASS (0 failures, 0 errors, 0 skipped).

### 2. Pruebas de Endpoints y Controladores HTTP
- **Comando:** `.\mvnw.cmd test "-Dtest=GrupoControllerWriteEndpointsTest"`
- **Resultados:**
  - `GrupoControllerWriteEndpointsTest`: 9/9 PASS.
  - Verifica que `POST /api/v1/grupos/{grupoId}/estudiantes` responda `201 Created` y no `FeatureUnavailableException`.

### 3. Pruebas de Seguridad y RBAC
- **Comando:** `.\mvnw.cmd test "-Dtest=RbacSecurityFilterChainTest"`
- **Resultados:**
  - `RbacSecurityFilterChainTest`: 40/40 PASS.
  - Rol `DOCENTE` autorizado para consultar `GET /api/v1/estudiantes/**` (para validación de identidad en frontend).
  - Rol `DOCENTE` autorizado para registrar alumnos en `POST /api/v1/grupos/{grupoId}/estudiantes`.

### 4. Pruebas de Inyección de Dependencias y Wiring
- **Comando:** `.\mvnw.cmd test "-Dtest=GrupoWiringConfigurationTest"`
- **Resultados:**
  - `GrupoWiringConfigurationTest`: 1/1 PASS.
  - Se inyectan correctamente `UsuarioRepositoryPort` y `EstudianteRepositoryPort` en el bean `RegistrarEstudianteInputPort`.

### 5. Resumen de Calidad
- **Total de pruebas ejecutadas:** 67 tests.
- **Fallos:** 0.
- **Errores:** 0.
- **Regresiones:** Ninguna detectada.
