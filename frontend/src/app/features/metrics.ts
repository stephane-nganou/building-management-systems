import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { AdminApi } from '../core/api';
import { TranslationService } from '../core/i18n';
import { METRICS_RANGES, MetricsRange, Split } from '../core/models';
import { MessageKey } from '../i18n/en';
import { ChartView } from '../shared/chart';
import { Icon } from '../shared/icon';
import { TranslatePipe } from '../shared/translate.pipe';
import { conversionRate, count, errorRate, panels, total } from './metrics-charts';
import { MetricsEndpoints } from './metrics-endpoints';

/**
 * How the service is used: who signs in and comes back, where the customers
 * stand, what they create, and how the API holds up. For administrators only.
 */
@Component({
  selector: 'bms-metrics',
  imports: [ChartView, Icon, MetricsEndpoints, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head metrics-head">
        <div>
          <h1>{{ 'metrics.title' | t }}</h1>
          <p>{{ 'metrics.subtitle' | t }}</p>
        </div>
        <div class="languages" role="group" [attr.aria-label]="'metrics.range' | t">
          @for (days of ranges; track days) {
            <button
              type="button"
              class="language"
              [class.on]="range() === days"
              [attr.aria-pressed]="range() === days"
              (click)="range.set(days)"
            >
              {{ 'metrics.days' | t: { days } }}
            </button>
          }
        </div>
      </div>

      @if (metrics.error()) {
        <p class="notice" role="alert">
          <bms-icon name="alert" [size]="16" />
          {{ 'metrics.failed' | t }}
        </p>
      } @else if (!metrics.hasValue()) {
        <p class="loading">{{ 'metrics.loading' | t }}</p>
      } @else {
        @let m = metrics.value()!;
        <div class="metrics-kpis" [attr.aria-busy]="metrics.isLoading()">
          @for (kpi of activity(); track kpi.label) {
            <div class="figure">
              <span class="caption">{{ kpi.label | t }}</span>
              <span class="amount">{{ number(kpi.split.owners + kpi.split.assistants) }}</span>
              <span class="split">
                {{
                  'metrics.ownersAssistants'
                    | t
                      : {
                          owners: number(kpi.split.owners),
                          assistants: number(kpi.split.assistants),
                        }
                }}
              </span>
            </div>
          }
          <div class="figure">
            <span class="caption">{{ 'metrics.kpi.newOwners' | t }}</span>
            <span class="amount">{{ number(newOwners()) }}</span>
          </div>
          <div class="figure">
            <span class="caption">{{ 'metrics.kpi.conversion' | t }}</span>
            <span class="amount">{{ percent(conversion()) }}</span>
            <span class="split">
              {{
                'metrics.trials'
                  | t
                    : {
                        converted: number(m.customers.trialsConverted),
                        started: number(m.customers.trialsStarted),
                      }
              }}
            </span>
          </div>
          <div class="figure">
            <span class="caption">{{ 'metrics.kpi.requests' | t }}</span>
            <span class="amount">{{ number(m.traffic.requests) }}</span>
            <span class="split">{{ 'metrics.errorRate' | t: { rate: percent(errors()) } }}</span>
          </div>
        </div>

        <div class="metrics-grid">
          @for (panel of panels(); track panel.title) {
            <section class="metrics-panel">
              <h2>{{ panel.title | t }}</h2>
              <bms-chart [spec]="panel.spec" [label]="panel.title | t" />
              <details class="chart-data">
                <summary>{{ 'metrics.showNumbers' | t }}</summary>
                <div class="sheet-frame">
                  <table class="sheet">
                    <thead>
                      <tr>
                        <th>
                          <span class="visually-hidden">{{ panel.title | t }}</span>
                        </th>
                        @for (series of panel.spec.series; track series.label) {
                          <th class="right">{{ series.label }}</th>
                        }
                      </tr>
                    </thead>
                    <tbody>
                      @for (label of panel.spec.labels; track $index; let row = $index) {
                        <tr>
                          <td>{{ label }}</td>
                          @for (series of panel.spec.series; track series.label) {
                            <td class="right">{{ number(series.data[row]) }}</td>
                          }
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              </details>
            </section>
          }
        </div>

        <div class="metrics-tables">
          <bms-metrics-endpoints [title]="'metrics.busiest' | t" [endpoints]="m.traffic.busiest" />
          <bms-metrics-endpoints [title]="'metrics.slowest' | t" [endpoints]="m.traffic.slowest" />
        </div>
      }
    </section>
  `,
})
export class MetricsPage {
  private api = inject(AdminApi);
  private i18n = inject(TranslationService);

  protected readonly ranges = METRICS_RANGES;
  protected readonly range = signal<MetricsRange>(30);

  protected readonly metrics = rxResource({
    params: () => this.range(),
    stream: ({ params }) => this.api.metrics(params),
  });

  protected readonly panels = computed(() =>
    panels(this.metrics.value()!, (key) => this.i18n.translate(key), this.i18n.locale()),
  );

  /** Active today, over the last 7 days and over the last 30, whatever the range. */
  protected readonly activity = computed((): { label: MessageKey; split: Split }[] => {
    const activity = this.metrics.value()!.activity;
    return [
      { label: 'metrics.kpi.dau', split: activity.dau },
      { label: 'metrics.kpi.wau', split: activity.wau },
      { label: 'metrics.kpi.mau', split: activity.mau },
    ];
  });

  protected readonly newOwners = computed(() => total(this.metrics.value()!.customers.newOwners));
  protected readonly conversion = computed(() => conversionRate(this.metrics.value()!));
  protected readonly errors = computed(() => errorRate(this.metrics.value()!));

  protected number(value: number): string {
    return count(value, this.i18n.locale());
  }

  /** A share, or a dash while there is nothing to take it of. */
  protected percent(value: number | null): string {
    return value === null
      ? '–'
      : new Intl.NumberFormat(this.i18n.locale(), {
          style: 'percent',
          maximumFractionDigits: 1,
        }).format(value);
  }
}
