import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';

import { ReportsApi } from '../core/api';
import { MoneyPipe } from '../shared/money.pipe';

const today = new Date();
const startOfYear = `${today.getFullYear()}-01-01`;
const isoToday = today.toISOString().slice(0, 10);

@Component({
  selector: 'bms-dashboard',
  imports: [RouterLink, MoneyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>Overview</h1>
          <p>Where the portfolio stands this year, {{ periodLabel }}.</p>
        </div>
      </div>

      @if (summary.isLoading()) {
        <p class="loading">Loading the ledger.</p>
      } @else if (summary.error()) {
        <p class="notice">The overview could not be loaded. Check that the backend is running.</p>
      } @else if (summary.hasValue()) {
        <div class="figures">
          <div class="figure">
            <span class="amount pos">{{ summary.value()!.yearToDateIncome | money }}</span>
            <span class="caption">Collected</span>
          </div>
          <div class="figure">
            <span class="amount neg">{{ summary.value()!.yearToDateExpenses | money }}</span>
            <span class="caption">Spent</span>
          </div>
          <div class="figure">
            <span class="amount" [class.pos]="net() >= 0" [class.neg]="net() < 0">
              {{ net() | money }}
            </span>
            <span class="caption">Net result</span>
          </div>
          <div class="figure">
            <span class="amount">{{ summary.value()!.monthlyRentRoll | money }}</span>
            <span class="caption">Rent roll each month</span>
          </div>
        </div>
      }
    </section>

    @if (report.hasValue() && report.value()!.buildings.length) {
      <section class="band">
        <div class="band-head">
          <div>
            <h2>Position by building</h2>
            <p>Rent collected against what each building cost to run.</p>
          </div>
        </div>

        <div class="elevation">
          @for (row of report.value()!.buildings; track row.buildingId) {
            <div class="elevation-row">
              <span class="who">{{ row.buildingName }}</span>
              <span class="bars">
                <span class="bar income" [style.width.%]="width(row.income)"></span>
                <span class="bar expense" [style.width.%]="width(row.expenses)"></span>
              </span>
              <span class="right strong" [class.pos]="row.netResult >= 0" [class.neg]="row.netResult < 0">
                {{ row.netResult | money }}
              </span>
            </div>
          }
        </div>
      </section>
    }

    <section class="band">
      <div class="band-head">
        <div>
          <h2>Portfolio</h2>
        </div>
      </div>

      @if (summary.hasValue()) {
        <table class="sheet">
          <tbody>
            <tr>
              <td>Buildings</td>
              <td class="right strong">{{ summary.value()!.buildingCount }}</td>
            </tr>
            <tr>
              <td>Apartments</td>
              <td class="right strong">{{ summary.value()!.apartmentCount }}</td>
            </tr>
            <tr>
              <td>Occupied</td>
              <td class="right strong">{{ summary.value()!.occupiedApartments }}</td>
            </tr>
            <tr>
              <td>Vacant</td>
              <td class="right strong">{{ summary.value()!.vacantApartments }}</td>
            </tr>
            <tr>
              <td>Active tenants</td>
              <td class="right strong">{{ summary.value()!.activeTenants }}</td>
            </tr>
          </tbody>
        </table>

        @if (summary.value()!.buildingCount === 0) {
          <div class="empty">
            <h3>Nothing to show yet</h3>
            <p>Add your first building, then its apartments, and the numbers here fill in.</p>
            <a class="btn btn-primary" routerLink="/buildings">Add a building</a>
          </div>
        }
      }
    </section>
  `,
})
export class DashboardPage {
  private reports = inject(ReportsApi);

  protected readonly periodLabel = `1 January to today`;

  protected readonly summary = rxResource({ stream: () => this.reports.summary() });

  protected readonly report = rxResource({
    stream: () => this.reports.profitLoss(startOfYear, isoToday),
  });

  protected readonly net = computed(() => this.summary.value()?.yearToDateNet ?? 0);

  /** Bars share one scale so buildings stay comparable against each other. */
  private readonly peak = computed(() => {
    const rows = this.report.value()?.buildings ?? [];
    return Math.max(1, ...rows.flatMap((row) => [row.income, row.expenses]));
  });

  protected width(value: number): number {
    return Math.max(1, (value / this.peak()) * 100);
  }
}
