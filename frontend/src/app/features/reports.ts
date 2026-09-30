import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { BuildingsApi, ReportsApi } from '../core/api';
import { Currency } from '../core/models';
import { MessageKey } from '../i18n/en';
import { Facade } from '../shared/facade';
import { Icon } from '../shared/icon';
import { DayPipe, LabelPipe, MoneyPipe } from '../shared/money.pipe';
import { TranslatePipe } from '../shared/translate.pipe';

@Component({
  selector: 'bms-reports',
  imports: [FormsModule, Facade, Icon, MoneyPipe, DayPipe, LabelPipe, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'reports.title' | t }}</h1>
          <p>{{ 'reports.subtitle' | t }}</p>
        </div>
        <button type="button" (click)="print()">
          <bms-icon name="print" [size]="16" />
          {{ 'reports.print' | t }}
        </button>
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
        <p class="notice" role="alert"><bms-icon name="alert" />{{ 'reports.error' | t }}</p>
      </section>
    } @else if (report.hasValue()) {
      <section class="band">
        <!-- One line per currency: buildings in different countries are never added up. -->
        <div class="figures" [class.several]="report.value()!.totals.length > 1">
          <div class="figure">
            <span class="caption">{{ 'reports.income' | t }}</span>
            @for (totals of report.value()!.totals; track totals.currency) {
              <span class="amount pos">{{ totals.income | money: totals.currency }}</span>
            }
          </div>
          <div class="figure">
            <span class="caption">{{ 'reports.costs' | t }}</span>
            @for (totals of report.value()!.totals; track totals.currency) {
              <span class="amount neg">{{ totals.expenses | money: totals.currency }}</span>
            }
          </div>
          <div class="figure">
            <span class="caption">
              {{ resultCaption() | t }},
              {{ report.value()!.from | day }} - {{ report.value()!.to | day }}
            </span>
            @for (totals of report.value()!.totals; track totals.currency) {
              <span class="amount" [class.pos]="totals.netResult >= 0" [class.neg]="totals.netResult < 0">
                {{ totals.netResult | money: totals.currency }}
              </span>
            }
          </div>
        </div>
      </section>

      <section class="band">
        <h2 class="section-title">{{ 'reports.byBuilding' | t }}</h2>
        @if (report.value()!.buildings.length === 0) {
          <div class="empty">
            <bms-facade [units]="[]" [scale]="2" />
            <div>
              <h3>{{ 'reports.emptyTitle' | t }}</h3>
              <p>{{ 'reports.emptyBody' | t }}</p>
            </div>
          </div>
        } @else {
          <div class="sheet-frame">
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
                    <td class="right pos">{{ row.income | money: row.currency }}</td>
                    <td class="right neg">{{ row.expenses | money: row.currency }}</td>
                    <td
                      class="right strong"
                      [class.pos]="row.netResult >= 0"
                      [class.neg]="row.netResult < 0"
                    >
                      {{ row.netResult | money: row.currency }}
                    </td>
                  </tr>
                }
              </tbody>
              <tfoot>
                @for (totals of report.value()!.totals; track totals.currency) {
                  <tr>
                    <td class="strong">{{ 'common.total' | t }}</td>
                    <td class="right strong pos">{{ totals.income | money: totals.currency }}</td>
                    <td class="right strong neg">{{ totals.expenses | money: totals.currency }}</td>
                    <td class="right strong">{{ totals.netResult | money: totals.currency }}</td>
                  </tr>
                }
              </tfoot>
            </table>
          </div>
        }
      </section>

      @if (report.value()!.expensesByCategory.length) {
        <section class="band">
          <h2 class="section-title">{{ 'reports.breakdown' | t }}</h2>
          <div class="sheet-frame">
            <table class="sheet">
              <thead>
                <tr>
                  <th>{{ 'reports.category' | t }}</th>
                  <th class="right">{{ 'common.amount' | t }}</th>
                  <th class="right">{{ 'reports.share' | t }}</th>
                </tr>
              </thead>
              <tbody>
                @for (row of report.value()!.expensesByCategory; track row.category + row.currency) {
                  <tr>
                    <td>{{ row.category | label: 'category' }}</td>
                    <td class="right">{{ row.amount | money: row.currency }}</td>
                    <td class="right muted">{{ share(row.amount, row.currency) }}%</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
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

  /** Profit or loss when every currency agrees; otherwise the neutral word. */
  protected readonly resultCaption = computed<MessageKey>(() => {
    const totals = this.report.value()?.totals ?? [];
    if (totals.every((each) => each.netResult >= 0)) {
      return 'reports.profit';
    }
    return totals.every((each) => each.netResult < 0) ? 'reports.loss' : 'reports.resultColumn';
  });

  /** A category's share of the costs in its own currency. */
  protected share(amount: number, currency: Currency): string {
    const total = this.report.value()?.totals.find((each) => each.currency === currency)?.expenses ?? 0;
    return total === 0 ? '0' : ((amount / total) * 100).toFixed(1);
  }

  protected print(): void {
    window.print();
  }
}
