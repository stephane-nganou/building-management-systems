import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { BuildingsApi, InvoicesApi, TenantsApi } from '../core/api';
import { TranslationService } from '../core/i18n';
import { Invoice, InvoiceStatus, InvoiceType } from '../core/models';
import { DayPipe, LabelPipe, MoneyPipe } from '../shared/money.pipe';
import { TranslatePipe } from '../shared/translate.pipe';

const STATUSES: InvoiceStatus[] = ['DRAFT', 'SENT', 'PAID', 'CANCELLED'];
const TYPES: InvoiceType[] = ['RENT', 'COLD_WATER'];

interface LineForm {
  description: string;
  quantity: number;
  unitPrice: number;
  unit: string;
}

interface InvoiceForm {
  tenantId: string;
  type: InvoiceType;
  periodStart: string;
  periodEnd: string;
  issueDate: string;
  dueDate: string;
  notes: string;
  lines: LineForm[];
}

function monthBounds(): { start: string; end: string } {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1);
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return { start: start.toISOString().slice(0, 10), end: end.toISOString().slice(0, 10) };
}

const blank = (): InvoiceForm => {
  const { start, end } = monthBounds();
  const due = new Date();
  due.setDate(due.getDate() + 14);
  return {
    tenantId: '',
    type: 'RENT',
    periodStart: start,
    periodEnd: end,
    issueDate: new Date().toISOString().slice(0, 10),
    dueDate: due.toISOString().slice(0, 10),
    notes: '',
    lines: [],
  };
};

@Component({
  selector: 'bms-invoices',
  imports: [FormsModule, MoneyPipe, DayPipe, LabelPipe, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'invoices.title' | t }}</h1>
          <p>{{ 'invoices.subtitle' | t }}</p>
        </div>
        <button class="primary" type="button" [disabled]="!hasTenants()" (click)="startCreate()">
          {{ 'invoices.add' | t }}
        </button>
      </div>

      <div class="toolbar">
        <div class="field">
          <label for="filterBuilding">{{ 'common.building' | t }}</label>
          <select
            id="filterBuilding"
            [ngModel]="filterBuilding()"
            (ngModelChange)="filterBuilding.set($event)"
          >
            <option value="">{{ 'common.allBuildings' | t }}</option>
            @for (building of buildings.value(); track building.id) {
              <option [value]="building.id">{{ building.name }}</option>
            }
          </select>
        </div>
        <div class="field">
          <label for="filterStatus">{{ 'common.status' | t }}</label>
          <select
            id="filterStatus"
            [ngModel]="filterStatus()"
            (ngModelChange)="filterStatus.set($event)"
          >
            <option value="">{{ 'invoices.anyStatus' | t }}</option>
            @for (status of statuses; track status) {
              <option [value]="status">{{ status | label: 'invoiceStatus' }}</option>
            }
          </select>
        </div>
      </div>

      @if (invoices.isLoading()) {
        <p class="loading">{{ 'invoices.loading' | t }}</p>
      } @else if (!hasTenants()) {
        <div class="empty">
          <h3>{{ 'invoices.needTenantTitle' | t }}</h3>
          <p>{{ 'invoices.needTenantBody' | t }}</p>
        </div>
      } @else if (invoices.hasValue() && invoices.value()!.length === 0) {
        <div class="empty">
          <h3>{{ 'invoices.emptyTitle' | t }}</h3>
          <p>{{ 'invoices.emptyBody' | t }}</p>
          <button class="primary" type="button" (click)="startCreate()">
            {{ 'invoices.add' | t }}
          </button>
        </div>
      } @else if (invoices.hasValue()) {
        <table class="sheet">
          <thead>
            <tr>
              <th>{{ 'invoices.number' | t }}</th>
              <th>{{ 'invoices.tenant' | t }}</th>
              <th>{{ 'invoices.unit' | t }}</th>
              <th>{{ 'invoices.type' | t }}</th>
              <th>{{ 'invoices.period' | t }}</th>
              <th>{{ 'invoices.due' | t }}</th>
              <th class="right">{{ 'common.total' | t }}</th>
              <th>{{ 'common.status' | t }}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (invoice of invoices.value(); track invoice.id) {
              <tr>
                <td class="strong">{{ invoice.invoiceNumber }}</td>
                <td>{{ invoice.tenantName }}</td>
                <td class="muted">{{ invoice.buildingName }} - {{ invoice.apartmentLabel }}</td>
                <td>{{ invoice.type | label: 'invoiceType' }}</td>
                <td class="muted">{{ invoice.periodStart | day }} - {{ invoice.periodEnd | day }}</td>
                <td class="muted">{{ invoice.dueDate | day }}</td>
                <td class="right strong">{{ invoice.total | money }}</td>
                <td>
                  <span class="mark {{ invoice.status.toLowerCase() }}">
                    {{ invoice.status | label: 'invoiceStatus' }}
                  </span>
                </td>
                <td class="right">
                  <button class="quiet" type="button" (click)="download(invoice)">PDF</button>
                  @if (invoice.status === 'DRAFT') {
                    <button class="quiet" type="button" (click)="setStatus(invoice, 'SENT')">
                      {{ 'invoices.markSent' | t }}
                    </button>
                  }
                  @if (invoice.status === 'SENT') {
                    <button class="quiet" type="button" (click)="setStatus(invoice, 'PAID')">
                      {{ 'invoices.markPaid' | t }}
                    </button>
                  }
                  @if (invoice.status === 'DRAFT') {
                    <button class="quiet danger" type="button" (click)="remove(invoice)">
                      {{ 'common.delete' | t }}
                    </button>
                  }
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
            <h2>{{ 'invoices.add' | t }}</h2>
          </header>
          <div class="body">
            <div class="grid-2">
              <div class="field">
                <label for="tenant">{{ 'invoices.tenant' | t }}</label>
                <select id="tenant" [(ngModel)]="form.tenantId">
                  @for (tenant of tenants.value(); track tenant.id) {
                    <option [value]="tenant.id">
                      {{ tenant.firstName }} {{ tenant.lastName }} - {{ tenant.apartmentLabel }}
                    </option>
                  }
                </select>
              </div>
              <div class="field">
                <label for="type">{{ 'invoices.type' | t }}</label>
                <select id="type" [(ngModel)]="form.type">
                  @for (type of types; track type) {
                    <option [value]="type">{{ type | label: 'invoiceType' }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label for="periodStart">{{ 'invoices.periodStart' | t }}</label>
                <input id="periodStart" type="date" [(ngModel)]="form.periodStart" />
              </div>
              <div class="field">
                <label for="periodEnd">{{ 'invoices.periodEnd' | t }}</label>
                <input id="periodEnd" type="date" [(ngModel)]="form.periodEnd" />
              </div>
              <div class="field">
                <label for="issueDate">{{ 'invoices.issueDate' | t }}</label>
                <input id="issueDate" type="date" [(ngModel)]="form.issueDate" />
              </div>
              <div class="field">
                <label for="dueDate">{{ 'invoices.dueDate' | t }}</label>
                <input id="dueDate" type="date" [(ngModel)]="form.dueDate" />
              </div>
            </div>

            @if (form.type === 'RENT' && form.lines.length === 0) {
              <p class="muted">{{ 'invoices.rentHint' | t }}</p>
            }
            @if (form.type === 'COLD_WATER' && form.lines.length === 0) {
              <p class="muted">{{ 'invoices.coldWaterHint' | t }}</p>
            }

            @for (line of form.lines; track $index) {
              <div class="grid-2">
                <div class="field">
                  <label>{{ 'common.description' | t }}</label>
                  <input [(ngModel)]="line.description" [name]="'d' + $index" />
                </div>
                <div class="field">
                  <label>{{ 'invoices.lineUnit' | t }}</label>
                  <input [(ngModel)]="line.unit" [name]="'u' + $index" placeholder="m3" />
                </div>
                <div class="field">
                  <label>{{ 'invoices.lineQuantity' | t }}</label>
                  <input type="number" step="0.001" [(ngModel)]="line.quantity" [name]="'q' + $index" />
                </div>
                <div class="field">
                  <label>{{ 'invoices.lineUnitPrice' | t }}</label>
                  <input type="number" step="0.01" [(ngModel)]="line.unitPrice" [name]="'p' + $index" />
                </div>
              </div>
            }

            <button type="button" (click)="addLine()">{{ 'invoices.addLine' | t }}</button>

            <div class="field" style="margin-top:14px">
              <label for="notes">{{ 'common.notes' | t }}</label>
              <input id="notes" [(ngModel)]="form.notes" />
            </div>
          </div>
          <footer>
            <button type="button" (click)="cancel()">{{ 'common.cancel' | t }}</button>
            <button class="primary" type="button" [disabled]="!form.tenantId" (click)="save()">
              {{ 'invoices.add' | t }}
            </button>
          </footer>
        </div>
      </div>
    }
  `,
})
export class InvoicesPage {
  private api = inject(InvoicesApi);
  private buildingsApi = inject(BuildingsApi);
  private tenantsApi = inject(TenantsApi);
  private i18n = inject(TranslationService);

  protected readonly statuses = STATUSES;
  protected readonly types = TYPES;
  protected readonly filterBuilding = signal('');
  protected readonly filterStatus = signal<InvoiceStatus | ''>('');
  protected readonly editing = signal(false);
  protected readonly error = signal<string | null>(null);
  protected form: InvoiceForm = blank();

  protected readonly buildings = rxResource({
    stream: () => this.buildingsApi.list(),
    defaultValue: [],
  });

  protected readonly tenants = rxResource({
    stream: () => this.tenantsApi.list(),
    defaultValue: [],
  });

  protected readonly invoices = rxResource({
    params: () => ({ buildingId: this.filterBuilding(), status: this.filterStatus() }),
    stream: ({ params }) =>
      this.api.search({ buildingId: params.buildingId || undefined, status: params.status }),
  });

  protected hasTenants(): boolean {
    return this.tenants.value().length > 0;
  }

  protected startCreate(): void {
    this.form = blank();
    this.form.tenantId = this.tenants.value()[0]?.id ?? '';
    this.editing.set(true);
  }

  protected addLine(): void {
    this.form.lines = [
      ...this.form.lines,
      { description: '', quantity: 1, unitPrice: 0, unit: '' },
    ];
  }

  protected cancel(): void {
    this.editing.set(false);
  }

  protected save(): void {
    const body = {
      ...this.form,
      notes: this.form.notes || null,
      lines: this.form.lines.length ? this.form.lines : null,
    };
    this.api.create(body).subscribe({
      next: () => {
        this.editing.set(false);
        this.error.set(null);
        this.invoices.reload();
      },
      error: (response) =>
        this.error.set(response?.error?.detail ?? this.i18n.translate('invoices.createFailed')),
    });
  }

  protected setStatus(invoice: Invoice, status: InvoiceStatus): void {
    this.api.changeStatus(invoice.id, status).subscribe({
      next: () => this.invoices.reload(),
      error: (response) =>
        this.error.set(response?.error?.detail ?? this.i18n.translate('invoices.statusFailed')),
    });
  }

  protected remove(invoice: Invoice): void {
    this.api.remove(invoice.id).subscribe({
      next: () => this.invoices.reload(),
      error: (response) =>
        this.error.set(response?.error?.detail ?? this.i18n.translate('invoices.deleteFailed')),
    });
  }

  protected download(invoice: Invoice): void {
    this.api.downloadPdf(invoice.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${invoice.invoiceNumber}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.error.set(this.i18n.translate('invoices.pdfFailed')),
    });
  }
}
