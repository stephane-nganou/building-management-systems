import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { BuildingsApi, InvoicesApi, TenantsApi } from '../core/api';
import { Invoice, InvoiceStatus, InvoiceType } from '../core/models';
import { DayPipe, LabelPipe, MoneyPipe } from '../shared/money.pipe';

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
  imports: [FormsModule, MoneyPipe, DayPipe, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>Invoices</h1>
          <p>Rent and cold water billed to your tenants. Download any of them as a PDF.</p>
        </div>
        <button class="primary" type="button" [disabled]="!hasTenants()" (click)="startCreate()">
          Create invoice
        </button>
      </div>

      <div class="toolbar">
        <div class="field">
          <label for="filterBuilding">Building</label>
          <select
            id="filterBuilding"
            [ngModel]="filterBuilding()"
            (ngModelChange)="filterBuilding.set($event)"
          >
            <option value="">All buildings</option>
            @for (building of buildings.value(); track building.id) {
              <option [value]="building.id">{{ building.name }}</option>
            }
          </select>
        </div>
        <div class="field">
          <label for="filterStatus">Status</label>
          <select
            id="filterStatus"
            [ngModel]="filterStatus()"
            (ngModelChange)="filterStatus.set($event)"
          >
            <option value="">Any status</option>
            <option value="DRAFT">Draft</option>
            <option value="SENT">Sent</option>
            <option value="PAID">Paid</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
      </div>

      @if (invoices.isLoading()) {
        <p class="loading">Loading invoices.</p>
      } @else if (!hasTenants()) {
        <div class="empty">
          <h3>Add a tenant first</h3>
          <p>Invoices are addressed to a tenant, so record one before billing.</p>
        </div>
      } @else if (invoices.hasValue() && invoices.value()!.length === 0) {
        <div class="empty">
          <h3>No invoices yet</h3>
          <p>Create a rent invoice and the lines fill in from the apartment automatically.</p>
          <button class="primary" type="button" (click)="startCreate()">Create invoice</button>
        </div>
      } @else if (invoices.hasValue()) {
        <table class="sheet">
          <thead>
            <tr>
              <th>Number</th>
              <th>Tenant</th>
              <th>Unit</th>
              <th>Type</th>
              <th>Period</th>
              <th>Due</th>
              <th class="right">Total</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (invoice of invoices.value(); track invoice.id) {
              <tr>
                <td class="strong">{{ invoice.invoiceNumber }}</td>
                <td>{{ invoice.tenantName }}</td>
                <td class="muted">{{ invoice.buildingName }} - {{ invoice.apartmentLabel }}</td>
                <td>{{ invoice.type | label }}</td>
                <td class="muted">{{ invoice.periodStart | day }} to {{ invoice.periodEnd | day }}</td>
                <td class="muted">{{ invoice.dueDate | day }}</td>
                <td class="right strong">{{ invoice.total | money }}</td>
                <td>
                  <span class="mark {{ invoice.status.toLowerCase() }}">
                    {{ invoice.status | label }}
                  </span>
                </td>
                <td class="right">
                  <button class="quiet" type="button" (click)="download(invoice)">PDF</button>
                  @if (invoice.status === 'DRAFT') {
                    <button class="quiet" type="button" (click)="setStatus(invoice, 'SENT')">
                      Mark sent
                    </button>
                  }
                  @if (invoice.status === 'SENT') {
                    <button class="quiet" type="button" (click)="setStatus(invoice, 'PAID')">
                      Mark paid
                    </button>
                  }
                  @if (invoice.status === 'DRAFT') {
                    <button class="quiet danger" type="button" (click)="remove(invoice)">Delete</button>
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
            <h2>Create invoice</h2>
          </header>
          <div class="body">
            <div class="grid-2">
              <div class="field">
                <label for="tenant">Tenant</label>
                <select id="tenant" [(ngModel)]="form.tenantId">
                  @for (tenant of tenants.value(); track tenant.id) {
                    <option [value]="tenant.id">
                      {{ tenant.firstName }} {{ tenant.lastName }} - {{ tenant.apartmentLabel }}
                    </option>
                  }
                </select>
              </div>
              <div class="field">
                <label for="type">Type</label>
                <select id="type" [(ngModel)]="form.type">
                  <option value="RENT">Rent</option>
                  <option value="COLD_WATER">Cold water</option>
                </select>
              </div>
              <div class="field">
                <label for="periodStart">Period from</label>
                <input id="periodStart" type="date" [(ngModel)]="form.periodStart" />
              </div>
              <div class="field">
                <label for="periodEnd">Period to</label>
                <input id="periodEnd" type="date" [(ngModel)]="form.periodEnd" />
              </div>
              <div class="field">
                <label for="issueDate">Issued</label>
                <input id="issueDate" type="date" [(ngModel)]="form.issueDate" />
              </div>
              <div class="field">
                <label for="dueDate">Due</label>
                <input id="dueDate" type="date" [(ngModel)]="form.dueDate" />
              </div>
            </div>

            @if (form.type === 'RENT' && form.lines.length === 0) {
              <p class="muted">
                Leave the lines empty and the rent and utilities advance are taken from the apartment.
              </p>
            }
            @if (form.type === 'COLD_WATER' && form.lines.length === 0) {
              <p class="muted">A cold water invoice needs at least one line, such as metered m3.</p>
            }

            @for (line of form.lines; track $index) {
              <div class="grid-2">
                <div class="field">
                  <label>Description</label>
                  <input [(ngModel)]="line.description" [name]="'d' + $index" />
                </div>
                <div class="field">
                  <label>Unit</label>
                  <input [(ngModel)]="line.unit" [name]="'u' + $index" placeholder="m3" />
                </div>
                <div class="field">
                  <label>Quantity</label>
                  <input type="number" step="0.001" [(ngModel)]="line.quantity" [name]="'q' + $index" />
                </div>
                <div class="field">
                  <label>Unit price</label>
                  <input type="number" step="0.01" [(ngModel)]="line.unitPrice" [name]="'p' + $index" />
                </div>
              </div>
            }

            <button type="button" (click)="addLine()">Add a line</button>

            <div class="field" style="margin-top:14px">
              <label for="notes">Notes</label>
              <input id="notes" [(ngModel)]="form.notes" />
            </div>
          </div>
          <footer>
            <button type="button" (click)="cancel()">Cancel</button>
            <button class="primary" type="button" [disabled]="!form.tenantId" (click)="save()">
              Create invoice
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
        this.error.set(response?.error?.detail ?? 'The invoice could not be created.'),
    });
  }

  protected setStatus(invoice: Invoice, status: InvoiceStatus): void {
    this.api.changeStatus(invoice.id, status).subscribe({
      next: () => this.invoices.reload(),
      error: (response) =>
        this.error.set(response?.error?.detail ?? 'The status could not be changed.'),
    });
  }

  protected remove(invoice: Invoice): void {
    this.api.remove(invoice.id).subscribe({
      next: () => this.invoices.reload(),
      error: (response) =>
        this.error.set(response?.error?.detail ?? 'The invoice could not be deleted.'),
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
      error: () => this.error.set('The PDF could not be generated.'),
    });
  }
}
