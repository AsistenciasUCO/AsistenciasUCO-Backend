# Handoff

## Entregables

- Rama experimental: `jose-valencia/experiment-security-baseline-mvc`.
- Worktree: `.w/sec-mvc`.
- Laboratorio local: `.workspace/security-baseline-local`.
- Contrato y límites: documentos de este directorio.

## No afirmar

- No afirmar compatibilidad E2E con Security Baseline.
- No afirmar que `sub` es `idUsuario`.
- No afirmar PASS de Docker, proveedor real, cobertura global o merge.
- No activar el flag en `develop` ni en ambientes compartidos.

## Próximo desbloqueo

Un responsable de identidad/seguridad debe aportar la regla versionada `sub`/`idUsuario`, los manuales PDF o confirmar que no son la fuente aplicable, y un procedimiento de credenciales fuera del repositorio. Después se puede ejecutar el laboratorio y la matriz manual sin cambiar el contrato por inferencia.
