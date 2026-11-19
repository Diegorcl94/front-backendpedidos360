import {
  BrowserCacheLocation,
  InteractionType,
  IPublicClientApplication,
  PublicClientApplication
} from '@azure/msal-browser';
import {
  MsalGuardConfiguration,
  MsalInterceptorConfiguration
} from '@azure/msal-angular';
import { environment } from '../environments/environment';

export const pedidosScopes = [
  `api://${environment.apiClientId}/Pedidos.Read`,
  `api://${environment.apiClientId}/Pedidos.Write`
];

export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.frontendClientId,
      authority: `https://login.microsoftonline.com/${environment.tenantId}`,
      redirectUri: window.location.origin,
      postLogoutRedirectUri: window.location.origin
    },
    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage
    }
  });
}

export function MSALGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: pedidosScopes
    },
    loginFailedRoute: '/'
  };
}

export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string>>();

  protectedResourceMap.set(
    `${environment.apiBaseUrl}/api/pedidos`,
    pedidosScopes
  );

  protectedResourceMap.set(
    `${environment.apiBaseUrl}/api/admin`,
    pedidosScopes
  );

  protectedResourceMap.set(
    `${environment.apiBaseUrl}/api/reportes/resumen`,
    pedidosScopes
  );

  protectedResourceMap.set(
    `${environment.apiBaseUrl}/api/auditoria/eventos`,
    pedidosScopes
  );

  protectedResourceMap.set(
    `${environment.apiBaseUrl}/api/admin/kafka/topics`,
    pedidosScopes
  );

  protectedResourceMap.set(
    `${environment.apiBaseUrl}/api/admin/kafka/topics/*`,
    pedidosScopes
  );

  protectedResourceMap.set(
    `${environment.apiBaseUrl}/api/admin/kafka/groups`,
    pedidosScopes
  );

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}
