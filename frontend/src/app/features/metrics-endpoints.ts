import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';

import { TranslationService } from '../core/i18n';
import { Endpoint } from '../core/models';
import { TranslatePipe } from '../shared/translate.pipe';
import { count } from './metrics-charts';

/** A ranked list of API routes, with how often they were called, failed, and how long they took. */
@Component({
  selector: 'bms-metrics-endpoints',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="metrics-panel">
      <h2>{{ title() }}</h2>
      @if (endpoints().length === 0) {
        <p class="muted">{{ 'metrics.noRequests' | t }}</p>
      } @else {
        <div class="sheet-frame">
          <table class="sheet">
            <thead>
              <tr>
                <th>{{ 'metrics.route' | t }}</th>
                <th class="right">{{ 'metrics.series.requests' | t }}</th>
                <th class="right">{{ 'metrics.errors' | t }}</th>
                <th class="right">{{ 'metrics.averageMs' | t }}</th>
                <th class="right">{{ 'metrics.maxMs' | t }}</th>
              </tr>
            </thead>
            <tbody>
              @for (endpoint of endpoints(); track endpoint.method + endpoint.route) {
                <tr>
                  <td>
                    <code>{{ endpoint.method }} {{ endpoint.route }}</code>
                  </td>
                  <td class="right">{{ number(endpoint.requests) }}</td>
                  <td class="right">{{ number(endpoint.clientErrors + endpoint.serverErrors) }}</td>
                  <td class="right">{{ number(endpoint.averageMs) }}</td>
                  <td class="right">{{ number(endpoint.maxMs) }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
})
export class MetricsEndpoints {
  readonly title = input.required<string>();
  readonly endpoints = input.required<Endpoint[]>();

  private i18n = inject(TranslationService);

  protected number(value: number): string {
    return count(value, this.i18n.locale());
  }
}
