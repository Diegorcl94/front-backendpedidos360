# Configuración mínima de Microsoft Entra ID

Para que el frontend obtenga un **Access Token para tu propia API** se recomienda usar dos registros de aplicación.

## 1. Registrar API / Backend

En Microsoft Entra ID -> App registrations -> New registration:

Nombre sugerido: `Pedidos360-API`

Guarda:

- Application (client) ID -> será `BACKEND_CLIENT_ID`.
- Directory (tenant) ID -> será `TENANT_ID`.

Luego entra a **Expose an API**:

1. Define Application ID URI como `api://<BACKEND_CLIENT_ID>`.
2. Crea un scope:
   - Scope name: `Pedidos.Read`
   - Who can consent: Admins and users
   - State: Enabled

El scope final quedará parecido a:

`api://BACKEND_CLIENT_ID/Pedidos.Read`

## 2. Registrar Frontend Angular

Crea otro registro:

Nombre sugerido: `Pedidos360-Frontend`

En Authentication:

- Add a platform -> Single-page application (SPA)
- Redirect URI: `http://localhost:4200`
- Logout URL / post logout: `http://localhost:4200`

Guarda su Application (client) ID como `FRONTEND_CLIENT_ID`.

## 3. Dar permiso del frontend a la API

En `Pedidos360-Frontend`:

API permissions -> Add a permission -> My APIs -> `Pedidos360-API` -> Delegated permissions -> `Pedidos.Read`.

Si el tenant lo requiere, concede consentimiento.

## 4. Configurar frontend

Edita:

`frontend/pedidos360-frontend/src/environments/environment.ts`

Reemplaza:

- `REEMPLAZAR_TENANT_ID`
- `REEMPLAZAR_FRONTEND_CLIENT_ID`
- `REEMPLAZAR_BACKEND_CLIENT_ID`

## 5. Configurar backend

En Windows CMD:

```bat
set AZURE_TENANT_ID=TU_TENANT_ID
set AZURE_API_CLIENT_ID=TU_BACKEND_CLIENT_ID
mvn spring-boot:run
```

En PowerShell:

```powershell
$env:AZURE_TENANT_ID="TU_TENANT_ID"
$env:AZURE_API_CLIENT_ID="TU_BACKEND_CLIENT_ID"
mvn spring-boot:run
```

El backend valida:

- firma con las claves públicas JWKS de Microsoft;
- `iss` (issuer);
- `aud` (audience);
- vigencia del JWT (`exp` / `nbf`).

## 6. Prueba esperada

- `GET http://localhost:8080/api/health` -> 200 sin autenticación.
- `GET http://localhost:8080/api/pedidos` sin token -> 401.
- Angular autenticado -> MSAL agrega `Authorization: Bearer <access_token>` -> backend devuelve los pedidos.

