import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';

import { AuthService } from './core/auth';
import { SessionService } from './core/session';
import { Brand } from './shared/brand';
import { ConfirmHost } from './shared/confirm';
import { Icon } from './shared/icon';
import { LanguageSwitcher } from './shared/language-switcher';
import { ThemeSwitcher } from './shared/theme-switcher';
import { DayPipe } from './shared/money.pipe';
import { Toasts } from './shared/toasts';
import { TranslatePipe } from './shared/translate.pipe';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    Brand,
    ConfirmHost,
    DayPipe,
    Icon,
    LanguageSwitcher,
    ThemeSwitcher,
    Toasts,
    TranslatePipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'menuOpen.set(false)' },
  template: `
    @if (me() && !session.mustChangePassword() && !session.suspended()) {
      <div class="shell" [class.drawer-open]="menuOpen()">
        <header class="topbar">
          <button
            class="icon"
            type="button"
            [attr.aria-label]="'app.openMenu' | t"
            aria-controls="spine"
            [attr.aria-expanded]="menuOpen()"
            (click)="menuOpen.set(true)"
          >
            <bms-icon name="menu" [size]="22" />
          </button>
          <bms-brand />
        </header>

        <aside class="spine" id="spine">
          <bms-brand />

          <nav [attr.aria-label]="'app.navigation' | t">
            @for (entry of entries(); track entry.path) {
              <a [routerLink]="entry.path" routerLinkActive="active" ariaCurrentWhenActive="page">
                <bms-icon [name]="entry.icon" />
                {{ entry.label | t }}
                <span class="lamp" aria-hidden="true"></span>
              </a>
            }
          </nav>

          <div class="spine-foot">
            <div class="person">
              <span class="initials" aria-hidden="true">{{ initials() }}</span>
              <div>
                <div class="who">{{ me()!.name }}</div>
                <div class="role">
                  @if (session.admin()) {
                    {{ 'app.role.admin' | t }}
                  } @else if (session.owner()) {
                    {{ 'app.role.owner' | t }}
                  } @else {
                    {{ 'app.role.assisting' | t: { count: me()!.assistingFor.length } }}
                  }
                </div>
              </div>
            </div>
            <bms-theme-switcher />
            <div class="foot-row">
              <bms-language-switcher />
              <button class="quiet" type="button" (click)="signOut()">
                <bms-icon name="signOut" [size]="16" />
                {{ 'app.signOut' | t }}
              </button>
            </div>
          </div>
        </aside>

        <div class="nav-scrim" (click)="menuOpen.set(false)"></div>

        <main class="main">
          @if (session.ownSubscriptionEnded() !== null) {
            <p class="notice read-only" role="status">
              <bms-icon name="alert" />
              {{ 'readOnly.own' | t: { date: (session.ownSubscriptionEnded() | day) } }}
            </p>
          } @else if (session.readOnlyOwners().length > 0) {
            <p class="notice read-only" role="status">
              <bms-icon name="alert" />
              {{ 'readOnly.assisting' | t: { names: session.readOnlyOwners().join(', ') } }}
            </p>
          }
          @if (session.suspendedOwners().length > 0) {
            <p class="notice read-only" role="status">
              <bms-icon name="alert" />
              {{ 'suspended.assisting' | t: { names: session.suspendedOwners().join(', ') } }}
            </p>
          }
          <router-outlet />
        </main>
      </div>
    } @else {
      <router-outlet />
    }
    <bms-confirm />
    <bms-toasts />
  `,
})
export class App {
  private auth = inject(AuthService);

  protected readonly session = inject(SessionService);
  protected readonly me = this.session.user;
  protected readonly entries = computed(() => (this.me() ? this.session.visibleEntries() : []));
  protected readonly menuOpen = signal(false);

  protected readonly initials = computed(() =>
    (this.me()?.name ?? '')
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0].toUpperCase())
      .join(''),
  );

  constructor() {
    // The drawer closes behind whichever link was chosen from it.
    inject(Router)
      .events.pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.menuOpen.set(false));
  }

  protected signOut(): void {
    this.auth.signOut();
  }
}
