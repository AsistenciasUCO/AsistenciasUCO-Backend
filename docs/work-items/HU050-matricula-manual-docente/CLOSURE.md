---
status: closed
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-17
---

# CLOSURE — HU050: Matrícula Manual de Estudiantes por Docente

## 1. Resumen de Cierre

El work item **HU050-matricula-manual-docente** ha sido completado y validado satisfactoriamente, adaptando los cambios de la rama `feature/hu050-matricula-docente-jpa` a la versión actual de la arquitectura JPA en `develop`.

### Objetivos Cumplidos:
1. **Resolución de Identidad Flexible (3 modalidades soportadas):**
   - **Estudiante existente con `estudianteId`:** Resuelve la información personal a través de `EstudianteRepositoryPort` y enrola en el grupo.
   - **Estudiante existente con solo documento (`tipoIdentificacionId` + `numeroIdentificacion`):** Resuelve la información del usuario a través de `UsuarioRepositoryPort` y enrola en el grupo.
   - **Nuevo estudiante:** Recibe datos completos de registro institucional, crea usuario en IdentityProvider (idempotente) y persiste en BD.
2. **Eliminación de Bloqueo en Controlador:**
   - Se removió la excepción artificial `FeatureUnavailableException` que impedía la matrícula de estudiantes con `estudianteId != null`.
   - El endpoint delega directamente al interactor y responde `201 Created`.
3. **Autorización RBAC Institucional:**
   - Se otorgó acceso al rol `DOCENTE` sobre `/api/v1/estudiantes/**` para que el componente frontend de búsqueda y autocompletado opere sin recibir HTTP 403 Forbidden.
   - Se mantuvo la protección de `POST /api/v1/grupos/{grupoId}/estudiantes` para `DOCENTE` y `COORDINADOR`.
4. **Preservación de JPA Moderno:**
   - La persistencia se realiza a través de `GrupoJpaRepository` y `JpaProcedureExecutor` invocando el procedimiento canónico `dbo.usp_registrar_estudiante_en_grupo`, sin reintroducir clases legacy ni romper la arquitectura limpia.

---

## 2. Definition of Done (DoD)

- [x] Contratos formalizados y sin drift (`CONTRACT.md` y `PLAN.md`).
- [x] Pruebas unitarias completas para el nuevo interactor (`RegistrarEstudianteInteractorTest`: 5/5 PASS).
- [x] Pruebas de controladores actualizadas (`GrupoControllerWriteEndpointsTest`: 9/9 PASS).
- [x] Pruebas de seguridad RBAC validadas (`RbacSecurityFilterChainTest`: 40/40 PASS).
- [x] Inyección de dependencias verificada (`GrupoWiringConfigurationTest`: 1/1 PASS).
- [x] Clean Architecture preservada (cero dependencias de infraestructura en el dominio/aplicación).
- [x] Sin regresiones en pruebas del caso de uso (`RegistrarEstudianteUseCaseImplTest`: 12/12 PASS).

**Estado Final:** CLOSED / MERGED ON DEVELOP
