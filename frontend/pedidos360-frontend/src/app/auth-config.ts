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

export const pedidosScope = `api://${environment.apiClientId}/Pedidos.Read`;

export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.frontendClientId,
      authority: `https://login.microsoftonline.com/${environment.tenantId}`,
      redirectUri: 'http://localhost:4200',
      postLogoutRedirectUri: 'http://localhost:4200'
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
      scopes: [pedidosScope]
    },
    loginFailedRoute: '/'
  };
}

export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string>>();

  protectedResourceMap.set(
    `${environment.apiBaseUrl}/api/pedidos`,
    [pedidosScope]
  );

  protectedResourceMap.set(
    `${environment.apiBaseUrl}/api/admin`,
    [pedidosScope]
  );

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}
