# TEST PLAN — P0-B2 log hygiene

## RED

1. Ejecutar el harness PowerShell contra un directorio temporal.
2. Verificar que el runner/helper actual no puede resolver su destino o que no
   existe la política de rotación requerida.
3. No editar una prueba RED para acomodar la implementación.

## GREEN

1. La inicialización crea la carpeta de logs y los archivos activos esperados.
2. Una segunda ejecución rota los activos, conserva los históricos dentro de
   la política y elimina únicamente archivos temporales del fixture.
3. Se eliminan logs fuera de retención o por encima del límite de cantidad y
   tamaño, sin tocar archivos no pertenecientes al prefijo del runner.
4. El runner devuelve el código de salida del proceso hijo.

## VALIDATE

- `powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scripts\tests\test-local-logging.ps1`
- `powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scripts\run-local.ps1 -Help`
- ` .\mvnw.cmd -B -ntp verify` si el cambio no altera el código de aplicación.
- Arranque/reinicio real solo si los servicios requeridos están disponibles;
  en otro caso `NOT_RUN` con evidencia.

## Gates

Los tests PowerShell prueban el comportamiento de la herramienta. No
certifican Spring Boot, SQL Server, Keycloak ni Docker.

