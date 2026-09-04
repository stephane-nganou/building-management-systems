import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { ApartmentsApi, TenantsApi } from '../core/api';
import { TranslationService } from '../core/i18n';
import { Tenant } from '../core/models';
import { DayPipe, MoneyPipe } from '../shared/money.pipe';
import { TranslatePipe } from '../shared/translate.pipe';

interface TenantForm {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  leaseStart: string;
  leaseEnd: string | null;
  deposit: number | null;
  active: boolean;
}

const blank = (): TenantForm => ({
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  leaseStart: new Date().toISOString().slice(0, 10),
  leaseEnd: null,
  deposit: null,
  active: true,
});

@Component({
  selector: 'bms-tenants',
  imports: [FormsModule, MoneyPipe, DayPipe, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'tenants.title' | t }}</h1>
          <p>{{ 'tenants.subtitle' | t }}</p>
        </div>
        <button class="primary" type="button" [disabled]="!hasApartments()" (click)="startCreate()">
          {{ 'tenants.add' | t }}
        </button>
      </div>

      @if (tenants.isLoading()) {
        <p class="loading">{{ 'tenants.loading' | t }}</p>
      } @else if (!hasApartments()) {
        <div class="empty">
          <h3>{{ 'tenants.needApartmentTitle' | t }}</h3>
          <p>{{ 'tenants.needApartmentBody' | t }}</p>
        </div>
      } @else if (tenants.hasValue() && tenants.value()!.length === 0) {
        <div class="empty">
          <h3>{{ 'tenants.emptyTitle' | t }}</h3>
          <p>{{ 'tenants.emptyBody' | t }}</p>
          <button class="primary" type="button" (click)="startCreate()">
            {{ 'tenants.add' | t }}
          </button>
        </div>
      } @else if (tenants.hasValue()) {
        <table class="sheet">
          <thead>
            <tr>
              <th>{{ 'common.name' | t }}</th>
              <th>{{ 'tenants.unit' | t }}</th>
              <th>{{ 'tenants.contact' | t }}</th>
              <th>{{ 'tenants.lease' | t }}</th>
              <th class="right">{{ 'tenants.deposit' | t }}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (tenant of tenants.value(); track tenant.id) {
              <tr>
                <td class="strong">
                  {{ tenant.firstName }} {{ tenant.lastName }}
                  @if (!tenant.active) {
                    <span class="mark">{{ 'tenants.past' | t }}</span>
                  }
                </td>
                <td class="muted">{{ tenant.buildingName }} - {{ tenant.apartmentLabel }}</td>
                <td class="muted">{{ tenant.email || tenant.phone || '-' }}</td>
                <td class="muted">
                  {{ tenant.leaseStart | day }}
                  @if (tenant.leaseEnd) {
                    - {{ tenant.leaseEnd | day }}
                  } @else {
                    {{ 'tenants.onwards' | t }}
                  }
                </td>
                <td class="right">{{ tenant.deposit | money }}</td>
                <td class="right">
                  <button class="quiet" type="button" (click)="startEdit(tenant)">
                    {{ 'common.edit' | t }}
                  </button>
                  <button class="quiet danger" type="button" (click)="remove(tenant)">
                    {{ 'common.delete' | t }}
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      }

      @if (error()) {
        <p class="notice">{{ error() }}</p>
      }
    </section>

    @if (editing()) {
      <div class="scrim" (click)="cancel()">
        <div class="panel" (click)="$event.stopPropagation()">
          <header>
            <h2>{{ (editingId() ? 'tenants.editTitle' : 'tenants.add') | t }}</h2>
          </header>
          <div class="body">
            @if (!editingId()) {
              <div class="field">
                <label for="apartment">{{ 'common.apartment' | t }}</label>
                <select id="apartment" [(ngModel)]="targetApartmentId">
                  @for (apartment of apartments.value(); track apartment.id) {
                    <option [value]="apartment.id">
                      {{ apartment.buildingName }} - {{ apartment.label }}
                    </option>
                  }
                </select>
              </div>
            }
            <div class="grid-2">
              <div class="field">
                <label for="firstName">{{ 'common.firstName' | t }}</label>
                <input id="firstName" [(ngModel)]="form.firstName" />
              </div>
              <div class="field">
                <label for="lastName">{{ 'common.lastName' | t }}</label>
                <input id="lastName" [(ngModel)]="form.lastName" />
              </div>
              <div class="field">
                <label for="email">{{ 'common.email' | t }}</label>
                <input id="email" type="email" [(ngModel)]="form.email" />
              </div>
              <div class="field">
                <label for="phone">{{ 'tenants.phone' | t }}</label>
                <input id="phone" [(ngModel)]="form.phone" />
              </div>
              <div class="field">
                <label for="leaseStart">{{ 'tenants.leaseStart' | t }}</label>
                <input id="leaseStart" type="date" [(ngModel)]="form.leaseStart" />
              </div>
              <div class="field">
                <label for="leaseEnd">{{ 'tenants.leaseEnd' | t }}</label>
                <input id="leaseEnd" type="date" [(ngModel)]="form.leaseEnd" />
              </div>
              <div class="field">
                <label for="deposit">{{ 'tenants.deposit' | t }}</label>
                <input id="deposit" type="number" step="0.01" [(ngModel)]="form.deposit" />
              </div>
            </div>
            <div class="check">
              <input id="active" type="checkbox" [(ngModel)]="form.active" />
              <label for="active">{{ 'tenants.living' | t }}</label>
            </div>
          </div>
          <footer>
            <button type="button" (click)="cancel()">{{ 'common.cancel' | t }}</button>
            <button
              class="primary"
              type="button"
              [disabled]="!form.firstName.trim() || !form.lastName.trim()"
              (click)="save()"
            >
              {{ (editingId() ? 'common.saveChanges' : 'tenants.add') | t }}
            </button>
          </footer>
        </div>
      </div>
    }
  `,
})
export class TenantsPage {
  private api = inject(TenantsApi);
  private apartmentsApi = inject(ApartmentsApi);
  private i18n = inject(TranslationService);

  protected readonly editing = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected form: TenantForm = blank();
  protected targetApartmentId = '';

  protected readonly apartments = rxResource({
    stream: () => this.apartmentsApi.list(),
    defaultValue: [],
  });

  protected readonly tenants = rxResource({ stream: () => this.api.list() });

  protected hasApartments(): boolean {
    return this.apartments.value().length > 0;
  }

  protected startCreate(): void {
    this.form = blank();
    this.editingId.set(null);
    this.targetApartmentId = this.apartments.value()[0]?.id ?? '';
    this.editing.set(true);
  }

  protected startEdit(tenant: Tenant): void {
    this.form = {
      firstName: tenant.firstName,
      lastName: tenant.lastName,
      email: tenant.email ?? '',
      phone: tenant.phone ?? '',
      leaseStart: tenant.leaseStart,
      leaseEnd: tenant.leaseEnd,
      deposit: tenant.deposit,
      active: tenant.active,
    };
    this.editingId.set(tenant.id);
    this.editing.set(true);
  }

  protected cancel(): void {
    this.editing.set(false);
  }

  protected save(): void {
    const id = this.editingId();
    const body = { ...this.form, email: this.form.email || null, leaseEnd: this.form.leaseEnd || null };
    const request = id
      ? this.api.update(id, body)
      : this.api.create(this.targetApartmentId, body);
    request.subscribe({
      next: () => {
        this.editing.set(false);
        this.error.set(null);
        this.tenants.reload();
      },
      error: (response) =>
        this.error.set(response?.error?.detail ?? this.i18n.translate('tenants.saveFailed')),
    });
  }

  protected remove(tenant: Tenant): void {
    const name = `${tenant.firstName} ${tenant.lastName}`;
    this.api.remove(tenant.id).subscribe({
      next: () => this.tenants.reload(),
      error: () => this.error.set(this.i18n.translate('tenants.deleteFailed', { name })),
    });
  }
}
