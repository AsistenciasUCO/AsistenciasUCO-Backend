---
status: closed
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-19
---

# VALIDATION — HU120: Crear y Aprovisionar Coordinador de Programa en Facultad

## 1. Resumen de Evidencias de Ejecución

Todas las verificaciones planificadas en [`TEST_PLAN.md`](./TEST_PLAN.md) para `HU120` fueron ejecutadas y validadas en las capas de Base de Datos, Backend, Frontend e Infraestructura WORM.

---

## 2. Pruebas Unitarias de Aplicación, Controlador y Persistencia (Backend)
- **Comando:**
  ```powershell
  cd GestioAsistenciaBackend; .\mvnw.cmd test "-Dtest=CrearCoordinadorUseCaseImplTest,ConsultarCoordinadoresUseCaseImplTest,DecanoPortalControllerTest,CoordinadorJpaCommandPatternTest"
  ```
- **Resultado:** `14/14 PASS` (`Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`).
  - `CrearCoordinadorUseCaseImplTest`: `4/4 PASS` (incluyendo aprovisionamiento con `idPrograma = null`, codificación de contraseña con `PasswordEncoderPort` y creación de cuenta IAM con `InstitutionalRole.COORDINADOR`).
  - `ConsultarCoordinadoresUseCaseImplTest`: `3/3 PASS` (incluyendo unificación y deduplicación de coordinadores de la facultad con coordinadores disponibles sin programa asignado).
  - `DecanoPortalControllerTest`: `4/4 PASS`.
  - `CoordinadorJpaCommandPatternTest`: `3/3 PASS`.

---

## 3. Compilación y Tipado de Pruebas en Frontend (Angular 19)
- **Comandos:**
  - `cd GestioAsistenciaFrontend; npm run build -- --configuration=development` ➔ `EXIT CODE 0` (Bundle generado sin errores).
  - `cd GestioAsistenciaFrontend; npx tsc -p tsconfig.spec.json --noEmit` ➔ `EXIT CODE 0` (Cero errores de compilación TypeScript en `dean-management.service.spec.ts`, `dean-coordinadores.component.spec.ts` y toda la suite de specs).

---

## 4. Base de Datos (SQL Server 2022+)
- **Objetos Actualizados:**
  - [`GestioAsistenciaDB/schema/views/uv_coordinador.sql`](file:///c:/Proyectos/GestioAsistencia/GestioAsistenciaDB/schema/views/uv_coordinador.sql): `LEFT JOIN uv_programa` y cálculo de `estaActivoCoordinador` para coordinadores con o sin programa.
  - [`GestioAsistenciaDB/schema/stored-procedures/usp_crear_coordinador.sql`](file:///c:/Proyectos/GestioAsistencia/GestioAsistenciaDB/schema/stored-procedures/usp_crear_coordinador.sql): `SET XACT_ABORT ON;`, control transaccional explícito, cero `ISNULL`/`COALESCE` en cabecera, cero `SELECT` a tablas físicas, soporte de `@idPrograma = NULL` y `@idCoordinador = NULL` con validación jerárquica de custodia sobre `@idFacultad`.
  - [`GestioAsistenciaDB/test/test_usp_crear_coordinador.sql`](file:///c:/Proyectos/GestioAsistencia/GestioAsistenciaDB/test/test_usp_crear_coordinador.sql): Escenario 5 (`@corrSinPrograma`) incorporado.

---

## 5. Pre-Flight Check de Infraestructura y Contratos WORM
- **Comando:** `.\AsistenciasUCO-Infra\infra.ps1 verify`
- **Resultado:** `[PASS] PRE-FLIGHT CHECK EXITOSO`
  - Escaneo DevSecOps: `0` secretos o credenciales expuestas.
  - Verificación WORM SHA-256: `18/18` comprobaciones en `[OK]` con paridad cero drift.

---

## 6. Dictamen de Auditoría
- **Estado de Calidad:** `APROBADO (PASS)`
- **Regresiones:** Ninguna detectada.
- **Autorización:** Proceder a cierre (`CLOSURE.md`).
