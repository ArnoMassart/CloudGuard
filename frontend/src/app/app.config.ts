import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAuth0 } from '@auth0/auth0-angular';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { errorInterceptor } from './interceptor/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([errorInterceptor])),
    provideAuth0({
      domain: 'dev-x2l40e775g2q2ot3.eu.auth0.com',
      clientId: '6RChZH73eEvLLEhwrk8DjTgDETGYTe4u',
      authorizationParams: {
        redirect_uri: globalThis.location.origin + '/callback',
        audience: 'https://cloudguard-api',
      },
      cacheLocation: 'memory',
      skipRedirectCallback: globalThis.location.pathname.includes('/callback'),
    }),
  ],
};
