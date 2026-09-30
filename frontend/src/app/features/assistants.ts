import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { AssistantsApi } from '../core/api';
import { TranslationService } from '../core/i18n';
import { Assistant, Permission } from '../core/models';
import { ConfirmService } from '../shared/confirm';
import { Dialog } from '../shared/dialog';
import { Facade } from '../shared/facade';
import { Icon } from '../shared/icon';
import { LabelPipe } from '../shared/money.pipe';
import { ToastService } from '../shared/toasts';
import { TranslatePipe } from '../shared/translate.pipe';

@Component({
  selector: 'bms-assistants',
  imports: [FormsModule, Dialog, Facade, Icon, LabelPipe, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'assistants.title' | t }}</h1>
          <p>{{ 'assistants.subtitle' | t }}</p>
        </div>
        <button class="primary" type="button" (click)="startGrant()">
          <bms-icon name="plus" />
          {{ 'assistants.add' | t }}
        </button>
      </div>

      @if (assistants.isLoading()) {
        <p class="loading">{{ 'assistants.loading' | t }}</p>
      } @else if (assistants.hasValue() && assistants.value()!.length === 0) {
        <div class="empty">
          <bms-facade [units]="[]" [scale]="2" />
          <div>
            <h3>{{ 'assistants.emptyTitle' | t }}</h3>
            <p>{{ 'assistants.emptyBody' | t }}</p>
            <button class="primary" type="button" (click)="startGrant()">
              <bms-icon name="plus" />
              {{ 'assistants.add' | t }}
            </button>
          </div>
        </div>
      } @else if (assistants.hasValue()) {
        <div class="sheet-frame">
          <table class="sheet">
            <thead>
              <tr>
                <th>{{ 'common.name' | t }}</th>
                <th>{{ 'common.email' | t }}</th>
                <th>{{ 'assistants.canDo' | t }}</th>
                <th><span class="visually-hidden">{{ 'common.actions' | t }}</span></th>
              </tr>
            </thead>
            <tbody>
              @for (assistant of assistants.value(); track assistant.id) {
                <tr>
                  <td class="strong">{{ assistant.name }}</td>
                  <td class="muted">{{ assistant.email }}</td>
                  <td>
                    <span class="tags">
                      @for (permission of assistant.permissions; track permission) {
                        <span class="tag">{{ permission | label: 'permission' }}</span>
                      }
                    </span>
                    @if (assistant.permissions.length === 0) {
                      <span class="muted">{{ 'assistants.nothingYet' | t }}</span>
                    }
                  </td>
                  <td class="actions-cell">
                    <span class="row-actions">
                      <button class="quiet" type="button" (click)="resetPassword(assistant)">
                        <bms-icon name="key" [size]="16" />
                        {{ 'assistants.newPassword' | t }}
                      </button>
                      <button
                        class="icon"
                        type="button"
                        [attr.aria-label]="'assistants.change' | t"
                        [title]="'assistants.change' | t"
                        (click)="startEdit(assistant)"
                      >
                        <bms-icon name="edit" />
                      </button>
                      <button
                        class="icon danger"
                        type="button"
                        [attr.aria-label]="'assistants.remove' | t"
                        [title]="'assistants.remove' | t"
                        (click)="revoke(assistant)"
                      >
                        <bms-icon name="trash" />
                      </button>
                    </span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      @if (error()) {
        <p class="notice" role="alert"><bms-icon name="alert" />{{ error() }}</p>
      }
    </section>

    @if (issued(); as credentials) {
      <bms-dialog [heading]="'assistants.handOver' | t" (closed)="dismissCredentials()">
        <p>{{ 'assistants.handOverBody' | t: { name: credentials.name } }}</p>
        <div class="field">
          <label for="issuedEmail">{{ 'common.email' | t }}</label>
          <input id="issuedEmail" [value]="credentials.email" readonly />
        </div>
        <div class="field">
          <label for="issuedPassword">{{ 'assistants.temporaryPassword' | t }}</label>
          <div class="handover">
            <input id="issuedPassword" [value]="credentials.temporaryPassword" readonly />
            <button type="button" (click)="copy(credentials.temporaryPassword!)">
              <bms-icon name="copy" [size]="16" />
              {{ 'assistants.copy' | t }}
            </button>
          </div>
        </div>
        <ng-container actions>
          <button class="primary" type="button" (click)="dismissCredentials()">
            {{ 'common.done' | t }}
          </button>
        </ng-container>
      </bms-dialog>
    }

    @if (editing()) {
      <bms-dialog [heading]="(editingId() ? 'assistants.editTitle' : 'assistants.add') | t" (closed)="cancel()">
        @if (!editingId()) {
          <div class="field">
            <label for="firstName">{{ 'common.firstName' | t }}</label>
            <input id="firstName" [(ngModel)]="firstName" />
          </div>

          <div class="field">
            <label for="lastName">{{ 'common.lastName' | t }}</label>
            <input id="lastName" [(ngModel)]="lastName" />
          </div>

          <div class="field">
            <label for="email">{{ 'assistants.theirEmail' | t }}</label>
            <input id="email" type="email" [(ngModel)]="email" placeholder="assistant@example.com" />
            <p class="hint">
              {{ 'assistants.emailHint' | t }}
            </p>
          </div>
        }

        <div class="field">
          <label>{{ 'assistants.mayDo' | t }}</label>
          <div class="checks">
            @for (permission of available.value(); track permission) {
              <div class="check">
                <input
                  type="checkbox"
                  [id]="permission"
                  [checked]="selected().has(permission)"
                  (change)="toggle(permission)"
                />
                <label [for]="permission">{{ permission | label: 'permission' }}</label>
              </div>
            }
          </div>
        </div>
        <ng-container actions>
          <button type="button" (click)="cancel()">{{ 'common.cancel' | t }}</button>
          <button class="primary" type="button" [disabled]="!complete()" (click)="save()">
            {{ (editingId() ? 'common.saveChanges' : 'assistants.create') | t }}
          </button>
        </ng-container>
      </bms-dialog>
    }
  `,
})
export class AssistantsPage {
  private api = inject(AssistantsApi);
  private i18n = inject(TranslationService);
  private confirm = inject(ConfirmService);
  private toasts = inject(ToastService);

  protected readonly editing = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly selected = signal(new Set<Permission>());
  protected readonly issued = signal<Assistant | null>(null);

  protected firstName = '';
  protected lastName = '';
  protected email = '';

  protected readonly assistants = rxResource({ stream: () => this.api.list() });

  protected readonly available = rxResource({
    stream: () => this.api.permissions(),
    defaultValue: [] as Permission[],
  });

  protected toggle(permission: Permission): void {
    const next = new Set(this.selected());
    if (next.has(permission)) {
      next.delete(permission);
    } else {
      next.add(permission);
    }
    this.selected.set(next);
  }

  protected complete(): boolean {
    if (this.editingId()) {
      return true;
    }
    return this.firstName.trim() !== '' && this.lastName.trim() !== '' && this.email.trim() !== '';
  }

  protected startGrant(): void {
    this.firstName = '';
    this.lastName = '';
    this.email = '';
    this.selected.set(new Set());
    this.editingId.set(null);
    this.editing.set(true);
  }

  protected startEdit(assistant: Assistant): void {
    this.email = assistant.email;
    this.selected.set(new Set(assistant.permissions));
    this.editingId.set(assistant.id);
    this.editing.set(true);
  }

  protected cancel(): void {
    this.editing.set(false);
  }

  protected save(): void {
    const permissions = [...this.selected()];
    const id = this.editingId();
    const request = id
      ? this.api.update(id, permissions)
      : this.api.grant(this.email.trim(), this.firstName.trim(), this.lastName.trim(), permissions);
    request.subscribe({
      next: (saved) => {
        this.editing.set(false);
        this.error.set(null);
        if (id) {
          this.toasts.show(this.i18n.translate('toast.saved', { name: saved.name }));
        }
        this.showCredentials(saved);
        this.assistants.reload();
      },
      error: (response) =>
        this.error.set(response?.error?.detail ?? this.i18n.translate('assistants.saveFailed')),
    });
  }

  protected resetPassword(assistant: Assistant): void {
    this.api.resetPassword(assistant.id).subscribe({
      next: (saved) => {
        this.error.set(null);
        this.showCredentials(saved);
      },
      error: () =>
        this.error.set(this.i18n.translate('assistants.resetFailed', { name: assistant.name })),
    });
  }

  protected copy(password: string): void {
    void navigator.clipboard
      .writeText(password)
      .then(() => this.toasts.show(this.i18n.translate('assistants.copied')));
  }

  protected dismissCredentials(): void {
    this.issued.set(null);
  }

  /** Only responses that created or reset an account carry a password. */
  private showCredentials(assistant: Assistant): void {
    this.issued.set(assistant.temporaryPassword ? assistant : null);
  }

  protected async revoke(assistant: Assistant): Promise<void> {
    const confirmed = await this.confirm.ask({
      heading: this.i18n.translate('confirm.removeAssistantTitle', { name: assistant.name }),
      body: this.i18n.translate('confirm.removeAssistantBody'),
      action: this.i18n.translate('assistants.remove'),
    });
    if (!confirmed) {
      return;
    }
    this.api.revoke(assistant.id).subscribe({
      next: () => {
        this.toasts.show(this.i18n.translate('toast.assistantRemoved', { name: assistant.name }));
        this.assistants.reload();
      },
      error: () =>
        this.error.set(this.i18n.translate('assistants.removeFailed', { name: assistant.name })),
    });
  }
}
