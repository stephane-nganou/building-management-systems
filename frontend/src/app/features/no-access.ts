import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Facade } from '../shared/facade';
import { TranslatePipe } from '../shared/translate.pipe';

/** Where an assistant lands while their owner has granted them nothing yet. */
@Component({
  selector: 'bms-no-access',
  imports: [Facade, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="empty">
        <bms-facade [units]="[]" [scale]="2" />
        <div>
          <h3>{{ 'noAccess.title' | t }}</h3>
          <p>{{ 'noAccess.body' | t }}</p>
        </div>
      </div>
    </section>
  `,
})
export class NoAccessPage {}
