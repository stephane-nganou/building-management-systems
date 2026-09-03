import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import Keycloak from 'keycloak-js';

import { SessionService } from './core/session';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (me()) {
      <div class="shell">
        <aside class="spine">
          <div class="spine-mark">
            <strong>Hausbuch</strong>
            <span>Property ledger</span>
          </div>

          <nav>
            @for (entry of entries(); track entry.path) {
              <a [routerLink]="entry.path" routerLinkActive="active">{{ entry.label }}</a>
            }
          </nav>

          <div class="spine-foot">
            <div class="who">{{ me()!.name }}</div>
            <div class="role">
              @if (session.owner()) {
                Owner
              } @else {
                Assisting {{ me()!.assistingFor.length }} owner(s)
              }
            </div>
            <button class="quiet" type="button" (click)="signOut()">Sign out</button>
          </div>
        </aside>

        <main class="main">
          <router-outlet />
        </main>
      </div>
    } @else {
      <router-outlet />
    }
  `,
})
export class App {
  private keycloak = inject(Keycloak);

  protected readonly session = inject(SessionService);
  protected readonly me = this.session.user;
  protected readonly entries = computed(() => (this.me() ? this.session.visibleEntries() : []));

  protected signOut(): void {
    void this.keycloak.logout({ redirectUri: window.location.origin });
  }
}
