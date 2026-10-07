import { ApplicationConfig, provideBrowserGlobalErrorListeners, APP_INITIALIZER } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { IPublicClientApplication } from '@azure/msal-browser';
import {
  MSAL_INSTANCE,
  MSAL_INTERCEPTOR_CONFIG,
  MsalInterceptor,
  MsalGuard,
  MsalService,
  MsalBroadcastService
} from '@azure/msal-angular';

import { routes } from './app.routes';
import { msalInstanceFactory, msalInterceptorConfigFactory } from './msal-config';

export function initializeMsal(instance: IPublicClientApplication) {
  return () =>
    instance.initialize().then(() =>
      instance.handleRedirectPromise()
        .then((result) => {
          if (result?.accessToken) {
            sessionStorage.setItem('debugToken', result.accessToken);
          }
        })
        .catch((err) => console.error(err))
    );
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),

    { provide: MSAL_INSTANCE, useFactory: msalInstanceFactory },
    { provide: MSAL_INTERCEPTOR_CONFIG, useFactory: msalInterceptorConfigFactory },
    { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },

    {
      provide: APP_INITIALIZER,
      useFactory: initializeMsal,
      deps: [MSAL_INSTANCE],
      multi: true
    },

    MsalGuard,
    MsalService,
    MsalBroadcastService
  ]
};
