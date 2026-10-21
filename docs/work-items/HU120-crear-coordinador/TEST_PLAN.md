# 🧪 TEST PLAN — HU120: Crear y Aprovisionar Coordinador de Programa en Facultad

> **Historia de Usuario:** HU120 (Crear y asignar Coordinador a Programa / Aprovisionamiento en Facultad)  
> **Work Item:** `HU120-crear-coordinador`  
> **Estado:** `RED_PLANNED`  
> **Fase SDD:** 03. TEST RED  
> **Ámbito:** Base de Datos (`GestioAsistenciaDB`), Backend (`GestioAsistenciaBackend`) y Frontend (`GestioAsistenciaFrontend`).  
> **Fecha:** 2026-10-19

---

## 🎯 1. Estrategia y Objetivos de Prueba

Verificar anticipadamente bajo TDD estricto el contrato definido en [`CONTRACT.md`](./CONTRACT.md):
1. **Base de Datos (`GestioAsistenciaDB/test/test_usp_crear_coordinador.sql`):** Validar que `dbo.usp_crear_coordinador` soporte tanto la asignación a un programa existente como el aprovisionamiento sin programa (`@idPrograma = NULL`), exponiendo el coordinador recién creado en `dbo.uv_coordinador` y rechazando datos inválidos o incoherencias de facultad.
2. **Backend (`GestioAsistenciaBackend`):** Validar en `CrearCoordinadorUseCaseImplTest` y `ConsultarCoordinadoresUseCaseImplTest` la codificación de contraseña, el aprovisionamiento IAM con `InstitutionalRole.COORDINADOR` y la combinación deduplicada de coordinadores con y sin programa.
3. **Frontend (`GestioAsistenciaFrontend`):** Validar en `dean-management.service.spec.ts` y `dean-coordinadores.component.spec.ts` el registro de coordinador con `idPrograma = null` y la apertura del formulario cuando la facultad tiene `0` programas.

---

## 📊 2. Matriz de Casos de Prueba

| ID Caso | Capa / Componente | Escenario / Entrada | Resultado Esperado | Nivel |
| :--- | :--- | :--- | :--- | :---: |
| **TC-DB-01** | SQL Server / SP | Crear coordinador asignado a `@idPrograma` válido de la facultad | `estadoResultado = 1`, `dbo.Programa.coordinador` actualizado. | Integración DB |
| **TC-DB-02** | SQL Server / SP | Crear coordinador con `@correo = NULL` | `estadoResultado = 0`, código `GEN_002`. | Integración DB |
| **TC-DB-03** | SQL Server / SP | Crear coordinador con `@idPrograma` inexistente | `estadoResultado = 0`, código `PROG_001`. | Integración DB |
| **TC-DB-04** | SQL Server / SP | Crear coordinador con `@idFacultad` distinta a la del programa | `estadoResultado = 0`, error `ERR_PROGRAMA_FACULTAD_INCONSISTENTE`. | Integración DB |
| **TC-DB-05** | SQL Server / SP + Vista | Crear coordinador con `@idPrograma = NULL` y `@idCoordinador = NULL` en facultad válida | `estadoResultado = 1`, coordinador visible en `dbo.uv_coordinador` con `idPrograma IS NULL` y `estaActivoCoordinador = 1`. | Integración DB |
| **TC-BK-01** | Backend / UseCase | `CrearCoordinadorUseCaseImpl` con `idPrograma = null` y puertos IAM activos | Codifica password con `PasswordEncoderPort`, invoca `commandPort.crearCoordinador` e invoca `IdentityProviderPort.crearCuenta` con `InstitutionalRole.COORDINADOR`. | Unitario |
| **TC-BK-02** | Backend / UseCase | `ConsultarCoordinadoresUseCaseImpl` con coordinadores de facultad y coordinadores sin programa | Combina ambas listas y deduplica por `id`. | Unitario |
| **TC-BK-03** | Backend / Controller | `POST /api/v1/decano/coordinadores` y `GET /api/v1/decano/coordinadores` | HTTP `201 Created` y `200 OK` en `DecanoPortalControllerTest`. | Unitario MockMvc |
| **TC-FE-01** | Frontend / Service | `DeanManagementService.crearCoordinador` sin `idPrograma` | Envía `idPrograma: null` en `POST /api/v1/decano/coordinadores`. | Unitario Angular |
| **TC-FE-02** | Frontend / Component | `DeanCoordinadoresComponent` con `programas = []` y envío sin `idPrograma` | Permite abrir modal (`openCreateForm`) y emite `crearCoordinador` con `idPrograma: null`. | Unitario Angular |
