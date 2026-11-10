import { PublicClientApplication, IPublicClientApplication, InteractionType } from '@azure/msal-browser';
import { MsalGuardConfiguration, MsalInterceptorConfiguration } from '@azure/msal-angular';
import { environment } from '../environments/environment';

export function msalInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.azure.clientId,
      authority: environment.azure.authority,
      redirectUri: environment.azure.redirectUri,
      postLogoutRedirectUri: environment.azure.redirectUri
    },
    cache: {
      cacheLocation: 'localStorage'
    }
  });
}

// Configuración del MsalGuard: si no hay sesión, redirige al login de Microsoft.
export function msalGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: ['openid', 'profile']
    },
    loginFailedRoute: '/login'
  };
}

export function msalInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const base = environment.apiBaseUrl.replace(/\/$/, '');
  const protectedResourceMap = new Map<string, Array<string>>([
    [`${base}/*`, environment.azure.protectedResourceScopes]
  ]);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}