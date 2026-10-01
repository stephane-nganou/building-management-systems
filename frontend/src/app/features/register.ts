import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthApi } from '../core/api';
import { AuthService } from '../core/auth';
import { TranslationService } from '../core/i18n';
import { acceptablePassword } from '../core/password-rule';
import { Gate } from '../shared/gate';
import { TranslatePipe } from '../shared/translate.pipe';

@Component({
  selector: 'bms-register',
  imports: [FormsModule, Gate, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <bms-gate>
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

        <div class="grid-2">
          <div class="field">
            <label for="firstName">{{ 'common.firstName' | t }}</label>
            <input id="firstName" name="firstName" [(ngModel)]="firstName" autocomplete="given-name" />
          </div>
          <div class="field">
            <label for="lastName">{{ 'common.lastName' | t }}</label>
            <input id="lastName" name="lastName" [(ngModel)]="lastName" autocomplete="family-name" />
          </div>
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
          <p class="hint">
            {{ 'register.passwordHint' | t }}
          </p>
        </div>

        @if (error()) {
          <p class="notice" role="alert">{{ error() }}</p>
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
    </bms-gate>
  `,
})
export class RegisterPage {
  private api = inject(AuthApi);
  private auth = inject(AuthService);
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
      acceptablePassword(this.password, this.email)
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
    this.auth.signIn('/');
  }
}
