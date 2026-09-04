import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';

import { ReportsApi } from '../core/api';
import { MoneyPipe } from '../shared/money.pipe';
import { TranslatePipe } from '../shared/translate.pipe';

const today = new Date();
const startOfYear = `${today.getFullYear()}-01-01`;
const isoToday = today.toISOString().slice(0, 10);

@Component({
  selector: 'bms-dashboard',
  imports: [RouterLink, MoneyPipe, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'dashboard.title' | t }}</h1>
          <p>{{ 'dashboard.subtitle' | t }}</p>
        </div>
      </div>

      @if (summary.isLoading()) {
        <p class="loading">{{ 'dashboard.loading' | t }}</p>
      } @else if (summary.error()) {
        <p class="notice">{{ 'dashboard.error' | t }}</p>
      } @else if (summary.hasValue()) {
        <div class="figures">
          <div class="figure">
            <span class="amount pos">{{ summary.value()!.yearToDateIncome | money }}</span>
            <span class="caption">{{ 'dashboard.collected' | t }}</span>
          </div>
          <div class="figure">
            <span class="amount neg">{{ summary.value()!.yearToDateExpenses | money }}</span>
            <span class="caption">{{ 'dashboard.spent' | t }}</span>
          </div>
          <div class="figure">
            <span class="amount" [class.pos]="net() >= 0" [class.neg]="net() < 0">
              {{ net() | money }}
            </span>
            <span class="caption">{{ 'dashboard.net' | t }}</span>
          </div>
          <div class="figure">
            <span class="amount">{{ summary.value()!.monthlyRentRoll | money }}</span>
            <span class="caption">{{ 'dashboard.rentRoll' | t }}</span>
          </div>
        </div>
      }
    </section>

    @if (report.hasValue() && report.value()!.buildings.length) {
      <section class="band">
        <div class="band-head">
          <div>
            <h2>{{ 'dashboard.byBuilding' | t }}</h2>
            <p>{{ 'dashboard.byBuildingSubtitle' | t }}</p>
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
          <h2>{{ 'dashboard.portfolio' | t }}</h2>
        </div>
      </div>

      @if (summary.hasValue()) {
        <table class="sheet">
          <tbody>
            <tr>
              <td>{{ 'dashboard.buildings' | t }}</td>
              <td class="right strong">{{ summary.value()!.buildingCount }}</td>
            </tr>
            <tr>
              <td>{{ 'dashboard.apartments' | t }}</td>
              <td class="right strong">{{ summary.value()!.apartmentCount }}</td>
            </tr>
            <tr>
              <td>{{ 'dashboard.occupied' | t }}</td>
              <td class="right strong">{{ summary.value()!.occupiedApartments }}</td>
            </tr>
            <tr>
              <td>{{ 'dashboard.vacant' | t }}</td>
              <td class="right strong">{{ summary.value()!.vacantApartments }}</td>
            </tr>
            <tr>
              <td>{{ 'dashboard.activeTenants' | t }}</td>
              <td class="right strong">{{ summary.value()!.activeTenants }}</td>
            </tr>
          </tbody>
        </table>

        @if (summary.value()!.buildingCount === 0) {
          <div class="empty">
            <h3>{{ 'dashboard.emptyTitle' | t }}</h3>
            <p>{{ 'dashboard.emptyBody' | t }}</p>
            <a class="btn btn-primary" routerLink="/buildings">
              {{ 'dashboard.emptyAction' | t }}
            </a>
          </div>
        }
      }
    </section>
  `,
})
export class DashboardPage {
  private reports = inject(ReportsApi);

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
