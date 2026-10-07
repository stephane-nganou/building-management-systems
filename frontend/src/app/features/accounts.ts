import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { AdminApi } from '../core/api';
import { TranslationService } from '../core/i18n';
import { Account, SubscriptionStatus } from '../core/models';
import { MessageKey } from '../i18n/en';
import { ConfirmService } from '../shared/confirm';
import { isoDay } from '../shared/dates';
import { Dialog } from '../shared/dialog';
import { Icon } from '../shared/icon';
import { IconButton } from '../shared/icon-button';
import { DayPipe, LabelPipe } from '../shared/money.pipe';
import { ToastService } from '../shared/toasts';
import { TranslatePipe } from '../shared/translate.pipe';

const STATUSES: SubscriptionStatus[] = ['ACTIVE', 'EXPIRED', 'SUSPENDED'];

function shift(iso: string, years: number, days: number): string {
  const [year, month, day] = iso.split('-').map(Number);
  return isoDay(new Date(year + years, month - 1, day + days));
}

/** The service's customers: every owner, where their subscription stands, and what can be done about it. */
@Component({
  selector: 'bms-accounts',
  imports: [FormsModule, Dialog, Icon, IconButton, DayPipe, LabelPipe, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'accounts.title' | t }}</h1>
          <p>{{ 'accounts.subtitle' | t }}</p>
        </div>
        <button class="primary" type="button" (click)="startCreate()">
          <bms-icon name="plus" />
          {{ 'accounts.add' | t }}
        </button>
      </div>

      <div class="toolbar">
        <div class="field">
          <label for="filterStatus">{{ 'common.status' | t }}</label>
          <select id="filterStatus" [ngModel]="filterStatus()" (ngModelChange)="filterStatus.set($event)">
            <option value="">{{ 'accounts.anyStatus' | t }}</option>
            @for (status of statuses; track status) {
              <option [value]="status">{{ status | label: 'subscriptionStatus' }}</option>
            }
          </select>
        </div>
      </div>

      @if (accounts.isLoading()) {
        <p class="loading">{{ 'accounts.loading' | t }}</p>
      } @else if (accounts.hasValue() && accounts.value()!.length === 0) {
        <p class="muted">{{ 'accounts.empty' | t }}</p>
      } @else if (accounts.hasValue()) {
        <div class="sheet-frame">
          <table class="sheet">
            <thead>
              <tr>
                <th>{{ 'common.name' | t }}</th>
                <th>{{ 'common.email' | t }}</th>
                <th>{{ 'accounts.since' | t }}</th>
                <th class="right">{{ 'nav.buildings' | t }}</th>
                <th class="right">{{ 'nav.assistants' | t }}</th>
                <th>{{ 'common.status' | t }}</th>
                <th>{{ 'accounts.endsOn' | t }}</th>
                <th><span class="visually-hidden">{{ 'common.actions' | t }}</span></th>
              </tr>
            </thead>
            <tbody>
              @for (account of accounts.value(); track account.id) {
                <tr>
                  <td class="strong">{{ account.name }}</td>
                  <td class="muted">{{ account.email }}</td>
                  <td class="muted">{{ account.createdAt | day }}</td>
                  <td class="right">{{ account.buildings }}</td>
                  <td class="right">{{ account.assistants }}</td>
                  <td>
                    <span class="mark {{ account.status.toLowerCase() }}">
                      {{ account.status | label: 'subscriptionStatus' }}
                    </span>
                  </td>
                  <td class="muted">{{ account.endsOn | day }}</td>
                  <td class="actions-cell">
                    <span class="row-actions">
                      <button class="quiet" type="button" (click)="manage(account)">
                        <bms-icon name="calendar" [size]="16" />
                        {{ 'accounts.subscription' | t }}
                      </button>
                      @if (account.status === 'SUSPENDED') {
                        <button bmsIconButton icon="play" [label]="'accounts.reactivate' | t" (click)="reactivate(account)"></button>
                      } @else {
                        <button bmsIconButton class="danger" icon="pause" [label]="'accounts.suspend' | t" (click)="suspend(account)"></button>
                      }
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

    @if (creating()) {
      <bms-dialog [heading]="'accounts.add' | t" [error]="error()" (closed)="creating.set(false)">
        <div class="field">
          <label for="firstName">{{ 'common.firstName' | t }}</label>
          <input id="firstName" [(ngModel)]="firstName" />
        </div>
        <div class="field">
          <label for="lastName">{{ 'common.lastName' | t }}</label>
          <input id="lastName" [(ngModel)]="lastName" />
        </div>
        <div class="field">
          <label for="email">{{ 'common.email' | t }}</label>
          <input id="email" type="email" [(ngModel)]="email" />
          <p class="hint">{{ 'accounts.emailHint' | t }}</p>
        </div>
        <ng-container actions>
          <button type="button" (click)="creating.set(false)">{{ 'common.cancel' | t }}</button>
          <button class="primary" type="button" [disabled]="!ownerComplete()" (click)="create()">
            {{ 'accounts.create' | t }}
          </button>
        </ng-container>
      </bms-dialog>
    }

    @if (issued(); as credentials) {
      <bms-dialog [heading]="'assistants.handOver' | t" (closed)="issued.set(null)">
        <p>{{ 'accounts.handOverBody' | t: { name: credentials.name } }}</p>
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
          <button class="primary" type="button" (click)="issued.set(null)">{{ 'common.done' | t }}</button>
        </ng-container>
      </bms-dialog>
    }

    @if (managing(); as account) {
      <bms-dialog
        [heading]="'accounts.subscriptionOf' | t: { name: account.name }"
        [error]="error()"
        (closed)="managing.set(null)"
      >
        @if (periods.hasValue()) {
          <table class="sheet periods">
            <thead>
              <tr>
                <th>{{ 'common.from' | t }}</th>
                <th>{{ 'common.to' | t }}</th>
                <th>{{ 'common.notes' | t }}</th>
              </tr>
            </thead>
            <tbody>
              @for (period of periods.value(); track period.id) {
                <tr>
                  <td>{{ period.startsOn | day }}</td>
                  <td>{{ period.endsOn | day }}</td>
                  <td class="muted">{{ period.note }}</td>
                </tr>
              }
            </tbody>
          </table>
        }

        <h3>{{ 'accounts.addPeriod' | t }}</h3>
        <div class="grid-2">
          <div class="field">
            <label for="startsOn">{{ 'common.from' | t }}</label>
            <input id="startsOn" type="date" [(ngModel)]="startsOn" />
          </div>
          <div class="field">
            <label for="endsOn">{{ 'accounts.lastDay' | t }}</label>
            <input id="endsOn" type="date" [(ngModel)]="endsOn" />
          </div>
        </div>
        <div class="field">
          <label for="note">{{ 'common.notes' | t }}</label>
          <input id="note" [(ngModel)]="note" />
        </div>
        <ng-container actions>
          <button class="quiet" type="button" (click)="endNow(account)">{{ 'accounts.endNow' | t }}</button>
          <button type="button" (click)="managing.set(null)">{{ 'common.close' | t }}</button>
          <button class="primary" type="button" [disabled]="!startsOn || !endsOn" (click)="addPeriod(account)">
            {{ 'accounts.addPeriod' | t }}
          </button>
        </ng-container>
      </bms-dialog>
    }
  `,
})
export class AccountsPage {
  private api = inject(AdminApi);
  private i18n = inject(TranslationService);
  private confirm = inject(ConfirmService);
  private toasts = inject(ToastService);

  protected readonly statuses = STATUSES;
  protected readonly filterStatus = signal<SubscriptionStatus | ''>('');
  protected readonly error = signal<string | null>(null);
  protected readonly creating = signal(false);
  protected readonly issued = signal<Account | null>(null);
  protected readonly managing = signal<Account | null>(null);

  protected firstName = '';
  protected lastName = '';
  protected email = '';
  protected startsOn = '';
  protected endsOn = '';
  protected note = '';

  protected readonly accounts = rxResource({
    params: () => this.filterStatus(),
    stream: ({ params }) => this.api.accounts(params || undefined),
  });

  protected readonly periods = rxResource({
    params: () => this.managing()?.id,
    stream: ({ params }) => this.api.periods(params),
  });

  protected ownerComplete(): boolean {
    return this.firstName.trim() !== '' && this.lastName.trim() !== '' && this.email.trim() !== '';
  }

  protected startCreate(): void {
    this.error.set(null);
    this.firstName = '';
    this.lastName = '';
    this.email = '';
    this.creating.set(true);
  }

  protected create(): void {
    this.api.createOwner(this.email.trim(), this.firstName.trim(), this.lastName.trim()).subscribe({
      next: (account) => {
        this.creating.set(false);
        this.error.set(null);
        this.issued.set(account);
        this.accounts.reload();
      },
      error: this.failed,
    });
  }

  /** Suggests a year that carries on from where the subscription stands. */
  protected manage(account: Account): void {
    const today = isoDay(new Date());
    this.startsOn = account.status === 'ACTIVE' && account.endsOn ? shift(account.endsOn, 0, 1) : today;
    this.endsOn = shift(this.startsOn, 1, -1);
    this.note = '';
    this.error.set(null);
    this.managing.set(account);
  }

  protected addPeriod(account: Account): void {
    this.api.addPeriod(account.id, this.startsOn, this.endsOn, this.note.trim()).subscribe({
      next: () => this.changed('accounts.periodAdded', account),
      error: this.failed,
    });
  }

  protected async endNow(account: Account): Promise<void> {
    const confirmed = await this.confirm.ask({
      heading: this.i18n.translate('confirm.endSubscriptionTitle', { name: account.name }),
      body: this.i18n.translate('confirm.endSubscriptionBody'),
      action: this.i18n.translate('accounts.endNow'),
    });
    if (!confirmed) {
      return;
    }
    this.api.endCurrentPeriod(account.id).subscribe({
      next: () => this.changed('accounts.ended', account),
      error: this.failed,
    });
  }

  protected async suspend(account: Account): Promise<void> {
    const confirmed = await this.confirm.ask({
      heading: this.i18n.translate('confirm.suspendTitle', { name: account.name }),
      body: this.i18n.translate('confirm.suspendBody'),
      action: this.i18n.translate('accounts.suspend'),
    });
    if (!confirmed) {
      return;
    }
    this.api.suspend(account.id).subscribe({
      next: () => this.changed('accounts.suspended', account),
      error: this.failed,
    });
  }

  protected reactivate(account: Account): void {
    this.api.reactivate(account.id).subscribe({
      next: () => this.changed('accounts.reactivated', account),
      error: this.failed,
    });
  }

  protected copy(password: string): void {
    void navigator.clipboard
      .writeText(password)
      .then(() => this.toasts.show(this.i18n.translate('assistants.copied')));
  }

  /** Every change here ends the same way: say what happened, and show where things stand now. */
  private changed(key: MessageKey, account: Account): void {
    this.error.set(null);
    this.toasts.show(this.i18n.translate(key, { name: account.name }));
    this.periods.reload();
    this.accounts.reload();
  }

  private readonly failed = (response: HttpErrorResponse): void =>
    this.error.set(response.error?.detail ?? this.i18n.translate('accounts.saveFailed'));
}
