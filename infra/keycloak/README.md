---
status: active
type: runbook
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Keycloak — AsistenciasUCO (LOCAL/DEV)

Infraestructura local de identidad y autenticacion para AsistenciasUCO, con bootstrap
automatizado y reproducible via Admin REST API. Ya **no** depende de configuracion manual desde
Keycloak Admin Console para llegar al contrato que el backend espera.

Esta automatizacion cubre unicamente LOCAL/DEV. No cambia logica de negocio, seguridad Java,
endpoints, SQL Server ni frontend.

## 1. Arquitectura: Keycloak/PostgreSQL vs SQL Server institucional

AsistenciasUCO tiene **dos bases de datos independientes que nunca se sincronizan
directamente**:

- **PostgreSQL** (este `infra/keycloak/compose.yaml`): almacenamiento propio de Keycloak
  (realm, clients, roles, usuarios de IdP, credenciales). Keycloak es dueño exclusivo de esta
  base; nada del backend la consulta directamente.
- **SQL Server institucional** (fuera de este directorio): la fuente de verdad del dominio de
  negocio — `dbo.Usuario` y demas tablas institucionales.

El vinculo entre ambos mundos es el atributo de usuario **`idUsuario`**: cuando el backend
provisiona una identidad en Keycloak (`KeycloakIdentityProviderAdapter`, ver
`docs/security/keycloak-identity-provider.md`), escribe `idUsuario = dbo.Usuario.id` exacto como
atributo del usuario de Keycloak. Ese mismo valor viaja luego en el `access_token` (claim
`idUsuario`, ver seccion 6) y es lo que el backend usa para resolver "quien es este request" sin
depender de `sub`, `username` ni `email` de Keycloak.

**Flujo DB-first de provisioning**: el backend primero persiste/actualiza en SQL Server, y solo
despues llama a `IdentityProviderPort.crearCuenta(...)` con el `idUsuario` ya resuelto de SQL
Server. Keycloak nunca genera un UUID institucional; siempre recibe el que ya existe en SQL
Server. Ver `docs/security/keycloak-identity-provider.md` para el detalle completo del flujo.

**Bootstrap de infraestructura vs provisioning funcional** — no confundir:

| | Bootstrap de infraestructura (este directorio) | Provisioning funcional |
|---|---|---|
| Que configura | Realm, clients, roles, client scope, mappers, permisos del service account | Usuarios institucionales individuales |
| Quien lo ejecuta | Un desarrollador/ops, manualmente, fuera del backend | El backend en runtime, via `IdentityProviderPort` |
| Cuando | Al levantar/reconciliar el ambiente | En cada operacion de negocio (crear estudiante, decano, etc.) |
| Scripts | `bootstrap-keycloak.ps1`, `seed-e2e-users.ps1` (solo E2E) | `KeycloakIdentityProviderAdapter` |

El backend de negocio **nunca** crea clients, scopes ni el realm en runtime — eso es
exclusivamente responsabilidad de `bootstrap-keycloak.ps1`.

## 2. Los tres clients

| Client ID | Tipo | Responsabilidad |
|---|---|---|
| `asistencias-api` | Confidential, sin flows interactivos | Resource/API client: namespace de los 5 roles institucionales. Nunca hace login. |
| `asistencias-uco-frontend` | Public (SPA) | Login interactivo del frontend Angular. |
| `asistencias-backend-admin` | Confidential, service account | Usado exclusivamente por `KeycloakIdentityProviderAdapter` para la Admin REST API. |

## 3. Client scope `asistencias-api-scope`

Asignado como **default** (no optional) al client `asistencias-uco-frontend`, para que el
frontend no tenga que pedir explicitamente `scope=asistencias-api-scope`. Incluye dos protocol
mappers:

- **`idUsuario`**: `oidc-usermodel-attribute-mapper` que copia el atributo de usuario
  `idUsuario` al claim `idUsuario` del access token (y del ID token).
- **`audience-asistencias-api`**: `oidc-audience-mapper` oficial de Keycloak que agrega
  `asistencias-api` al claim `aud` del access token.

El scope debe tener **cero Role Scope Mappings**. En Keycloak, un client scope que contiene
Role Scope Mappings solo se aplica si el usuario posee al menos uno de esos roles; cuando no hay
interseccion, Keycloak omite todos sus protocol mappers aunque el scope este asignado como
DEFAULT. El bootstrap elimina restricciones heredadas de roles legacy (`AD/DE/CD/DO/ES`) sin
eliminar los roles ni sus asignaciones a usuarios.

`idUsuario` es un atributo gestionado de User Profile, de valor unico y formato UUID. Puede ser
visto por `user` y `admin`, pero solo `admin` puede editarlo. Esto permite que el mapper lo lea
durante la emision del token sin permitir que un usuario se autoasigne otra identidad
institucional.

## 4. Roles institucionales

Client roles de `asistencias-api` — **nunca realm roles**:

```
ADMINISTRADOR
DECANO
COORDINADOR
DOCENTE
ESTUDIANTE
```

El backend lee roles exclusivamente desde `resource_access["asistencias-api"].roles`
(`KeycloakJwtClaimsExtractor`) y **deliberadamente ignora `realm_access`**. No usar codigos
cortos (`AD`/`DE`/`CD`/`DO`/`ES`) ni el prefijo `ROLE_` — ese prefijo es una convencion de Spring
Security que se aplica solo despues de convertir el JWT, nunca en Keycloak.

## 5. Password policy

Default institucional (no se debilita):

```
length(12) and digits(1) and lowerCase(1) and upperCase(1) and specialChars(1)
and notUsername and notEmail and passwordHistory(5) and hashAlgorithm(argon2)
```

`KC_PASSWORD_MIN_LENGTH` en `.env` controla solo la longitud minima — un desarrollador puede
bajarla (por ejemplo a `8`) para su ambiente local, pero nunca se hardcodea un valor mas debil
que 12 en los scripts.

## 6. Contrato de tokens

Access token final que el frontend obtiene y el backend valida:

```json
{
  "iss": "http://127.0.0.1:8081/realms/asistencias-uco",
  "aud": ["asistencias-api", "..."],
  "idUsuario": "<UUID de dbo.Usuario.id>",
  "resource_access": {
    "asistencias-api": {
      "roles": ["DOCENTE"]
    }
  }
}
```

## 7. Migracion de objetos legacy

Un Keycloak local existente puede tener:

- Roles cortos `AD`/`DE`/`CD`/`DO`/`ES` en `asistencias-api`.
- Un client `asistencias-frontend` (en vez del canonico `asistencias-uco-frontend`).

`bootstrap-keycloak.ps1` migra esto de forma **segura e idempotente**, nunca destructiva por
default:

- Crea los roles largos si faltan y migra los **role mappings** de cada usuario con un rol corto
  al rol largo equivalente. El rol corto **no se borra** salvo `KC_REMOVE_LEGACY_ROLES=true`
  (default `false`) — y solo despues de migrar los mappings.
- Si `asistencias-uco-frontend` no existe pero `asistencias-frontend` si, **renombra ese mismo
  client** (`clientId` → `asistencias-uco-frontend`), preservando su UUID interno y
  configuracion util. Si ambos existen, no borra ninguno automaticamente y reporta
  `LEGACY CLIENT DETECTED`; solo se borra el legacy si `KC_REMOVE_LEGACY_FRONTEND_CLIENT=true`
  (default `false`).

## 8. Secretos

Nunca se versionan: `infra/keycloak/.env`, passwords reales, access/refresh tokens, exports con
usuarios reales. `infra/keycloak/.env` esta en `.gitignore`; usar `.env.example` como plantilla.

`KEYCLOAK_ADMIN_CLIENT_SECRET` es obligatorio para `bootstrap-keycloak.ps1` — si falta, el script
falla con un mensaje claro y no imprime nunca el secreto, el admin token ni ningun password.

## 9. Mapeo de variables hacia el backend

Infra usa variables `KC_*` para el bootstrap; el backend (Spring Boot) usa nombres `KEYCLOAK_*`.
No debe haber valores contradictorios entre ambos:

| infra (`infra/keycloak/.env`) | backend (`application.yml` / entorno del backend) |
|---|---|
| `KC_REALM` | `KEYCLOAK_REALM` |
| `KC_API_CLIENT_ID` | `KEYCLOAK_API_CLIENT_ID` |
| `KC_BACKEND_ADMIN_CLIENT_ID` | `KEYCLOAK_ADMIN_CLIENT_ID` |
| `KEYCLOAK_ADMIN_CLIENT_SECRET` | `KEYCLOAK_ADMIN_CLIENT_SECRET` (mismo nombre, mismo valor) |
| — (fijo en el contrato) | `KEYCLOAK_USER_ID_ATTRIBUTE=idUsuario` |
| — | `KEYCLOAK_SERVER_URL=http://127.0.0.1:8081` |
| — | `KEYCLOAK_ISSUER_URI=http://127.0.0.1:8081/realms/asistencias-uco` |
| — | `KEYCLOAK_EXPECTED_AUDIENCE=asistencias-api` |
| — | `KEYCLOAK_USER_ID_CLAIM=idUsuario` |

Variables que usa el **frontend**: `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`
(=`asistencias-uco-frontend`). El frontend nunca necesita conocer `asistencias-backend-admin`,
su secret, ni nada de `realm-management`.

## 10. Servicios Docker

- Keycloak: http://localhost:8081
- Keycloak Management / Health: http://localhost:9001
- PostgreSQL: disponible unicamente dentro de la red Docker

### Configuracion inicial

Copiar `.env.example` a `.env` dentro de `infra/keycloak/` y reemplazar los valores `change-me`.

### Validar / descargar / iniciar

```bash
docker compose config
docker compose pull
docker compose up -d
docker compose ps
```

### Logs

```bash
docker compose logs -f keycloak
docker compose logs -f keycloak-db
```

### Health

http://localhost:9001/health/ready

### Detener

```bash
docker compose down
```

## 11. Que NO borrar / NO hacer

**Nunca ejecutar:**

```bash
docker compose down -v
```

Elimina el volumen `keycloak_db_data` con **todos** los realms, usuarios, clients, roles y
demas configuracion local almacenada en Keycloak/PostgreSQL. No es reversible.

Ademas: no borrar PostgreSQL manualmente, no recrear el volumen, no hacer merge de la rama de
bootstrap sin revision.

## 12. Como arrancar desde cero (instalacion limpia)

Con Postgres y Keycloak vacios, el propio arranque de Keycloak importa el realm base desde
`realm-import/asistencias-uco-realm.json` (flag `--import-realm` en `compose.yaml`). Esto deja
el realm, los 3 clients, los 5 roles institucionales, el client scope y sus mappers ya creados.

```bash
docker compose up -d
```

**Importante**: el import de arranque de Keycloak **omite el realm si ya existe** (no
reconcilia). Por eso, sin importar si el ambiente es nuevo o existente, el siguiente paso
(`bootstrap-keycloak.ps1`) es igual de necesario: fija el secret del service account, migra
legacy si aplica, asigna permisos minimos y deja el User Profile listo para `idUsuario`.

```powershell
cd infra/keycloak
.\scripts\bootstrap-keycloak.ps1
```

## 13. Como reconciliar un realm existente (el caso mas comun)

Si ya tienes un Keycloak local con realm, usuarios, client legacy del frontend y roles cortos
(el caso tipico antes de esta automatizacion): **no lo borres**. Simplemente corre el bootstrap
contra ese ambiente:

```powershell
cd infra/keycloak
.\scripts\bootstrap-keycloak.ps1
```

Es idempotente: la primera corrida crea/actualiza lo que falte; las siguientes solo verifican
(`[OK]` sin cambios). Correrlo varias veces seguidas es seguro.

## 14. Seed de usuarios E2E (opcional)

Para pruebas manuales de autenticacion/autorizacion, no forma parte del realm base:

```powershell
.\scripts\seed-e2e-users.ps1
```

Lee `E2E_DOCENTE_*`, `E2E_ADMIN_*`, `E2E_COORDINADOR_*` y `E2E_ESTUDIANTE_*` desde `.env`. Si falta el username o el `ID_USUARIO`, ese
usuario se omite (nunca se inventa un UUID institucional). Para `E2E_DOCENTE_ID_USUARIO` en
particular, debe ser un `dbo.Usuario.id` **real** en SQL Server si se quiere probar autorizacion
contextual (por ejemplo, asignaciones academicas del docente). No resetea la password de un
usuario ya existente salvo `KC_RESET_EXISTING_E2E_PASSWORDS=true`.

Despues de cada alta/reconciliacion, el seed vuelve a leer el usuario por Admin REST y falla si
`idUsuario` no quedo persistido exactamente. Tambien genera un token de ejemplo (sin imprimir
JWTs ni secretos) y verifica `idUsuario`, el audience `asistencias-api` y el rol institucional.

## 15. Obtener un token de prueba

```powershell
.\scripts\get-test-token.ps1 -Username docente.prueba
```

Pide la password de forma interactiva si no la encuentra en `.env`. Por default solo imprime
claims decodificados (`issuer`, `audience`, `idUsuario`, `resource_access.asistencias-api.roles`,
`exp`); usa `-ShowToken` para imprimir tambien el access token crudo. El refresh token nunca se
imprime. Ningun token se versiona.

## 16. Validar la configuracion

```powershell
.\scripts\validate-keycloak.ps1
```

Inspeccion de solo lectura: realm, clients, client scope, roles canonicos, mappers, default
scope, redirects/origins del frontend, permisos minimos del service account, y objetos legacy
encontrados. Verifica ademas que `asistencias-api-scope` no tenga Role Scope Mappings, que haya
exactamente un mapper efectivo de `idUsuario` con toda su configuracion canonica y que User
Profile mantenga `view=user,admin`, `edit=admin`, UUID y `multivalued=false`. Termina con exit
code distinto de cero si falta algo esencial.

Prueba rapida completa:

```powershell
.\scripts\bootstrap-keycloak.ps1
.\scripts\validate-keycloak.ps1
.\scripts\seed-e2e-users.ps1
.\scripts\get-test-token.ps1 -Username admin.prueba
.\scripts\get-test-token.ps1 -Username docente.prueba
```

Si un atributo aparece por Admin REST pero falta en el JWT, ejecutar
`.\scripts\diagnose-idusuario.ps1`. El diagnostico compara subject, atributo REST, persistencia
PostgreSQL cuando Docker esta disponible, claim y roles sin imprimir passwords, secrets ni
tokens crudos. Revisar primero que el validator reporte cero Role Scope Mappings en
`asistencias-api-scope`; asignarlo como DEFAULT no evita por si solo esa restriccion.

## 17. Exportar el realm para diagnostico

```powershell
.\scripts\export-realm.ps1
```

Genera un JSON en `infra/keycloak/generated/` (ignorado por Git) usando partial-export de la
Admin REST API — **sin usuarios ni credenciales**. No es un mecanismo de backup consistente de
un servidor activo; es solo una foto de configuracion para depurar manualmente.

## 18. Estructura

```
infra/keycloak/
├── .env.example
├── compose.yaml
├── README.md
│
├── realm-import/
│   └── asistencias-uco-realm.json   # base para instalacion limpia
│
├── generated/                        # exports temporales, ignorado por Git
│
└── scripts/
    ├── lib/
    │   └── KeycloakAdmin.psm1        # capa reutilizable de Admin REST API
    │
    ├── bootstrap-keycloak.ps1        # comando principal, idempotente
    ├── seed-e2e-users.ps1
    ├── get-test-token.ps1
    ├── validate-keycloak.ps1
    ├── diagnose-idusuario.ps1       # troubleshooting read-only
    └── export-realm.ps1
```

## 19. Source of truth

En orden de prioridad:

1. **Contrato logico**: backend (`application.yml`) + `docs/security/*`.
2. **Bootstrap de ambiente nuevo**: `realm-import/asistencias-uco-realm.json`.
3. **Reconciliacion de realm existente**: `bootstrap-keycloak.ps1`.
4. **Secretos locales**: `infra/keycloak/.env` (nunca versionado).

Nunca versionar: secretos, passwords reales, access/refresh tokens, exports con usuarios reales.
