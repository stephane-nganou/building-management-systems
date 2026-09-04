import { inject } from '@angular/core';
import { CanMatchFn } from '@angular/router';
import Keycloak from 'keycloak-js';

import { Permission } from './models';
import { SessionService } from './session';

/**
 * The page to come back to after signing in. The fragment is dropped on
 * purpose: a check-sso that finds no session leaves `#error=login_required`
 * behind, and Keycloak rejects a redirect_uri carrying it.
 */
function currentPage(): string {
  return window.location.origin + window.location.pathname + window.location.search;
}

/**
 * Sends anyone without a session to Keycloak, and makes sure the profile is
 * loaded before any later guard asks what they may do.
 *
 * <p>Every guard injects what it needs before awaiting anything, because
 * `inject` only works while the injection context is still on the stack.
 */
export const authGuard: CanMatchFn = () => {
  const keycloak = inject(Keycloak);
  const session = inject(SessionService);
  if (!keycloak.authenticated) {
    void keycloak.login({ redirectUri: currentPage() });
    return false;
  }
  return session.load().then(() => true);
};

/**
 * Refuses the route when the permission is missing. Used as canMatch rather
 * than canActivate so the lazy chunk is never even fetched: an assistant gets
 * no hint that the screen exists.
 */
export const permissionGuard =
  (permission: Permission): CanMatchFn =>
  () => {
    const session = inject(SessionService);
    return session.load().then(() => session.can(permission));
  };

/** Owners only, for screens that manage the account itself. */
export const ownerGuard: CanMatchFn = () => {
  const session = inject(SessionService);
  return session.load().then(() => session.owner());
};
