import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';
import Keycloak from 'keycloak-js';

import { MeApi } from './core/api';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="shell">
      <aside class="spine">
        <div class="spine-mark">
          <strong>Hausbuch</strong>
          <span>Property ledger</span>
        </div>

        <nav>
          <a routerLink="/dashboard" routerLinkActive="active">Overview</a>
          <a routerLink="/buildings" routerLinkActive="active">Buildings</a>
          <a routerLink="/apartments" routerLinkActive="active">Apartments</a>
          <a routerLink="/tenants" routerLinkActive="active">Tenants</a>
          <a routerLink="/expenses" routerLinkActive="active">Expenses</a>
          <a routerLink="/invoices" routerLinkActive="active">Invoices</a>
          <a routerLink="/reports" routerLinkActive="active">Profit and loss</a>
          <a routerLink="/assistants" routerLinkActive="active">Assistants</a>
        </nav>

        <div class="spine-foot">
          @if (me.hasValue()) {
            <div class="who">{{ me.value()!.name }}</div>
            <div class="role">
              @if (me.value()!.assistingFor.length) {
                Assisting {{ me.value()!.assistingFor.length }} owner(s)
              } @else {
                Owner
              }
            </div>
          }
          <button class="quiet" type="button" (click)="signOut()">Sign out</button>
        </div>
      </aside>

      <main class="main">
        <router-outlet />
      </main>
    </div>
  `,
})
export class App {
  private keycloak = inject(Keycloak);
  private meApi = inject(MeApi);

  protected readonly me = rxResource({ stream: () => this.meApi.get() });

  protected signOut(): void {
    void this.keycloak.logout({ redirectUri: window.location.origin });
  }
}
