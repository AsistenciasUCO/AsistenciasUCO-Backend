# Identidad `sub` vs `idUsuario`

## Hecho comprobado

El backend actual exige y usa un claim UUID institucional configurable (`idUsuario`). El JWT de prueba existente incluye además `sub`, pero el converter deliberadamente no lo usa como actor institucional.

El material del Security Baseline v0.1.6 inspeccionado describe resolución de bearer/sesión y decisiones PEP, pero no aporta una regla versionada que iguale `sub` con el UUID de la tabla institucional ni una garantía de que el claim `idUsuario` viaje en el token del laboratorio.

## Decisión

No se agrega un mapper `sub → idUsuario`, no se cambia `KEYCLOAK_USER_ID_CLAIM`, no se relajan validadores y no se ejecuta E2E de ownership con una identidad cuya equivalencia no esté probada.

## Evidencia requerida para desbloquear E2E

- Contrato o configuración oficial del emisor que establezca el claim institucional y su formato UUID.
- Registro controlado que relacione una identidad de laboratorio con un `idUsuario` sintético, sin incluir secretos.
- Token de prueba obtenido por el procedimiento oficial, almacenado solo fuera del repositorio o validado en memoria.
- Prueba observable que demuestre `401`/`403`/`2xx` sin alterar las reglas actuales de roles y ownership.

Estado actual: `BLOCKED_BY_MISSING_EVIDENCE`.
