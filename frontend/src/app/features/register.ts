import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import Keycloak from 'keycloak-js';

import { AuthApi } from '../core/api';
import { TranslationService } from '../core/i18n';
import { LanguageSwitcher } from '../shared/language-switcher';
import { TranslatePipe } from '../shared/translate.pipe';

@Component({
  selector: 'bms-register',
  imports: [FormsModule, LanguageSwitcher, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="gate">
      <section class="band gate-card">
        @if (registered()) {
          <div class="band-head">
            <div>
              <h1>{{ 'register.readyTitle' | t }}</h1>
              <p>{{ 'register.readyBody' | t: { email: email } }}</p>
            </div>
          </div>
          <button class="primary" type="button" (click)="signIn()">
            {{ 'register.signIn' | t }}
          </button>
        } @else {
          <div class="band-head">
            <div>
              <h1>{{ 'register.title' | t }}</h1>
              <p>{{ 'register.subtitle' | t }}</p>
            </div>
          </div>

          <div class="field">
            <label for="firstName">{{ 'common.firstName' | t }}</label>
            <input id="firstName" name="firstName" [(ngModel)]="firstName" autocomplete="given-name" />
          </div>

          <div class="field">
            <label for="lastName">{{ 'common.lastName' | t }}</label>
            <input id="lastName" name="lastName" [(ngModel)]="lastName" autocomplete="family-name" />
          </div>

          <div class="field">
            <label for="email">{{ 'common.email' | t }}</label>
            <input id="email" name="email" type="email" [(ngModel)]="email" autocomplete="email" />
          </div>

          <div class="field">
            <label for="password">{{ 'register.password' | t }}</label>
            <input
              id="password"
              name="password"
              type="password"
              [(ngModel)]="password"
              autocomplete="new-password"
            />
            <p class="muted" style="font-size:0.8125rem;margin:6px 0 0">
              {{ 'register.passwordHint' | t }}
            </p>
          </div>

          @if (error()) {
            <p class="notice">{{ error() }}</p>
          }

          <div class="gate-actions">
            <button class="primary" type="button" [disabled]="!complete() || saving()" (click)="submit()">
              {{ (saving() ? 'register.submitting' : 'register.submit') | t }}
            </button>
            <button class="quiet" type="button" (click)="signIn()">
              {{ 'register.haveAccount' | t }}
            </button>
          </div>
        }
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
export class RegisterPage {
  private api = inject(AuthApi);
  private keycloak = inject(Keycloak);
  private i18n = inject(TranslationService);

  protected readonly saving = signal(false);
  protected readonly registered = signal(false);
  protected readonly error = signal<string | null>(null);

  protected firstName = '';
  protected lastName = '';
  protected email = '';
  protected password = '';

  protected complete(): boolean {
    return (
      this.firstName.trim() !== '' &&
      this.lastName.trim() !== '' &&
      this.email.trim() !== '' &&
      this.password.length >= 8
    );
  }

  protected submit(): void {
    this.saving.set(true);
    this.error.set(null);
    this.api
      .register({
        email: this.email.trim(),
        firstName: this.firstName.trim(),
        lastName: this.lastName.trim(),
        password: this.password,
      })
      .subscribe({
        next: () => {
          this.password = '';
          this.saving.set(false);
          this.registered.set(true);
        },
        error: (response) => {
          this.saving.set(false);
          this.error.set(response?.error?.detail ?? this.i18n.translate('register.failed'));
        },
      });
  }

  protected signIn(): void {
    void this.keycloak.login({
      redirectUri: window.location.origin,
      locale: this.i18n.language(),
    });
  }
}
