import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { AuthService } from '../core/auth';
import { Gate } from '../shared/gate';
import { TranslatePipe } from '../shared/translate.pipe';

/**
 * All a suspended account is shown. The backend answers it nothing but its own
 * profile, so there is nothing else this page could offer.
 */
@Component({
  selector: 'bms-suspended',
  imports: [Gate, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <bms-gate>
      <div class="band-head">
        <div>
          <h1>{{ 'suspended.title' | t }}</h1>
          <p>{{ 'suspended.body' | t }}</p>
        </div>
      </div>
      <div class="gate-actions">
        <button class="quiet" type="button" (click)="signOut()">{{ 'app.signOut' | t }}</button>
      </div>
    </bms-gate>
  `,
})
export class SuspendedPage {
  private auth = inject(AuthService);

  protected signOut(): void {
    this.auth.signOut();
  }
}
