import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  AutoRefreshTokenService,
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  IncludeBearerTokenCondition,
  UserActivityService,
  createInterceptorCondition,
  includeBearerTokenInterceptor,
  provideKeycloak,
  withAutoRefreshToken,
} from 'keycloak-angular';

import { routes } from './app.routes';
import { config } from './core/config';
import { acceptLanguageInterceptor } from './core/i18n';

/** Attach the access token to our own API only, never to third party hosts. */
const apiCondition = createInterceptorCondition<IncludeBearerTokenCondition>({
  urlPattern: new RegExp(`^${config.apiUrl.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/api(/.*)?$`, 'i'),
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideKeycloak({
      config: {
        url: config.keycloakUrl,
        realm: config.realm,
        clientId: config.clientId,
      },
      initOptions: {
        // Not login-required: the registration page has to open without an account.
        onLoad: 'check-sso',
        checkLoginIframe: false,
        pkceMethod: 'S256',
      },
      features: [withAutoRefreshToken({ onInactivityTimeout: 'logout', sessionTimeout: 1_800_000 })],
      providers: [
        // Both are required by the withAutoRefreshToken feature above.
        AutoRefreshTokenService,
        UserActivityService,
        {
          provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
          useValue: [apiCondition],
        },
      ],
    }),
    provideHttpClient(withInterceptors([includeBearerTokenInterceptor, acceptLanguageInterceptor])),
    provideRouter(routes, withComponentInputBinding()),
  ],
};
