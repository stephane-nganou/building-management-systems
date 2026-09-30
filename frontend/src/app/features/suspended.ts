import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { SubscriptionApi } from '../core/api';
import { AuthService } from '../core/auth';
import { Gate } from '../shared/gate';
import { SupportCard } from '../shared/support-card';
import { TranslatePipe } from '../shared/translate.pipe';

/**
 * All a suspended account is shown. The backend answers it nothing but its own
 * profile, so there is nothing else this page could offer.
 */
@Component({
  selector: 'bms-suspended',
  imports: [Gate, SupportCard, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <bms-gate>
      <div class="band-head">
        <div>
          <h1>{{ 'suspended.title' | t }}</h1>
          <p>{{ 'suspended.body' | t }}</p>
        </div>
      </div>
      @if (support.hasValue()) {
        <bms-support-card [contacts]="support.value()!" />
      }
      <div class="gate-actions">
        <button class="quiet" type="button" (click)="signOut()">{{ 'app.signOut' | t }}</button>
      </div>
    </bms-gate>
  `,
})
export class SuspendedPage {
  private auth = inject(AuthService);
  private api = inject(SubscriptionApi);

  /** One of the few things a suspended account is still answered. */
  protected readonly support = rxResource({ stream: () => this.api.support() });

  protected signOut(): void {
    this.auth.signOut();
  }
}
