# Pedidos360 Frontend

Angular + MSAL + Microsoft Entra ID.

## Configurar

Edita `src/environments/environment.ts` con:

- Tenant ID
- Client ID del frontend SPA
- Client ID del backend/API

## Ejecutar

```bash
npm install
npm start
```

Abrir `http://localhost:4200`.

## Seguridad

- `/pedidos` usa `MsalGuard`.
- `MsalInterceptor` solicita el scope `api://<API_CLIENT_ID>/Pedidos.Read`.
- El Access Token se adjunta automáticamente al request hacia `/api/pedidos`.

Para AWS cambia `apiBaseUrl` por la URL Invoke del API Gateway.
