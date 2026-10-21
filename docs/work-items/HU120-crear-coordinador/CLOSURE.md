---
status: closed
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-19
---

# CLOSURE — HU120: Crear y Aprovisionar Coordinador de Programa en Facultad

## 1. Resumen de Cierre

El work item **`HU120-crear-coordinador`** ha sido completado, validado y certificado en las tres capas arquitectónicas (Base de Datos, Backend y Frontend), resolviendo el candado circular relacional con `dbo.Programa.[coordinador] NOT NULL` y habilitando el aprovisionamiento completo de Coordinadores (con sincronización IAM en Keycloak bajo rol `COORDINADOR`) dentro de la Facultad del Decano.

### Entregables Certificados:
1. **Base de Datos (`GestioAsistenciaDB`):**
   - `dbo.uv_coordinador` actualizada con `LEFT JOIN uv_programa` para listar coordinadores disponibles sin programa asignado.
   - `dbo.usp_crear_coordinador` actualizado para permitir `@idPrograma = NULL` y `@idCoordinador = NULL`, validando la custodia jerárquica del Decano sobre `@idFacultad`.
   - Prueba transaccional agregada en `test/test_usp_crear_coordinador.sql` (Escenario 5).
2. **Backend (`GestioAsistenciaBackend`):**
   - `CrearCoordinadorUseCaseImpl` y `DecanoWiringConfiguration` integrados con `UsuarioRepositoryPort`, `PasswordEncoderPort` e `IdentityProviderPort` (`InstitutionalRole.COORDINADOR`).
   - `CoordinadorQueryPort`, `CoordinadorJpaRepository` y `ConsultarCoordinadoresUseCaseImpl` extendidos con `consultarCoordinadoresSinPrograma()` y deduplicación determinista por `id`.
   - Suite de pruebas unitarias (`CrearCoordinadorUseCaseImplTest`, `ConsultarCoordinadoresUseCaseImplTest`, `DecanoPortalControllerTest`, `CoordinadorJpaCommandPatternTest`) pasando `14/14`.
3. **Frontend (`GestioAsistenciaFrontend`):**
   - `role-management.model.ts`, `dean-management.service.ts` y `dean-coordinadores.component.ts` actualizados para permitir crear coordinadores sin programa inicial y mostrarlos como `'Sin programa asignado (Disponible)'`.
   - Pruebas `dean-management.service.spec.ts` y `dean-coordinadores.component.spec.ts` actualizadas y verificadas.

---

## 2. Definition of Done (DoD)

- [x] **Fase 01 (PLAN):** `PLAN.md` elaborado y aprobado (DoR: `READY`).
- [x] **Fase 02 (CONTRACT):** `CONTRACT.md` formalizado para DB, Backend REST y Frontend.
- [x] **Fase 03 (TEST RED):** `TEST_PLAN.md` y casos de prueba definidos en las 3 capas.
- [x] **Fase 04 (IMPL GREEN):** Implementación completada en DB, Backend y Frontend.
- [x] **Fase 05 (AUDIT):** Evidencias de validación y `infra.ps1 verify` registradas en `VALIDATION.md`.
- [x] **Fase 06 (CLOSURE):** `CLOSURE.md` emitido y sincronizado en el índice maestro de Work Items.

**Estado Final:** `CERTIFICADA / READY FOR PRODUCTION`
