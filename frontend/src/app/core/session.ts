import { Injectable, computed, inject, signal } from '@angular/core';

import { MeApi } from './api';
import { Me, Permission } from './models';
import { NAV_ENTRIES, NavEntry } from './navigation';

/**
 * Who is signed in and what they may do.
 *
 * <p>There is one way to find out, and it is to ask the backend. A session is
 * a cookie this application cannot read, so the profile is the only evidence
 * the page has that anybody is signed in at all: a profile means yes, a refusal
 * means no.
 *
 * <p>The profile is fetched from the route guards rather than an app
 * initializer, because Angular starts every initializer at once without waiting
 * for the one before, and a guard runs at a point where the answer can be
 * awaited.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private meApi = inject(MeApi);
  private me = signal<Me | null>(null);
  private loading: Promise<unknown> | null = null;

  readonly user = this.me.asReadonly();
  readonly owner = computed(() => this.me()?.owner ?? false);
  readonly signedIn = computed(() => this.me() !== null);

  /** True while this account is still using a password somebody else chose. */
  readonly mustChangePassword = computed(() => this.me()?.mustChangePassword ?? false);

  /** Fetches the profile the first time it is asked for, then hands out the same result. */
  load(): Promise<unknown> {
    return (this.loading ??= this.fetch());
  }

  /** Forgets the profile, so the next guard reads it again. */
  reload(): Promise<unknown> {
    this.loading = null;
    return this.load();
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
    return new Promise((resolve) => {
      this.meApi.get().subscribe({
        next: (me) => {
          this.me.set(me);
          resolve(null);
        },
        // A failure is not remembered: forgetting it lets the next guard try
        // again, rather than stranding the user on an empty page until they
        // reload because the backend was a moment slower than the browser. A
        // 401 has already sent the browser off to sign in by this point.
        error: () => {
          this.loading = null;
          resolve(null);
        },
      });
    });
  }
}
