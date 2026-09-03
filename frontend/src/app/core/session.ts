import { Injectable, computed, inject, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { catchError, of, tap } from 'rxjs';

import { MeApi } from './api';
import { Me, Permission } from './models';
import { NAV_ENTRIES, NavEntry } from './navigation';

/** The only pages that open without an account. */
const PUBLIC_PATHS = ['/register'];

/**
 * Who is signed in and what they may do, resolved once before the app starts so
 * route guards can answer without waiting on a request.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private keycloak = inject(Keycloak);
  private meApi = inject(MeApi);
  private me = signal<Me | null>(null);

  readonly user = this.me.asReadonly();
  readonly owner = computed(() => this.me()?.owner ?? false);

  /**
   * Resolves once the profile is known. Signing in happens here rather than in
   * a route guard, because the router forbids a guard on a redirecting route
   * and every screen but registration needs an account anyway.
   */
  load(): Promise<unknown> {
    if (!this.keycloak.authenticated) {
      return PUBLIC_PATHS.includes(window.location.pathname)
        ? Promise.resolve(null)
        : this.keycloak.login({ redirectUri: window.location.href });
    }
    return new Promise((resolve) => {
      this.meApi
        .get()
        .pipe(
          tap((me) => this.me.set(me)),
          catchError(() => of(null)),
        )
        .subscribe(() => resolve(null));
    });
  }

  can(permission: Permission): boolean {
    return this.me()?.permissions.includes(permission) ?? false;
  }

  allows(entry: NavEntry): boolean {
    if (entry.ownerOnly) {
      return this.owner();
    }
    return entry.permission === undefined || this.can(entry.permission);
  }

  visibleEntries(): NavEntry[] {
    return NAV_ENTRIES.filter((entry) => this.allows(entry));
  }

  /** Where to send someone who asked for a page they may not see. */
  landingRoute(): string {
    return this.visibleEntries()[0]?.path ?? '/no-access';
  }
}
