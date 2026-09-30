import { ChangeDetectionStrategy, Component } from '@angular/core';

import { TranslatePipe } from './translate.pipe';

/** The mark, a small building with a light on, beside the product name. */
@Component({
  selector: 'bms-brand',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg width="30" height="34" viewBox="0 0 30 34" aria-hidden="true">
      <rect x="1" y="4" width="28" height="29" rx="2" fill="#272b4d" stroke="#8b90ad" stroke-width="1.25" />
      <rect x="0" y="2" width="30" height="3" rx="1" fill="#8b90ad" />
      <rect x="6" y="9" width="7" height="8" rx="1" fill="#3b4166" />
      <rect x="17" y="9" width="7" height="8" rx="1" fill="#f0b43c" />
      <rect x="6" y="20" width="7" height="8" rx="1" fill="#f0b43c" />
      <rect x="17" y="20" width="7" height="8" rx="1" fill="#3b4166" />
    </svg>
    <span>
      <strong>Hausbuch</strong>
      <span class="tagline">{{ 'app.tagline' | t }}</span>
    </span>
  `,
  host: { class: 'brand' },
})
export class Brand {}
