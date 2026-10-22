# Cierre y Certificación: HU076 - Agregar nuevo programa académico a la facultad

- **ID Work Item**: `HU076-agregar-programa-academico`
- **Historia de Usuario**: `HU076`
- **Estado Final**: `[CERTIFICADA]`

## Resumen de Entrega End-to-End

1. **Capa de Datos (`Repositorio_GestionAsistenciadb`)**:
   - Procedimientos almacenados `dbo.usp_crear_programa_academico` y `dbo.usp_crear_programa_academico_interno` estandarizados bajo reglas de seguridad *Fail-Closed*, validación RBAC (`DECANO`), titularidad jerárquica (`FACULTAD`), integridad referencial sobre vistas canónicas (`uv_facultad`, `uv_tipo_programa`, `uv_coordinador_identidad`) y unicidad de nombre (`GEN_003`).
   - Vistas `dbo.uv_programa` y `dbo.uv_auth_programa` enriquecidas con `idTipoPrograma` y `nombreTipoPrograma`.
   - Suite de pruebas `test/test_usp_crear_programa_academico.sql` integrada en `test_suite.sql`.
2. **Capa de Backend (`Repositorio_GestionAsistenciaBackend`)**:
   - Casos de uso puros en `application/features/decano/` (`consultarprogramas`, `consultartiposprograma`, `crearprograma`), adaptadores JPA sobre `uv_programa`, `uv_tipo_programa` y `usp_crear_programa_academico`, wiring en `DecanoWiringConfiguration` y endpoints REST en `DecanoPortalController`.
3. **Capa de Frontend (`Repositorio_GestionAsistenciaFrontend`)**:
   - Integración en `DeanManagementService` y `DeanProgramasComponent` permitiendo al Decano consultar los programas de su facultad y dar de alta nuevos programas académicos seleccionando su modalidad (`pregrado`/`postgrado`) y coordinador titular.
4. **Gobierno y Trazabilidad (`Repositorio_GestionAsistenciaInfra`)**:
   - Actualización de estado a `[CERTIFICADA]` en `HU-CATALOG.md` y `HOJA_DE_RUTA_JERARQUIA_ROLES.md`.
