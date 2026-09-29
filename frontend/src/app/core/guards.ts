import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';

import { AuthService, currentPath } from './auth';
import { Permission } from './models';
import { SessionService } from './session';

/**
 * Makes sure there is a session, and that the profile is loaded before any later
 * guard asks what this user may do.
 *
 * <p>Every guard injects what it needs before awaiting anything, because
 * `inject` only works while the injection context is still on the stack.
 */
export const authGuard: CanMatchFn = () => {
  const session = inject(SessionService);
  const auth = inject(AuthService);
  const router = inject(Router);
  const path = currentPath();
  return session.load().then(() => {
    if (!session.signedIn()) {
      // The profile request was refused, which the interceptor has already
      // turned into a sign in. Nothing here should match in the meantime.
      auth.signIn(path);
      return false;
    }
    if (session.mustChangePassword()) {
      return router.parseUrl('/password');
    }
    return true;
  });
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

/**
 * The change password screen, which only exists while it is required. Leaving
 * it reachable afterwards would offer a screen the sidebar never mentions.
 */
export const passwordChangeGuard: CanMatchFn = () => {
  const session = inject(SessionService);
  const auth = inject(AuthService);
  const path = currentPath();
  return session.load().then(() => {
    if (!session.signedIn()) {
      auth.signIn(path);
      return false;
    }
    return session.mustChangePassword();
  });
};
