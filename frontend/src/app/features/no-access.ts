import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Where an assistant lands while their owner has granted them nothing yet. */
@Component({
  selector: 'bms-no-access',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="empty">
        <h3>Nothing to show yet</h3>
        <p>Ask the owner you assist to give you access, then sign in again.</p>
      </div>
    </section>
  `,
})
export class NoAccessPage {}
