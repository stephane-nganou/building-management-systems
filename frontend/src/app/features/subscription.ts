import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { SubscriptionApi } from '../core/api';
import { DayPipe, LabelPipe } from '../shared/money.pipe';
import { SupportCard } from '../shared/support-card';
import { TranslatePipe } from '../shared/translate.pipe';

/**
 * Manage subscription: where an owner stands, every period they have had or
 * have booked, and whom to contact to renew. Renewing is done by customer
 * service, so the screen informs rather than acts.
 */
@Component({
  selector: 'bms-subscription',
  imports: [DayPipe, LabelPipe, SupportCard, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'subscription.title' | t }}</h1>
          <p>{{ 'subscription.subtitle' | t }}</p>
        </div>
      </div>

      @if (subscription.isLoading()) {
        <p class="loading">{{ 'subscription.loading' | t }}</p>
      } @else if (subscription.hasValue()) {
        @let mine = subscription.value()!;
        <div class="figures">
          <div class="figure">
            <span class="caption">{{ 'common.status' | t }}</span>
            <span class="mark {{ mine.standing.status.toLowerCase() }}">
              {{ mine.standing.status | label: 'subscriptionStatus' }}
            </span>
          </div>
          <div class="figure">
            <span class="caption">
              {{ (mine.standing.status === 'EXPIRED' ? 'subscription.ended' : 'subscription.runsUntil') | t }}
            </span>
            <span class="amount">{{ mine.standing.endsOn | day }}</span>
          </div>
          @if (mine.standing.daysLeft !== null) {
            <div class="figure">
              <span class="caption">{{ 'subscription.daysLeft' | t }}</span>
              <span class="amount">{{ mine.standing.daysLeft }}</span>
            </div>
          }
        </div>

        <h2 class="gap-above">{{ 'subscription.history' | t }}</h2>
        <div class="sheet-frame">
          <table class="sheet">
            <thead>
              <tr>
                <th>{{ 'common.from' | t }}</th>
                <th>{{ 'common.to' | t }}</th>
                <th>{{ 'common.notes' | t }}</th>
              </tr>
            </thead>
            <tbody>
              @for (period of mine.periods; track period.id) {
                <tr>
                  <td>{{ period.startsOn | day }}</td>
                  <td>{{ period.endsOn | day }}</td>
                  <td class="muted">{{ period.note }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        <p class="muted gap-above">{{ 'subscription.renewHint' | t }}</p>
        <bms-support-card [contacts]="mine.support" />
      }
    </section>
  `,
})
export class SubscriptionPage {
  private api = inject(SubscriptionApi);

  protected readonly subscription = rxResource({ stream: () => this.api.mine() });
}
