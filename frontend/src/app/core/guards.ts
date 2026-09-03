import { inject } from '@angular/core';
import { CanMatchFn } from '@angular/router';

import { Permission } from './models';
import { SessionService } from './session';

/**
 * Refuses the route when the permission is missing. Used as canMatch rather
 * than canActivate so the lazy chunk is never even fetched: an assistant gets
 * no hint that the screen exists.
 */
export const permissionGuard =
  (permission: Permission): CanMatchFn =>
  () =>
    inject(SessionService).can(permission);

/** Owners only, for screens that manage the account itself. */
export const ownerGuard: CanMatchFn = () => inject(SessionService).owner();
