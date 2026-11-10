import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideAppInitializer, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import {
  MSAL_INSTANCE,
  MSAL_GUARD_CONFIG,
  MSAL_INTERCEPTOR_CONFIG,
  MsalInterceptor,
  MsalGuard,
  MsalService,
  MsalBroadcastService
} from '@azure/msal-angular';

import { routes } from './app.routes';
import { msalInstanceFactory, msalGuardConfigFactory, msalInterceptorConfigFactory } from './msal-config';

// Inicializa MSAL y procesa la respuesta que llega de Azure después del login.
async function initializeMsal(): Promise<void> {
  const msal = inject(MsalService);
  await msal.instance.initialize();
  try {
    const result = await msal.instance.handleRedirectPromise();
    if (result?.account) {
      msal.instance.setActiveAccount(result.account);
    } else if (!msal.instance.getActiveAccount() && msal.instance.getAllAccounts().length > 0) {
      msal.instance.setActiveAccount(msal.instance.getAllAccounts()[0]);
    }
    if (result?.accessToken) {
      sessionStorage.setItem('debugToken', result.accessToken);
    }
  } catch (err) {
    console.error('Error procesando el login de Azure:', err);
  }
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),

    { provide: MSAL_INSTANCE, useFactory: msalInstanceFactory },
    { provide: MSAL_GUARD_CONFIG, useFactory: msalGuardConfigFactory },
    { provide: MSAL_INTERCEPTOR_CONFIG, useFactory: msalInterceptorConfigFactory },
    { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },

    MsalService,
    MsalGuard,
    MsalBroadcastService,

    provideAppInitializer(initializeMsal)
  ]
};