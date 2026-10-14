# Decisiones

## D-001 — no incorporar WebFlux

El backend AS-IS es Spring MVC y el starter oficial es WebFlux. Copiarlo directamente cambiaría el runtime y el contrato de infraestructura. Se implementa un filtro servlet mínimo con el mismo comportamiento observable y sin dependencia WebFlux.

## D-002 — PEP embebido, no PDP directo

El código oficial expone `/internal/v1/embedded-access-decisions` para aplicaciones protegidas. El contrato PEP→PDP es interno al Security Baseline y no se implementa en este backend.

## D-003 — conservar JWT institucional

El flag agrega una barrera de autorización externa después de la autenticación local. No cambia issuer, audience, `idUsuario`, roles ni ownership. Esto permite rollback apagando una propiedad y evita una migración de identidad implícita.

## D-004 — fail-closed

La indisponibilidad o una respuesta inválida del PEP no debe abrir rutas protegidas. El estado se expone como `503` con el envelope de error técnico vigente; no se inventa un código público nuevo en esta fase experimental.

## D-005 — identidad bloqueada

No hay evidencia versionada que pruebe que el `sub` de Security Baseline sea el UUID institucional `idUsuario`, ni que el PEP garantice esa equivalencia. Por tanto, el E2E de una identidad nueva queda `BLOCKED_BY_MISSING_EVIDENCE`.

## D-006 — laboratorio separado

El laboratorio usa `asistencias-security-lab`, loopback y volúmenes propios. No ejecuta `down -v`, no comparte recursos con el stack estable y no recibe credenciales desde archivos versionados.
