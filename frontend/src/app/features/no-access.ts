import { ChangeDetectionStrategy, Component } from '@angular/core';

import { TranslatePipe } from '../shared/translate.pipe';

/** Where an assistant lands while their owner has granted them nothing yet. */
@Component({
  selector: 'bms-no-access',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="empty">
        <h3>{{ 'noAccess.title' | t }}</h3>
        <p>{{ 'noAccess.body' | t }}</p>
      </div>
    </section>
  `,
})
export class NoAccessPage {}
