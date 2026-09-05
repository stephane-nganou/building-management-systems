import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth';
import { acceptLanguageInterceptor } from './core/i18n';

/**
 * The application knows one host: its own. The API is served from it, so every
 * request is same origin, the session cookie travels on its own, and Angular's
 * own cross site request forgery protection reads the token cookie the backend
 * sets and returns it in a header. There is no identity provider to configure
 * here, because this application never talks to one.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([authInterceptor, acceptLanguageInterceptor])),
    provideRouter(routes, withComponentInputBinding()),
  ],
};
