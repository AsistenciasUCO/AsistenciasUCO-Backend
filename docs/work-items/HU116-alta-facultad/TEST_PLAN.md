# 🧪 TEST PLAN — HU116: Alta Institucional de Nueva Facultad

> **Historia de Usuario:** HU116 (Alta institucional de nueva facultad)  
> **Work Item:** `HU116-alta-facultad`  
> **Estado:** `RED_PLANNED`  
> **Fase SDD:** 03. TEST RED  
> **Ámbito:** Base de Datos (`GestioAsistenciaDB`), Backend (`GestioAsistenciaBackend`) y Frontend (`GestioAsistenciaFrontend`).  
> **Fecha:** 2026-10-17

---

## 🎯 1. Estrategia y Objetivos de Prueba

El propósito de este plan de pruebas es garantizar la verificación estricta y anticipada (TDD / Test-First) del comportamiento observable definido en [`CONTRACT.md`](./CONTRACT.md), asegurando:
1. **Transaccionalidad Atómica y Salvaguardas en DB:** `dbo.usp_crear_facultad` debe ejecutar con `SET XACT_ABORT ON;`, cero transacciones huérfanas, validaciones de obligatoriedad, unicidad de nombre y retorno de la tupla estándar de 4 columnas.
2. **Clean Architecture y Aislamiento en Backend:** `CrearFacultadUseCaseImpl` y `AdminPortalController` deben procesar las reglas de dominio, mapear DTOs UI-Ready, emitir respuestas HTTP REST (`201`, `400`, `409`, `403`) y no acoplar lógica en infraestructura.
3. **Inviolabilidad de Pruebas (Zero Test Tampering):** Prohibido relajar, comentar o alterar aserciones para forzar un pase.

---

## 📊 2. Matriz de Casos de Prueba

| ID Caso | Capa / Componente | Escenario / Entrada | Resultado Esperado | Nivel |
| :--- | :--- | :--- | :--- | :---: |
| **TC-DB-01** | SQL Server / SP | Inserción con nombre válido no existente | `exitoso = 1`, `idResultado != NULL`, registro insertado en `dbo.Facultad` con `estado = 1`. | Integración DB |
| **TC-DB-02** | SQL Server / SP | Inserción con `@nombre = NULL` o `@nombre = '  '` | `exitoso = 0`, mensaje de validación, cero inserciones. | Integración DB |
| **TC-DB-03** | SQL Server / SP | Inserción con nombre existente (case-insensitive) | `exitoso = 0`, mensaje de conflicto, cero duplicados en `dbo.Facultad`. | Integración DB |
| **TC-DB-04** | SQL Server / SP | Manejo de transacciones en error o rollback | Cero transacciones abiertas (`@@TRANCOUNT == 0`), aislamiento transaccional intacto. | Integración DB |
| **TC-BK-01** | Backend / UseCase | Insumo válido `CrearFacultadDomain` | Invocación exitosa al puerto `FacultadCommandPort`, retorna `FacultadDTO` completo con estado activo. | Unitario |
| **TC-BK-02** | Backend / UseCase | Insumo con nombre en blanco o menor a 3 caracteres | Lanza `ValidationException` antes de persistir, cero llamadas al repositorio. | Unitario |
| **TC-BK-03** | Backend / UseCase | Repositorio reporta conflicto de unicidad | Captura el error de persistencia y lanza `ConflictException` tipada (HTTP 409). | Unitario |
| **TC-BK-04** | Backend / Controller | `POST /api/v1/admin/facultades` con payload válido | HTTP `201 Created`, JSON con `ApiDataResponse<FacultadDTO>`, delegación a `CrearFacultadInputPort`. | Unitario MockMvc |
| **TC-BK-05** | Backend / Controller | `POST /api/v1/admin/facultades` con body vacío | HTTP `400 Bad Request` procesado por `GlobalExceptionHandler`. | Unitario MockMvc |
| **TC-BK-06** | Backend / Security | Invocación sin token o con rol ajeno a `ADMINISTRADOR` | HTTP `401 Unauthorized` o `403 Forbidden`. | Integración Seguridad |
| **TC-BK-07** | Backend / UseCase | Insumo con `idDecano = NULL` | Lanza `ValidationException` ("El decano titular de la facultad es obligatorio."), cero persistencia. | Unitario |
| **TC-FE-01** | Frontend / Component | Envío de formulario `catalog-facultades.component.ts` | Dispara `save.emit()`, actualiza Signal reactivo `_facultades` y muestra Toast. | Unitario Angular |

---

## 🛠️ 3. Implementación de Artefactos de Prueba (Fase RED)

### 3.1 Script de Pruebas SQL (`GestioAsistenciaDB/test/test_usp_crear_facultad.sql`)
- Crea una tabla temporal `#facultadResultado` con las 4 columnas de contrato (`idResultado`, `exitoso`, `mensajeUsuario`, `mensajeTecnico`).
- Evalúa los 4 escenarios de base de datos bajo transacción segura con rollback final para garantizar la idempotencia de los fixtures.

### 3.2 Pruebas Unitarias de Backend (`CrearFacultadUseCaseImplTest.java`)
- Pruebas con JUnit 5 y Mockito para `CrearFacultadUseCaseImpl` cubriendo Happy Path, nombres inválidos, conflictos y fallos de infraestructura.

### 3.3 Pruebas de Controlador MockMvc (`AdminPortalControllerTest.java`)
- Actualización de las pruebas del controlador para validar el endpoint `POST /api/v1/admin/facultades` esperando HTTP `201 Created`.

---

## 🚦 4. Criterios de Transición RED ➔ GREEN

1. **Estado Inicial Esperado (RED):**
   - El script SQL falla si `usp_crear_facultad` no cumple la tupla de 4 columnas o la firma acordada.
   - Los tests de backend no compilan o fallan al no existir `CrearFacultadInputPort` ni la implementación en `AdminPortalController`.
2. **Condición para GREEN:**
   - La implementación en producción hace pasar el 100% de los casos sin alterar ninguna aserción del test.
