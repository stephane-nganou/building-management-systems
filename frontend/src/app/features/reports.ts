import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { BuildingsApi, ReportsApi } from '../core/api';
import { DayPipe, LabelPipe, MoneyPipe } from '../shared/money.pipe';

@Component({
  selector: 'bms-reports',
  imports: [FormsModule, MoneyPipe, DayPipe, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>Profit and loss</h1>
          <p>
            Rent invoiced against costs incurred over a period. Use this for your tax declaration.
          </p>
        </div>
        <button type="button" (click)="print()">Print</button>
      </div>

      <div class="toolbar">
        <div class="field">
          <label for="from">From</label>
          <input id="from" type="date" [ngModel]="from()" (ngModelChange)="from.set($event)" />
        </div>
        <div class="field">
          <label for="to">To</label>
          <input id="to" type="date" [ngModel]="to()" (ngModelChange)="to.set($event)" />
        </div>
        <div class="field">
          <label for="building">Building</label>
          <select id="building" [ngModel]="buildingId()" (ngModelChange)="buildingId.set($event)">
            <option value="">All buildings</option>
            @for (building of buildings.value(); track building.id) {
              <option [value]="building.id">{{ building.name }}</option>
            }
          </select>
        </div>
      </div>
    </section>

    @if (report.isLoading()) {
      <section class="band"><p class="loading">Calculating.</p></section>
    } @else if (report.error()) {
      <section class="band">
        <p class="notice">That period could not be calculated. Check the dates are the right way round.</p>
      </section>
    } @else if (report.hasValue()) {
      <section class="band">
        <div class="figures">
          <div class="figure">
            <span class="amount pos">{{ report.value()!.totalIncome | money }}</span>
            <span class="caption">Income invoiced</span>
          </div>
          <div class="figure">
            <span class="amount neg">{{ report.value()!.totalExpenses | money }}</span>
            <span class="caption">Costs incurred</span>
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
              {{ report.value()!.netResult >= 0 ? 'Profit' : 'Loss' }} for
              {{ report.value()!.from | day }} to {{ report.value()!.to | day }}
            </span>
          </div>
        </div>
      </section>

      <section class="band">
        <h2>By building</h2>
        @if (report.value()!.buildings.length === 0) {
          <div class="empty">
            <h3>Nothing booked in this period</h3>
            <p>Record expenses, or mark invoices as sent, and they appear here.</p>
          </div>
        } @else {
          <table class="sheet">
            <thead>
              <tr>
                <th>Building</th>
                <th class="right">Income</th>
                <th class="right">Expenses</th>
                <th class="right">Result</th>
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
                <td class="strong">Total</td>
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
          <h2>Where the money went</h2>
          <table class="sheet">
            <thead>
              <tr>
                <th>Category</th>
                <th class="right">Amount</th>
                <th class="right">Share</th>
              </tr>
            </thead>
            <tbody>
              @for (row of report.value()!.expensesByCategory; track row.category) {
                <tr>
                  <td>{{ row.category | label }}</td>
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
