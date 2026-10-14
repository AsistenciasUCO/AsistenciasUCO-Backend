# Guía de prueba manual (cuando se desbloquee el laboratorio)

## Preparación segura

1. Obtener la imagen/organización y credenciales desde el canal operativo autorizado; no escribirlas en Git.
2. Copiar `lab.env.example` a un archivo local ignorado y completar variables externamente.
3. Validar `docker compose ... config --quiet`.
4. Confirmar que los puertos del laboratorio estén libres y que el backend estable no comparta volúmenes.

## Pruebas observables

1. Flag OFF + request existente: comprobar que el comportamiento coincida con el backend AS-IS.
2. Flag ON + sin Bearer: esperar `401` del backend; no declarar éxito del PEP por esta sola prueba.
3. JWT institucional válido + PEP permite: comprobar continuación a un endpoint autorizado.
4. JWT válido + PEP deniega: comprobar `403` y que el handler de negocio no se ejecute.
5. PEP detenido/timeout: comprobar `503` y que no haya bypass.
6. JWT válido + rol insuficiente: comprobar que un `2xx` PEP no conceda el rol y Spring responda `403`.
7. Repetir con un recurso no propio cuando exista fixture contractual: comprobar que ownership siga en Application/DB.

Registrar URL, timestamp, correlation ID, status, código de error y resultado del handler, pero nunca token, Cookie, client secret o contraseña.

Estado: `NOT_RUN`; falta evidencia de identidad y acceso al runtime Docker.
