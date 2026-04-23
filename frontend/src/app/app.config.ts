import { ApplicationConfig, provideBrowserGlobalErrorListeners, isDevMode } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAuth0, authHttpInterceptorFn } from '@auth0/auth0-angular';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { errorInterceptor } from './interceptor/error.interceptor';
import { TranslocoHttpLoader } from './transloco-loader';
import { provideTransloco } from '@jsverse/transloco';
import { languageInterceptor } from './interceptor/language.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([errorInterceptor, languageInterceptor, authHttpInterceptorFn])
    ),
    provideAuth0({
      domain: 'dev-x2l40e775g2q2ot3.eu.auth0.com',
      clientId: '6RChZH73eEvLLEhwrk8DjTgDETGYTe4u',
      authorizationParams: {
        redirect_uri: globalThis.location.origin + '/callback',
        audience: 'https://cloudguard-api',
      },
      cacheLocation: 'memory',
      skipRedirectCallback: globalThis.location.pathname.includes('/callback'),
      httpInterceptor: {
        allowedList: [
          'http://localhost:8080/api/*', // Voor als je lokaal ontwikkelt
          'https://cloudguard.cloudmen.net/api/*', // Voor je VM in Google Cloud
        ],
      },
    }),
    provideTransloco({
      config: {
        availableLangs: ['nl', 'en'],
        defaultLang: 'nl',
        fallbackLang: 'nl',
        // Remove this option if your application doesn't support changing language in runtime.
        reRenderOnLangChange: true,
        prodMode: !isDevMode(),
      },
      loader: TranslocoHttpLoader,
    }),
  ],
};
