import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { BuildingsApi, ReportsApi } from '../core/api';
import { DayPipe, LabelPipe, MoneyPipe } from '../shared/money.pipe';
import { TranslatePipe } from '../shared/translate.pipe';

@Component({
  selector: 'bms-reports',
  imports: [FormsModule, MoneyPipe, DayPipe, LabelPipe, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'reports.title' | t }}</h1>
          <p>{{ 'reports.subtitle' | t }}</p>
        </div>
        <button type="button" (click)="print()">{{ 'reports.print' | t }}</button>
      </div>

      <div class="toolbar">
        <div class="field">
          <label for="from">{{ 'common.from' | t }}</label>
          <input id="from" type="date" [ngModel]="from()" (ngModelChange)="from.set($event)" />
        </div>
        <div class="field">
          <label for="to">{{ 'common.to' | t }}</label>
          <input id="to" type="date" [ngModel]="to()" (ngModelChange)="to.set($event)" />
        </div>
        <div class="field">
          <label for="building">{{ 'common.building' | t }}</label>
          <select id="building" [ngModel]="buildingId()" (ngModelChange)="buildingId.set($event)">
            <option value="">{{ 'common.allBuildings' | t }}</option>
            @for (building of buildings.value(); track building.id) {
              <option [value]="building.id">{{ building.name }}</option>
            }
          </select>
        </div>
      </div>
    </section>

    @if (report.isLoading()) {
      <section class="band"><p class="loading">{{ 'reports.loading' | t }}</p></section>
    } @else if (report.error()) {
      <section class="band">
        <p class="notice">{{ 'reports.error' | t }}</p>
      </section>
    } @else if (report.hasValue()) {
      <section class="band">
        <div class="figures">
          <div class="figure">
            <span class="amount pos">{{ report.value()!.totalIncome | money }}</span>
            <span class="caption">{{ 'reports.income' | t }}</span>
          </div>
          <div class="figure">
            <span class="amount neg">{{ report.value()!.totalExpenses | money }}</span>
            <span class="caption">{{ 'reports.costs' | t }}</span>
          </div>
          <div class="figure">
            <span
              class="amount"
              [class.pos]="report.value()!.netResult >= 0"
              [class.neg]="report.value()!.netResult < 0"
            >
              {{ report.value()!.netResult | money }}
            </span>
            <span class="caption">
              {{ (report.value()!.netResult >= 0 ? 'reports.profit' : 'reports.loss') | t }},
              {{ report.value()!.from | day }} - {{ report.value()!.to | day }}
            </span>
          </div>
        </div>
      </section>

      <section class="band">
        <h2>{{ 'reports.byBuilding' | t }}</h2>
        @if (report.value()!.buildings.length === 0) {
          <div class="empty">
            <h3>{{ 'reports.emptyTitle' | t }}</h3>
            <p>{{ 'reports.emptyBody' | t }}</p>
          </div>
        } @else {
          <table class="sheet">
            <thead>
              <tr>
                <th>{{ 'common.building' | t }}</th>
                <th class="right">{{ 'reports.incomeColumn' | t }}</th>
                <th class="right">{{ 'reports.expensesColumn' | t }}</th>
                <th class="right">{{ 'reports.resultColumn' | t }}</th>
              </tr>
            </thead>
            <tbody>
              @for (row of report.value()!.buildings; track row.buildingId) {
                <tr>
                  <td class="strong">{{ row.buildingName }}</td>
                  <td class="right pos">{{ row.income | money }}</td>
                  <td class="right neg">{{ row.expenses | money }}</td>
                  <td
                    class="right strong"
                    [class.pos]="row.netResult >= 0"
                    [class.neg]="row.netResult < 0"
                  >
                    {{ row.netResult | money }}
                  </td>
                </tr>
              }
            </tbody>
            <tfoot>
              <tr>
                <td class="strong">{{ 'common.total' | t }}</td>
                <td class="right strong pos">{{ report.value()!.totalIncome | money }}</td>
                <td class="right strong neg">{{ report.value()!.totalExpenses | money }}</td>
                <td class="right strong">{{ report.value()!.netResult | money }}</td>
              </tr>
            </tfoot>
          </table>
        }
      </section>

      @if (report.value()!.expensesByCategory.length) {
        <section class="band">
          <h2>{{ 'reports.breakdown' | t }}</h2>
          <table class="sheet">
            <thead>
              <tr>
                <th>{{ 'reports.category' | t }}</th>
                <th class="right">{{ 'common.amount' | t }}</th>
                <th class="right">{{ 'reports.share' | t }}</th>
              </tr>
            </thead>
            <tbody>
              @for (row of report.value()!.expensesByCategory; track row.category) {
                <tr>
                  <td>{{ row.category | label: 'category' }}</td>
                  <td class="right">{{ row.amount | money }}</td>
                  <td class="right muted">{{ share(row.amount) }}%</td>
                </tr>
              }
            </tbody>
          </table>
        </section>
      }
    }
  `,
})
export class ReportsPage {
  private api = inject(ReportsApi);
  private buildingsApi = inject(BuildingsApi);

  protected readonly from = signal(`${new Date().getFullYear()}-01-01`);
  protected readonly to = signal(new Date().toISOString().slice(0, 10));
  protected readonly buildingId = signal('');

  protected readonly buildings = rxResource({
    stream: () => this.buildingsApi.list(),
    defaultValue: [],
  });

  protected readonly report = rxResource({
    params: () => ({ from: this.from(), to: this.to(), buildingId: this.buildingId() }),
    stream: ({ params }) =>
      this.api.profitLoss(params.from, params.to, params.buildingId || undefined),
  });

  private readonly totalExpenses = computed(() => this.report.value()?.totalExpenses ?? 0);

  protected share(amount: number): string {
    const total = this.totalExpenses();
    return total === 0 ? '0' : ((amount / total) * 100).toFixed(1);
  }

  protected print(): void {
    window.print();
  }
}
