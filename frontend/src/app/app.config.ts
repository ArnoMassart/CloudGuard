import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAuth0 } from '@auth0/auth0-angular';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideAuth0({
      domain: 'dev-x2l40e775g2q2ot3.eu.auth0.com',
      clientId: '6RChZH73eEvLLEhwrk8DjTgDETGYTe4u',
      authorizationParams: {
        redirect_uri: window.location.origin,
        audience: 'https://cloudguard-api',
      },
    }),
  ],
};