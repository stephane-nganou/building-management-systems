import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';

import { ApartmentsApi, BuildingsApi, ReportsApi } from '../core/api';
import { Currency } from '../core/models';
import { SessionService } from '../core/session';
import { Facade, Unit } from '../shared/facade';
import { Icon } from '../shared/icon';
import { MoneyPipe } from '../shared/money.pipe';
import { TranslatePipe } from '../shared/translate.pipe';

const today = new Date();
const startOfYear = `${today.getFullYear()}-01-01`;
const isoToday = today.toISOString().slice(0, 10);

interface Lot {
  id: string;
  name: string;
  units: Unit[];
  net: number;
  currency: Currency;
}

@Component({
  selector: 'bms-dashboard',
  imports: [RouterLink, Facade, Icon, MoneyPipe, TranslatePipe],
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
        <p class="notice" role="alert"><bms-icon name="alert" />{{ 'dashboard.error' | t }}</p>
      } @else if (summary.hasValue()) {
        @if (summary.value()!.buildingCount === 0) {
          <div class="empty">
            <bms-facade [units]="[]" [scale]="2" />
            <div>
              <h3>{{ 'dashboard.emptyTitle' | t }}</h3>
              <p>{{ 'dashboard.emptyBody' | t }}</p>
              <a class="btn primary" routerLink="/buildings">
                <bms-icon name="plus" />
                {{ 'dashboard.emptyAction' | t }}
              </a>
            </div>
          </div>
        } @else {
          @if (lots().length) {
            <div class="street night">
              <div class="street-head">
                <h2>{{ 'dashboard.street' | t }}</h2>
                <p>
                  {{
                    'dashboard.streetSubtitle'
                      | t: { occupied: summary.value()!.occupiedApartments, total: summary.value()!.apartmentCount }
                  }}
                </p>
              </div>
              <div class="lots">
                @for (lot of lots(); track lot.id) {
                  <div class="lot">
                    <bms-facade [units]="lot.units" [scale]="2.4" [animate]="true" />
                    <span class="lot-name">{{ lot.name }}</span>
                    <span class="lot-net" [class.pos]="lot.net > 0" [class.neg]="lot.net < 0">
                      {{ lot.net | money: lot.currency }}
                    </span>
                  </div>
                }
              </div>
            </div>
          }

          <!-- One line per currency in each figure: buildings in different countries are never added up. -->
          <div class="figures" [class.several]="summary.value()!.totals.length > 1">
            <div class="figure">
              <span class="caption">{{ 'dashboard.collected' | t }}</span>
              @for (totals of summary.value()!.totals; track totals.currency) {
                <span class="amount pos">{{ totals.yearToDateIncome | money: totals.currency }}</span>
              }
            </div>
            <div class="figure">
              <span class="caption">{{ 'dashboard.spent' | t }}</span>
              @for (totals of summary.value()!.totals; track totals.currency) {
                <span class="amount neg">{{ totals.yearToDateExpenses | money: totals.currency }}</span>
              }
            </div>
            <div class="figure">
              <span class="caption">{{ 'dashboard.net' | t }}</span>
              @for (totals of summary.value()!.totals; track totals.currency) {
                <span class="amount" [class.pos]="totals.yearToDateNet >= 0" [class.neg]="totals.yearToDateNet < 0">
                  {{ totals.yearToDateNet | money: totals.currency }}
                </span>
              }
            </div>
            <div class="figure">
              <span class="caption">{{ 'dashboard.rentRoll' | t }}</span>
              @for (totals of summary.value()!.totals; track totals.currency) {
                <span class="amount">{{ totals.monthlyRentRoll | money: totals.currency }}</span>
              }
            </div>
          </div>
        }
      }
    </section>

    @if (!seesStreet && report.hasValue() && report.value()!.buildings.length) {
      <section class="band">
        <div class="band-head">
          <div>
            <h2>{{ 'dashboard.byBuilding' | t }}</h2>
            <p>{{ 'dashboard.byBuildingSubtitle' | t }}</p>
          </div>
        </div>
        <div class="sheet-frame">
          <table class="sheet">
            <tbody>
              @for (row of report.value()!.buildings; track row.buildingId) {
                <tr>
                  <td class="strong">{{ row.buildingName }}</td>
                  <td class="right strong" [class.pos]="row.netResult >= 0" [class.neg]="row.netResult < 0">
                    {{ row.netResult | money: row.currency }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    }

    @if (summary.hasValue() && summary.value()!.buildingCount > 0) {
      <section class="band">
        <div class="band-head">
          <h2>{{ 'dashboard.portfolio' | t }}</h2>
        </div>
        <dl class="tallies">
          <div>
            <dt>{{ 'dashboard.buildings' | t }}</dt>
            <dd>{{ summary.value()!.buildingCount }}</dd>
          </div>
          <div>
            <dt>{{ 'dashboard.apartments' | t }}</dt>
            <dd>{{ summary.value()!.apartmentCount }}</dd>
          </div>
          <div>
            <dt>{{ 'dashboard.occupied' | t }}</dt>
            <dd>{{ summary.value()!.occupiedApartments }}</dd>
          </div>
          <div>
            <dt>{{ 'dashboard.vacant' | t }}</dt>
            <dd>{{ summary.value()!.vacantApartments }}</dd>
          </div>
          <div>
            <dt>{{ 'dashboard.activeTenants' | t }}</dt>
            <dd>{{ summary.value()!.activeTenants }}</dd>
          </div>
        </dl>
      </section>
    }
  `,
})
export class DashboardPage {
  private reports = inject(ReportsApi);
  private buildingsApi = inject(BuildingsApi);
  private apartmentsApi = inject(ApartmentsApi);
  private session = inject(SessionService);

  /** The street needs the buildings and their apartments, which not every assistant may read. */
  protected readonly seesStreet = this.session.can('BUILDING_READ') && this.session.can('APARTMENT_READ');

  protected readonly summary = rxResource({ stream: () => this.reports.summary() });

  protected readonly report = rxResource({
    stream: () => this.reports.profitLoss(startOfYear, isoToday),
  });

  private readonly buildings = rxResource({
    params: () => (this.seesStreet ? {} : undefined),
    stream: () => this.buildingsApi.list(),
    defaultValue: [],
  });

  private readonly apartments = rxResource({
    params: () => (this.seesStreet ? {} : undefined),
    stream: () => this.apartmentsApi.list(),
    defaultValue: [],
  });

  protected readonly lots = computed<Lot[]>(() => {
    const nets = new Map(
      (this.report.value()?.buildings ?? []).map((row) => [row.buildingId, row.netResult]),
    );
    const apartments = this.apartments.value();
    return this.buildings.value().map((building) => ({
      id: building.id,
      name: building.name,
      units: apartments.filter((apartment) => apartment.buildingId === building.id),
      net: nets.get(building.id) ?? 0,
      currency: building.currency,
    }));
  });
}
