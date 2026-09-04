export const environment = {
  production: false,

  // Microsoft Entra ID
  tenantId: 'REEMPLAZAR_TENANT_ID',
  frontendClientId: 'REEMPLAZAR_FRONTEND_CLIENT_ID',
  apiClientId: 'REEMPLAZAR_BACKEND_CLIENT_ID',

  // Local: http://localhost:8080
  // AWS: reemplazar por la URL Invoke de API Gateway, sin slash final.
  apiBaseUrl: 'http://localhost:8080'
};
