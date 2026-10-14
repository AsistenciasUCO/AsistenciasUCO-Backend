# Infraestructura aislada

## Release inspeccionado

- Repositorio local: `.workspace/security-baseline-local`.
- Tag: `v0.1.6` (`272c8e3168aeee6934194e2278b3537dc3c5dd78`).
- Assets publicados descargados para lectura: `compose.yaml`, `default.env.example`, `MANUAL.md`, `README.md`.
- No se encontró ninguno de los dos manuales PDF solicitados.

## Laboratorio preparado

- `.workspace/security-baseline-local/release-assets/compose.lab.yaml`
- `.workspace/security-baseline-local/release-assets/lab.env.example`
- Proyecto Compose: `asistencias-security-lab`.
- Puertos publicados solo en `127.0.0.1`: `19090`, `18080`, `18081`.
- Volúmenes con nombres exclusivos del laboratorio.
- Credenciales: placeholders y variables externas; ningún valor real versionado.

## Resultado de configuración

`docker compose --project-name asistencias-security-lab --env-file lab.env.example -f compose.lab.yaml config --quiet` terminó con exit code `0`.

El engine Docker no pudo inspeccionarse desde esta sesión por acceso denegado al named pipe de Docker Desktop. No se levantaron contenedores y no se declara PASS de integración. La imagen/organización real tampoco se inventa desde el repositorio.

## Finding de seguridad

`SECURITY_FINDING`: archivo `.workspace/security-baseline-local/distribution/compose.yaml`; tipo: credencial literal en variable de entorno de Compose. El valor no se reproduce.
