import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import Keycloak from 'keycloak-js';

import { AuthApi } from '../core/api';

@Component({
  selector: 'bms-register',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="gate">
      <section class="band gate-card">
        @if (registered()) {
          <div class="band-head">
            <div>
              <h1>Your account is ready</h1>
              <p>Sign in with {{ email }} and start by adding your first building.</p>
            </div>
          </div>
          <button class="primary" type="button" (click)="signIn()">Sign in</button>
        } @else {
          <div class="band-head">
            <div>
              <h1>Create your account</h1>
              <p>
                For landlords who manage their own buildings. Assistants do not sign up here; their
                owner creates them.
              </p>
            </div>
          </div>

          <div class="field">
            <label for="firstName">First name</label>
            <input id="firstName" name="firstName" [(ngModel)]="firstName" autocomplete="given-name" />
          </div>

          <div class="field">
            <label for="lastName">Last name</label>
            <input id="lastName" name="lastName" [(ngModel)]="lastName" autocomplete="family-name" />
          </div>

          <div class="field">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" [(ngModel)]="email" autocomplete="email" />
          </div>

          <div class="field">
            <label for="password">Password</label>
            <input
              id="password"
              name="password"
              type="password"
              [(ngModel)]="password"
              autocomplete="new-password"
            />
            <p class="muted" style="font-size:0.8125rem;margin:6px 0 0">At least 8 characters.</p>
          </div>

          @if (error()) {
            <p class="notice">{{ error() }}</p>
          }

          <div class="gate-actions">
            <button class="primary" type="button" [disabled]="!complete() || saving()" (click)="submit()">
              {{ saving() ? 'Creating' : 'Create account' }}
            </button>
            <button class="quiet" type="button" (click)="signIn()">I already have an account</button>
          </div>
        }
      </section>
    </div>
  `,
  styles: `
    .gate {
      display: grid;
      place-items: center;
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
          this.error.set(response?.error?.detail ?? 'That account could not be created.');
        },
      });
  }

  protected signIn(): void {
    void this.keycloak.login({ redirectUri: window.location.origin });
  }
}
