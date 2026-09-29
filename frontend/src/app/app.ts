import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth';
import { SessionService } from './core/session';
import { LanguageSwitcher } from './shared/language-switcher';
import { TranslatePipe } from './shared/translate.pipe';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LanguageSwitcher, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (me() && !session.mustChangePassword()) {
      <div class="shell">
        <aside class="spine">
          <div class="spine-mark">
            <strong>Hausbuch</strong>
            <span>{{ 'app.tagline' | t }}</span>
          </div>

          <nav>
            @for (entry of entries(); track entry.path) {
              <a [routerLink]="entry.path" routerLinkActive="active">{{ entry.label | t }}</a>
            }
          </nav>

          <div class="spine-foot">
            <div class="who">{{ me()!.name }}</div>
            <div class="role">
              @if (session.owner()) {
                {{ 'app.role.owner' | t }}
              } @else {
                {{ 'app.role.assisting' | t: { count: me()!.assistingFor.length } }}
              }
            </div>
            <bms-language-switcher />
            <button class="quiet" type="button" (click)="signOut()">{{ 'app.signOut' | t }}</button>
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
  private auth = inject(AuthService);

  protected readonly session = inject(SessionService);
  protected readonly me = this.session.user;
  protected readonly entries = computed(() => (this.me() ? this.session.visibleEntries() : []));

  protected signOut(): void {
    this.auth.signOut();
  }
}
