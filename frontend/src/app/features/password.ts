import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthApi } from '../core/api';
import { AuthService } from '../core/auth';
import { TranslationService } from '../core/i18n';
import { SessionService } from '../core/session';
import { LanguageSwitcher } from '../shared/language-switcher';
import { TranslatePipe } from '../shared/translate.pipe';

/**
 * Where an assistant lands the first time they sign in, holding a password
 * their owner read out to them. Nothing else in the application is reachable
 * until they have chosen one of their own.
 */
@Component({
  selector: 'bms-password',
  imports: [FormsModule, LanguageSwitcher, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="gate">
      <section class="band gate-card">
        <div class="band-head">
          <div>
            <h1>{{ 'password.title' | t }}</h1>
            <p>{{ 'password.subtitle' | t }}</p>
          </div>
        </div>

        <div class="field">
          <label for="newPassword">{{ 'password.new' | t }}</label>
          <input
            id="newPassword"
            name="newPassword"
            type="password"
            [(ngModel)]="newPassword"
            autocomplete="new-password"
          />
          <p class="muted" style="font-size:0.8125rem;margin:6px 0 0">
            {{ 'password.hint' | t }}
          </p>
        </div>

        <div class="field">
          <label for="confirmPassword">{{ 'password.confirm' | t }}</label>
          <input
            id="confirmPassword"
            name="confirmPassword"
            type="password"
            [(ngModel)]="confirmPassword"
            autocomplete="new-password"
          />
        </div>

        @if (error()) {
          <p class="notice">{{ error() }}</p>
        }

        <div class="gate-actions">
          <button class="primary" type="button" [disabled]="!complete() || saving()" (click)="submit()">
            {{ (saving() ? 'password.saving' : 'password.submit') | t }}
          </button>
          <button class="quiet" type="button" (click)="signOut()">{{ 'app.signOut' | t }}</button>
        </div>
      </section>

      <bms-language-switcher />
    </div>
  `,
  styles: `
    .gate {
      display: grid;
      place-items: center;
      align-content: center;
      gap: 18px;
      min-height: 100vh;
      padding: 32px 20px;
    }

    .gate-card {
      width: min(440px, 100%);
    }

    .gate-actions {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-top: 20px;
    }
  `,
})
export class PasswordPage {
  private api = inject(AuthApi);
  private auth = inject(AuthService);
  private session = inject(SessionService);
  private router = inject(Router);
  private i18n = inject(TranslationService);

  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected newPassword = '';
  protected confirmPassword = '';

  protected complete(): boolean {
    return this.newPassword.length >= 8 && this.newPassword === this.confirmPassword;
  }

  protected submit(): void {
    this.saving.set(true);
    this.error.set(null);
    this.api.changePassword(this.newPassword).subscribe({
      next: () => {
        this.newPassword = '';
        this.confirmPassword = '';
        this.saving.set(false);
        // The obligation is on the profile, so it has to be read again before
        // the guards will let any other screen match.
        void this.session.reload().then(() => this.router.navigateByUrl(this.session.landingRoute()));
      },
      error: (response) => {
        this.saving.set(false);
        this.error.set(response?.error?.detail ?? this.i18n.translate('password.failed'));
      },
    });
  }

  protected signOut(): void {
    this.auth.signOut();
  }
}
