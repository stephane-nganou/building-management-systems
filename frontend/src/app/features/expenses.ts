import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { ApartmentsApi, BuildingsApi, ExpensesApi } from '../core/api';
import { TranslationService } from '../core/i18n';
import { Expense, ExpenseCategory } from '../core/models';
import { ConfirmService } from '../shared/confirm';
import { Dialog } from '../shared/dialog';
import { Facade } from '../shared/facade';
import { Icon } from '../shared/icon';
import { IconButton } from '../shared/icon-button';
import { Pager, stepBackFromEmptyPage } from '../shared/pager';
import { DayPipe, LabelPipe, MoneyPipe, totalsByCurrency } from '../shared/money.pipe';
import { ToastService } from '../shared/toasts';
import { TranslatePipe } from '../shared/translate.pipe';

const CATEGORIES: ExpenseCategory[] = [
  'MAINTENANCE',
  'REPAIR',
  'UTILITIES',
  'INSURANCE',
  'TAX',
  'MANAGEMENT',
  'RENOVATION',
  'OTHER',
];

interface ExpenseForm {
  buildingId: string;
  apartmentId: string;
  category: ExpenseCategory;
  amount: number;
  incurredOn: string;
  description: string;
  vendor: string;
}

const blank = (buildingId: string): ExpenseForm => ({
  buildingId,
  apartmentId: '',
  category: 'MAINTENANCE',
  amount: 0,
  incurredOn: new Date().toISOString().slice(0, 10),
  description: '',
  vendor: '',
});

@Component({
  selector: 'bms-expenses',
  imports: [FormsModule, Dialog, Facade, Icon, IconButton, Pager, MoneyPipe, DayPipe, LabelPipe, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'expenses.title' | t }}</h1>
          <p>{{ 'expenses.subtitle' | t }}</p>
        </div>
        <button class="primary" type="button" [disabled]="!hasBuildings()" (click)="startCreate()">
          <bms-icon name="plus" />
          {{ 'expenses.add' | t }}
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
          <label for="from">{{ 'common.from' | t }}</label>
          <input id="from" type="date" [ngModel]="from()" (ngModelChange)="from.set($event)" />
        </div>
        <div class="field">
          <label for="to">{{ 'common.to' | t }}</label>
          <input id="to" type="date" [ngModel]="to()" (ngModelChange)="to.set($event)" />
        </div>
      </div>

      @if (expenses.isLoading()) {
        <p class="loading">{{ 'expenses.loading' | t }}</p>
      } @else if (!hasBuildings()) {
        <div class="empty">
          <bms-facade [units]="[]" [scale]="2" />
          <div>
            <h3>{{ 'expenses.needBuildingTitle' | t }}</h3>
            <p>{{ 'expenses.needBuildingBody' | t }}</p>
          </div>
        </div>
      } @else if (expenses.hasValue() && expenses.value()!.page.totalElements === 0) {
        <div class="empty">
          <bms-facade [units]="[]" [scale]="2" />
          <div>
            <h3>{{ 'expenses.emptyTitle' | t }}</h3>
            <p>{{ 'expenses.emptyBody' | t }}</p>
            <button class="primary" type="button" (click)="startCreate()">
              <bms-icon name="plus" />
              {{ 'expenses.add' | t }}
            </button>
          </div>
        </div>
      } @else if (expenses.hasValue()) {
        <div class="sheet-frame">
          <table class="sheet">
            <thead>
              <tr>
                <th>{{ 'common.date' | t }}</th>
                <th>{{ 'expenses.reason' | t }}</th>
                <th>{{ 'expenses.category' | t }}</th>
                <th>{{ 'common.building' | t }}</th>
                <th>{{ 'expenses.vendor' | t }}</th>
                <th class="right">{{ 'common.amount' | t }}</th>
                <th><span class="visually-hidden">{{ 'common.actions' | t }}</span></th>
              </tr>
            </thead>
            <tbody>
              @for (expense of expenses.value()!.content; track expense.id) {
                <tr>
                  <td class="muted">{{ expense.incurredOn | day }}</td>
                  <td class="strong">{{ expense.description }}</td>
                  <td>{{ expense.category | label: 'category' }}</td>
                  <td class="muted">
                    {{ expense.buildingName }}
                    @if (expense.apartmentLabel) {
                      - {{ expense.apartmentLabel }}
                    }
                  </td>
                  <td class="muted">{{ expense.vendor || '-' }}</td>
                  <td class="right strong neg">{{ expense.amount | money: expense.currency }}</td>
                  <td class="actions-cell">
                    <span class="row-actions">
                      <button bmsIconButton icon="edit" [label]="'common.edit' | t" (click)="startEdit(expense)"></button>
                      <button bmsIconButton class="danger" icon="trash" [label]="'common.delete' | t" (click)="remove(expense)"></button>
                    </span>
                  </td>
                </tr>
              }
            </tbody>
            <tfoot>
              @for (total of totals(); track total.currency) {
                <tr>
                  <td colspan="5" class="strong">{{ totalLabel() | t }}</td>
                  <td class="right strong neg">{{ total.amount | money: total.currency }}</td>
                  <td></td>
                </tr>
              }
            </tfoot>
          </table>
        </div>
        <bms-pager [page]="expenses.value()!.page" (go)="page.set($event)" />
      }

      @if (error()) {
        <p class="notice" role="alert"><bms-icon name="alert" />{{ error() }}</p>
      }
    </section>

    @if (editing()) {
      <bms-dialog [heading]="(editingId() ? 'expenses.editTitle' : 'expenses.add') | t" [error]="error()" (closed)="cancel()">
        <div class="grid-2">
          <div class="field">
            <label for="building">{{ 'common.building' | t }}</label>
            <select id="building" [(ngModel)]="form.buildingId" (ngModelChange)="form.apartmentId = ''">
              @for (building of buildings.value(); track building.id) {
                <option [value]="building.id">{{ building.name }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label for="apartment">{{ 'expenses.apartmentOptional' | t }}</label>
            <select id="apartment" [(ngModel)]="form.apartmentId">
              <option value="">{{ 'expenses.wholeBuilding' | t }}</option>
              @for (apartment of apartmentsFor(form.buildingId); track apartment.id) {
                <option [value]="apartment.id">{{ apartment.label }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label for="category">{{ 'expenses.category' | t }}</label>
            <select id="category" [(ngModel)]="form.category">
              @for (category of categories; track category) {
                <option [value]="category">{{ category | label: 'category' }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label for="amount">{{ 'common.amount' | t }}</label>
            <input id="amount" type="number" step="0.01" [(ngModel)]="form.amount" />
          </div>
          <div class="field">
            <label for="incurredOn">{{ 'common.date' | t }}</label>
            <input id="incurredOn" type="date" [(ngModel)]="form.incurredOn" />
          </div>
          <div class="field">
            <label for="vendor">{{ 'expenses.vendor' | t }}</label>
            <input id="vendor" [(ngModel)]="form.vendor" />
          </div>
        </div>
        <div class="field">
          <label for="description">{{ 'expenses.reason' | t }}</label>
          <input
            id="description"
            [(ngModel)]="form.description"
            [placeholder]="'expenses.reasonPlaceholder' | t"
          />
        </div>
        <ng-container actions>
          <button type="button" (click)="cancel()">{{ 'common.cancel' | t }}</button>
          <button
            class="primary"
            type="button"
            [disabled]="!form.description.trim()"
            (click)="save()"
          >
            {{ (editingId() ? 'common.saveChanges' : 'expenses.add') | t }}
          </button>
        </ng-container>
      </bms-dialog>
    }
  `,
})
export class ExpensesPage {
  private api = inject(ExpensesApi);
  private buildingsApi = inject(BuildingsApi);
  private apartmentsApi = inject(ApartmentsApi);
  private i18n = inject(TranslationService);
  private confirm = inject(ConfirmService);
  private toasts = inject(ToastService);

  protected readonly categories = CATEGORIES;
  protected readonly filterBuilding = signal('');
  protected readonly from = signal(`${new Date().getFullYear()}-01-01`);
  protected readonly to = signal(new Date().toISOString().slice(0, 10));
  protected readonly editing = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected form: ExpenseForm = blank('');

  protected readonly buildings = rxResource({
    stream: () => this.buildingsApi.list(),
    defaultValue: [],
  });

  protected readonly allApartments = rxResource({
    stream: () => this.apartmentsApi.list(),
    defaultValue: [],
  });

  /** Back to the first page whenever the filters change. */
  protected readonly page = linkedSignal({
    source: () => [this.filterBuilding(), this.from(), this.to()],
    computation: () => 0,
  });

  protected readonly expenses = rxResource({
    params: () => ({
      buildingId: this.filterBuilding(),
      from: this.from(),
      to: this.to(),
      page: this.page(),
    }),
    stream: ({ params }) =>
      this.api.search(
        { buildingId: params.buildingId || undefined, from: params.from, to: params.to },
        params.page,
      ),
  });

  private readonly emptyPage = stepBackFromEmptyPage(this.page, this.expenses.value);

  /**
   * One line per currency: buildings in different countries are never added up.
   * Only the page on screen is added; the profit and loss report covers a period.
   */
  protected readonly totals = computed(() =>
    totalsByCurrency(this.expenses.value()?.content ?? [], (expense) => expense.amount),
  );

  protected readonly totalLabel = computed(() =>
    (this.expenses.value()?.page.totalPages ?? 0) > 1 ? 'expenses.pageTotal' : 'common.total',
  );

  protected hasBuildings(): boolean {
    return this.buildings.value().length > 0;
  }

  protected apartmentsFor(buildingId: string) {
    return this.allApartments.value().filter((apartment) => apartment.buildingId === buildingId);
  }

  protected startCreate(): void {
    this.error.set(null);
    this.form = blank(this.filterBuilding() || this.buildings.value()[0]?.id || '');
    this.editingId.set(null);
    this.editing.set(true);
  }

  protected startEdit(expense: Expense): void {
    this.error.set(null);
    this.form = {
      buildingId: expense.buildingId,
      apartmentId: expense.apartmentId ?? '',
      category: expense.category,
      amount: expense.amount,
      incurredOn: expense.incurredOn,
      description: expense.description,
      vendor: expense.vendor ?? '',
    };
    this.editingId.set(expense.id);
    this.editing.set(true);
  }

  protected cancel(): void {
    this.editing.set(false);
  }

  protected save(): void {
    const body = { ...this.form, apartmentId: this.form.apartmentId || null };
    const id = this.editingId();
    const request = id ? this.api.update(id, body) : this.api.create(body);
    request.subscribe({
      next: (saved) => {
        this.editing.set(false);
        this.error.set(null);
        this.toasts.show(this.i18n.translate('toast.saved', { name: saved.description }));
        this.expenses.reload();
      },
      error: (response) =>
        this.error.set(response?.error?.detail ?? this.i18n.translate('expenses.saveFailed')),
    });
  }

  protected async remove(expense: Expense): Promise<void> {
    if (!(await this.confirm.delete(expense.description))) {
      return;
    }
    this.api.remove(expense.id).subscribe({
      next: () => {
        this.toasts.show(this.i18n.translate('toast.deleted', { name: expense.description }));
        this.expenses.reload();
      },
      error: (response) =>
        this.error.set(response?.error?.detail ?? this.i18n.translate('expenses.deleteFailed')),
    });
  }
}
