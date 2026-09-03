import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { ApartmentsApi, BuildingsApi, ExpensesApi } from '../core/api';
import { Expense, ExpenseCategory } from '../core/models';
import { DayPipe, LabelPipe, MoneyPipe } from '../shared/money.pipe';

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
  imports: [FormsModule, MoneyPipe, DayPipe, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>Expenses</h1>
          <p>What each building costs to run, and why. These offset your rental income.</p>
        </div>
        <button class="primary" type="button" [disabled]="!hasBuildings()" (click)="startCreate()">
          Record expense
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
          <label for="from">From</label>
          <input id="from" type="date" [ngModel]="from()" (ngModelChange)="from.set($event)" />
        </div>
        <div class="field">
          <label for="to">To</label>
          <input id="to" type="date" [ngModel]="to()" (ngModelChange)="to.set($event)" />
        </div>
      </div>

      @if (expenses.isLoading()) {
        <p class="loading">Loading expenses.</p>
      } @else if (!hasBuildings()) {
        <div class="empty">
          <h3>Add a building first</h3>
          <p>Expenses are always booked against a building.</p>
        </div>
      } @else if (expenses.hasValue() && expenses.value()!.length === 0) {
        <div class="empty">
          <h3>No expenses in this period</h3>
          <p>Record repairs, insurance and running costs so your profit and loss is accurate.</p>
          <button class="primary" type="button" (click)="startCreate()">Record expense</button>
        </div>
      } @else if (expenses.hasValue()) {
        <table class="sheet">
          <thead>
            <tr>
              <th>Date</th>
              <th>Reason</th>
              <th>Category</th>
              <th>Building</th>
              <th>Paid to</th>
              <th class="right">Amount</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (expense of expenses.value(); track expense.id) {
              <tr>
                <td class="muted">{{ expense.incurredOn | day }}</td>
                <td class="strong">{{ expense.description }}</td>
                <td>{{ expense.category | label }}</td>
                <td class="muted">
                  {{ expense.buildingName }}
                  @if (expense.apartmentLabel) {
                    - {{ expense.apartmentLabel }}
                  }
                </td>
                <td class="muted">{{ expense.vendor || '-' }}</td>
                <td class="right strong neg">{{ expense.amount | money }}</td>
                <td class="right">
                  <button class="quiet" type="button" (click)="startEdit(expense)">Edit</button>
                  <button class="quiet danger" type="button" (click)="remove(expense)">Delete</button>
                </td>
              </tr>
            }
          </tbody>
          <tfoot>
            <tr>
              <td colspan="5" class="strong">Total</td>
              <td class="right strong neg">{{ total() | money }}</td>
              <td></td>
            </tr>
          </tfoot>
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
            <h2>{{ editingId() ? 'Edit expense' : 'Record expense' }}</h2>
          </header>
          <div class="body">
            <div class="grid-2">
              <div class="field">
                <label for="building">Building</label>
                <select id="building" [(ngModel)]="form.buildingId" (ngModelChange)="form.apartmentId = ''">
                  @for (building of buildings.value(); track building.id) {
                    <option [value]="building.id">{{ building.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label for="apartment">Apartment (optional)</label>
                <select id="apartment" [(ngModel)]="form.apartmentId">
                  <option value="">Whole building</option>
                  @for (apartment of apartmentsFor(form.buildingId); track apartment.id) {
                    <option [value]="apartment.id">{{ apartment.label }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label for="category">Category</label>
                <select id="category" [(ngModel)]="form.category">
                  @for (category of categories; track category) {
                    <option [value]="category">{{ category | label }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label for="amount">Amount</label>
                <input id="amount" type="number" step="0.01" [(ngModel)]="form.amount" />
              </div>
              <div class="field">
                <label for="incurredOn">Date</label>
                <input id="incurredOn" type="date" [(ngModel)]="form.incurredOn" />
              </div>
              <div class="field">
                <label for="vendor">Paid to</label>
                <input id="vendor" [(ngModel)]="form.vendor" />
              </div>
            </div>
            <div class="field">
              <label for="description">Reason</label>
              <input id="description" [(ngModel)]="form.description" placeholder="Replaced boiler valve" />
            </div>
          </div>
          <footer>
            <button type="button" (click)="cancel()">Cancel</button>
            <button
              class="primary"
              type="button"
              [disabled]="!form.description.trim()"
              (click)="save()"
            >
              {{ editingId() ? 'Save changes' : 'Record expense' }}
            </button>
          </footer>
        </div>
      </div>
    }
  `,
})
export class ExpensesPage {
  private api = inject(ExpensesApi);
  private buildingsApi = inject(BuildingsApi);
  private apartmentsApi = inject(ApartmentsApi);

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

  protected readonly expenses = rxResource({
    params: () => ({ buildingId: this.filterBuilding(), from: this.from(), to: this.to() }),
    stream: ({ params }) =>
      this.api.search({
        buildingId: params.buildingId || undefined,
        from: params.from,
        to: params.to,
      }),
  });

  protected readonly total = computed(() =>
    (this.expenses.value() ?? []).reduce((sum, expense) => sum + expense.amount, 0),
  );

  protected hasBuildings(): boolean {
    return this.buildings.value().length > 0;
  }

  protected apartmentsFor(buildingId: string) {
    return this.allApartments.value().filter((apartment) => apartment.buildingId === buildingId);
  }

  protected startCreate(): void {
    this.form = blank(this.filterBuilding() || this.buildings.value()[0]?.id || '');
    this.editingId.set(null);
    this.editing.set(true);
  }

  protected startEdit(expense: Expense): void {
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
      next: () => {
        this.editing.set(false);
        this.error.set(null);
        this.expenses.reload();
      },
      error: (response) =>
        this.error.set(response?.error?.detail ?? 'The expense could not be saved.'),
    });
  }

  protected remove(expense: Expense): void {
    this.api.remove(expense.id).subscribe({
      next: () => this.expenses.reload(),
      error: () => this.error.set('The expense could not be deleted.'),
    });
  }
}
