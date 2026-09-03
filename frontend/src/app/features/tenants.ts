import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { ApartmentsApi, TenantsApi } from '../core/api';
import { Tenant } from '../core/models';
import { DayPipe, MoneyPipe } from '../shared/money.pipe';

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
  imports: [FormsModule, MoneyPipe, DayPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>Tenants</h1>
          <p>Who lives where, and for how long their lease runs.</p>
        </div>
        <button class="primary" type="button" [disabled]="!hasApartments()" (click)="startCreate()">
          Add tenant
        </button>
      </div>

      @if (tenants.isLoading()) {
        <p class="loading">Loading tenants.</p>
      } @else if (!hasApartments()) {
        <div class="empty">
          <h3>Add an apartment first</h3>
          <p>A tenant is always attached to a unit, so create one before adding people.</p>
        </div>
      } @else if (tenants.hasValue() && tenants.value()!.length === 0) {
        <div class="empty">
          <h3>No tenants recorded</h3>
          <p>Add the people renting your units to start issuing invoices to them.</p>
          <button class="primary" type="button" (click)="startCreate()">Add tenant</button>
        </div>
      } @else if (tenants.hasValue()) {
        <table class="sheet">
          <thead>
            <tr>
              <th>Name</th>
              <th>Unit</th>
              <th>Contact</th>
              <th>Lease</th>
              <th class="right">Deposit</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (tenant of tenants.value(); track tenant.id) {
              <tr>
                <td class="strong">
                  {{ tenant.firstName }} {{ tenant.lastName }}
                  @if (!tenant.active) {
                    <span class="mark">Past</span>
                  }
                </td>
                <td class="muted">{{ tenant.buildingName }} - {{ tenant.apartmentLabel }}</td>
                <td class="muted">{{ tenant.email || tenant.phone || '-' }}</td>
                <td class="muted">
                  {{ tenant.leaseStart | day }}
                  @if (tenant.leaseEnd) {
                    to {{ tenant.leaseEnd | day }}
                  } @else {
                    onwards
                  }
                </td>
                <td class="right">{{ tenant.deposit | money }}</td>
                <td class="right">
                  <button class="quiet" type="button" (click)="startEdit(tenant)">Edit</button>
                  <button class="quiet danger" type="button" (click)="remove(tenant)">Delete</button>
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
            <h2>{{ editingId() ? 'Edit tenant' : 'Add tenant' }}</h2>
          </header>
          <div class="body">
            @if (!editingId()) {
              <div class="field">
                <label for="apartment">Apartment</label>
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
                <label for="firstName">First name</label>
                <input id="firstName" [(ngModel)]="form.firstName" />
              </div>
              <div class="field">
                <label for="lastName">Last name</label>
                <input id="lastName" [(ngModel)]="form.lastName" />
              </div>
              <div class="field">
                <label for="email">Email</label>
                <input id="email" type="email" [(ngModel)]="form.email" />
              </div>
              <div class="field">
                <label for="phone">Phone</label>
                <input id="phone" [(ngModel)]="form.phone" />
              </div>
              <div class="field">
                <label for="leaseStart">Lease start</label>
                <input id="leaseStart" type="date" [(ngModel)]="form.leaseStart" />
              </div>
              <div class="field">
                <label for="leaseEnd">Lease end</label>
                <input id="leaseEnd" type="date" [(ngModel)]="form.leaseEnd" />
              </div>
              <div class="field">
                <label for="deposit">Deposit</label>
                <input id="deposit" type="number" step="0.01" [(ngModel)]="form.deposit" />
              </div>
            </div>
            <div class="check">
              <input id="active" type="checkbox" [(ngModel)]="form.active" />
              <label for="active">Currently living here</label>
            </div>
          </div>
          <footer>
            <button type="button" (click)="cancel()">Cancel</button>
            <button
              class="primary"
              type="button"
              [disabled]="!form.firstName.trim() || !form.lastName.trim()"
              (click)="save()"
            >
              {{ editingId() ? 'Save changes' : 'Add tenant' }}
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
        this.error.set(response?.error?.detail ?? 'The tenant could not be saved.'),
    });
  }

  protected remove(tenant: Tenant): void {
    this.api.remove(tenant.id).subscribe({
      next: () => this.tenants.reload(),
      error: () => this.error.set(`${tenant.firstName} ${tenant.lastName} could not be deleted.`),
    });
  }
}
