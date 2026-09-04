import { Injectable, computed, inject, signal } from '@angular/core';
import Keycloak from 'keycloak-js';

import { MeApi } from './api';
import { Me, Permission } from './models';
import { NAV_ENTRIES, NavEntry } from './navigation';

/**
 * Who is signed in and what they may do.
 *
 * <p>The profile is fetched from the route guards rather than an app
 * initializer. Angular starts every initializer at once without waiting for the
 * one before, so an initializer here would run while Keycloak was still working
 * out whether there is a session, read `authenticated` as false and bounce the
 * browser back to the login page for ever. By the time a guard runs, Keycloak
 * has finished.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private keycloak = inject(Keycloak);
  private meApi = inject(MeApi);
  private me = signal<Me | null>(null);
  private loading: Promise<unknown> | null = null;

  readonly user = this.me.asReadonly();
  readonly owner = computed(() => this.me()?.owner ?? false);

  /** Fetches the profile the first time it is asked for, then hands out the same result. */
  load(): Promise<unknown> {
    return (this.loading ??= this.fetch());
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

  private fetch(): Promise<unknown> {
    if (!this.keycloak.authenticated) {
      return Promise.resolve(null);
    }
    return new Promise((resolve) => {
      this.meApi.get().subscribe({
        next: (me) => {
          this.me.set(me);
          resolve(null);
        },
        // A failure is not remembered: forgetting it lets the next guard try
        // again, rather than stranding the user on the empty page until they
        // reload because the backend was a moment slower than the browser.
        error: () => {
          this.loading = null;
          resolve(null);
        },
      });
    });
  }
}
