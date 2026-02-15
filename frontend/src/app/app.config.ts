import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { AuthModule } from '@auth0/auth0-angular';

import { routes } from './app.routes';

AuthModule.forRoot({
  domain: 'dev-x2l40e775g2q2ot3.eu.auth0.com',
  clientId: '6RChZH73eEvLLEhwrk8DjTgDETGYTe4u',
  authorizationParams: {
    redirect_uri: window.location.origin,
    audience: 'https://cloudguard-api', 
  }
})

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes)
  ]
};