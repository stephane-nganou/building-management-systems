import { ChangeDetectionStrategy, Component, inject, linkedSignal, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { BuildingsApi, InvoicesApi, TenantsApi } from '../core/api';
import { TranslationService } from '../core/i18n';
import { Invoice, InvoiceStatus, InvoiceType } from '../core/models';
import { MessageKey } from '../i18n/en';
import { ConfirmService } from '../shared/confirm';
import { Dialog } from '../shared/dialog';
import { Facade } from '../shared/facade';
import { Icon } from '../shared/icon';
import { IconButton } from '../shared/icon-button';
import { Pager, stepBackFromEmptyPage } from '../shared/pager';
import { DayPipe, LabelPipe, MoneyPipe } from '../shared/money.pipe';
import { ToastService } from '../shared/toasts';
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
  imports: [FormsModule, Dialog, Facade, Icon, IconButton, Pager, MoneyPipe, DayPipe, LabelPipe, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'invoices.title' | t }}</h1>
          <p>{{ 'invoices.subtitle' | t }}</p>
        </div>
        <button class="primary" type="button" [disabled]="!hasTenants()" (click)="startCreate()">
          <bms-icon name="plus" />
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
          <bms-facade [units]="[]" [scale]="2" />
          <div>
            <h3>{{ 'invoices.needTenantTitle' | t }}</h3>
            <p>{{ 'invoices.needTenantBody' | t }}</p>
          </div>
        </div>
      } @else if (invoices.hasValue() && invoices.value()!.page.totalElements === 0) {
        <div class="empty">
          <bms-facade [units]="[]" [scale]="2" />
          <div>
            <h3>{{ 'invoices.emptyTitle' | t }}</h3>
            <p>{{ 'invoices.emptyBody' | t }}</p>
            <button class="primary" type="button" (click)="startCreate()">
              <bms-icon name="plus" />
              {{ 'invoices.add' | t }}
            </button>
          </div>
        </div>
      } @else if (invoices.hasValue()) {
        <div class="sheet-frame">
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
                <th><span class="visually-hidden">{{ 'common.actions' | t }}</span></th>
              </tr>
            </thead>
            <tbody>
              @for (invoice of invoices.value()!.content; track invoice.id) {
                <tr>
                  <td class="strong">{{ invoice.invoiceNumber }}</td>
                  <td>{{ invoice.tenantName }}</td>
                  <td class="muted">{{ invoice.buildingName }} - {{ invoice.apartmentLabel }}</td>
                  <td>{{ invoice.type | label: 'invoiceType' }}</td>
                  <td class="muted">{{ invoice.periodStart | day }} - {{ invoice.periodEnd | day }}</td>
                  <td class="muted">{{ invoice.dueDate | day }}</td>
                  <td class="right strong">{{ invoice.total | money: invoice.currency }}</td>
                  <td>
                    <span class="mark {{ invoice.status.toLowerCase() }}">
                      {{ invoice.status | label: 'invoiceStatus' }}
                    </span>
                  </td>
                  <td class="actions-cell">
                    <span class="row-actions">
                      @if (invoice.status === 'DRAFT') {
                        <button class="quiet" type="button" (click)="setStatus(invoice, 'SENT')">
                          <bms-icon name="send" [size]="16" />
                          {{ 'invoices.markSent' | t }}
                        </button>
                      }
                      @if (invoice.status === 'SENT') {
                        <button class="quiet" type="button" (click)="setStatus(invoice, 'PAID')">
                          <bms-icon name="paid" [size]="16" />
                          {{ 'invoices.markPaid' | t }}
                        </button>
                      }
                      <button bmsIconButton icon="download" [label]="'invoices.downloadPdf' | t" (click)="download(invoice)"></button>
                      @if (invoice.status === 'DRAFT') {
                        <button bmsIconButton class="danger" icon="trash" [label]="'common.delete' | t" (click)="remove(invoice)"></button>
                      }
                    </span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <bms-pager [page]="invoices.value()!.page" (go)="page.set($event)" />
      }

      @if (error()) {
        <p class="notice" role="alert"><bms-icon name="alert" />{{ error() }}</p>
      }
    </section>

    @if (editing()) {
      <bms-dialog [heading]="'invoices.add' | t" [error]="error()" (closed)="cancel()">
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

        <button type="button" (click)="addLine()">
          <bms-icon name="plus" [size]="16" />
          {{ 'invoices.addLine' | t }}
        </button>

        <div class="field notes-field">
          <label for="notes">{{ 'common.notes' | t }}</label>
          <input id="notes" [(ngModel)]="form.notes" />
        </div>
        <ng-container actions>
          <button type="button" (click)="cancel()">{{ 'common.cancel' | t }}</button>
          <button class="primary" type="button" [disabled]="!form.tenantId" (click)="save()">
            {{ 'invoices.add' | t }}
          </button>
        </ng-container>
      </bms-dialog>
    }
  `,
})
export class InvoicesPage {
  private api = inject(InvoicesApi);
  private buildingsApi = inject(BuildingsApi);
  private tenantsApi = inject(TenantsApi);
  private i18n = inject(TranslationService);
  private confirm = inject(ConfirmService);
  private toasts = inject(ToastService);

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

  /** Back to the first page whenever the filters change. */
  protected readonly page = linkedSignal({
    source: () => [this.filterBuilding(), this.filterStatus()],
    computation: () => 0,
  });

  protected readonly invoices = rxResource({
    params: () => ({ buildingId: this.filterBuilding(), status: this.filterStatus(), page: this.page() }),
    stream: ({ params }) =>
      this.api.search({ buildingId: params.buildingId || undefined, status: params.status }, params.page),
  });

  private readonly emptyPage = stepBackFromEmptyPage(this.page, this.invoices.value);

  protected hasTenants(): boolean {
    return this.tenants.value().length > 0;
  }

  protected startCreate(): void {
    this.error.set(null);
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
        this.toasts.show(this.i18n.translate('toast.invoiceCreated'));
        this.invoices.reload();
      },
      error: (response) =>
        this.error.set(response?.error?.detail ?? this.i18n.translate('invoices.createFailed')),
    });
  }

  protected setStatus(invoice: Invoice, status: InvoiceStatus): void {
    this.api.changeStatus(invoice.id, status).subscribe({
      next: () => {
        const label = this.i18n.translate(`enum.invoiceStatus.${status}` as MessageKey).toLowerCase();
        this.toasts.show(
          this.i18n.translate('toast.invoiceStatus', { number: invoice.invoiceNumber, status: label }),
        );
        this.invoices.reload();
      },
      error: (response) =>
        this.error.set(response?.error?.detail ?? this.i18n.translate('invoices.statusFailed')),
    });
  }

  protected async remove(invoice: Invoice): Promise<void> {
    if (!(await this.confirm.delete(invoice.invoiceNumber))) {
      return;
    }
    this.api.remove(invoice.id).subscribe({
      next: () => {
        this.toasts.show(this.i18n.translate('toast.deleted', { name: invoice.invoiceNumber }));
        this.invoices.reload();
      },
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
