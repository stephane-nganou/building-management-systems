import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { TranslationService } from './i18n';

/**
 * Signing in and out, as far as this application is concerned.
 *
 * <p>There is no identity provider here, no token and no library. The backend
 * runs the whole flow: the browser is sent to `/api/auth/login/keycloak`, comes
 * back holding a session cookie it cannot read, and from then on simply calls
 * the API. Nothing a script on this page can reach would be worth stealing.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private i18n = inject(TranslationService);

  /**
   * A refused request and the guard that follows it both ask to sign in, and a
   * navigation already under way must not be replaced by a second one.
   */
  private leaving = false;

  /**
   * Leaves the application for the backend, which sends the browser on to
   * wherever it keeps accounts, and asks to be returned to this page.
   */
  signIn(returnPath = currentPath()): void {
    if (this.leaving) {
      return;
    }
    this.leaving = true;
    const language = this.i18n.language();
    window.location.assign(
      `/api/auth/login/keycloak?redirect=${encodeURIComponent(returnPath)}&ui_locales=${language}`,
    );
  }

  /** Ends the session here and at the identity provider, in one navigation. */
  signOut(): void {
    this.leaving = true;
    window.location.assign('/api/auth/logout');
  }
}

/** The page the browser is on, without the fragment, which no redirect needs. */
export function currentPath(): string {
  return window.location.pathname + window.location.search;
}

/**
 * Turns the backend's "you are not signed in" into a sign in.
 *
 * <p>The API answers 401 rather than redirecting, because a redirect to another
 * host is unreadable to a background request: the browser would follow it and
 * hand back an opaque failure. Meeting it here means any screen can simply ask
 * for its data and let this take over if there is no session.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        auth.signIn();
      }
      return throwError(() => error);
    }),
  );
};
