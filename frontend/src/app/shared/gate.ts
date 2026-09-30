import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Unit } from './facade';
import { Brand } from './brand';
import { Facade } from './facade';
import { LanguageSwitcher } from './language-switcher';
import { TranslatePipe } from './translate.pipe';

/** A small street to stand beside the form: illustration, not anybody's data. */
const STREET: Unit[][] = [
  [
    { floor: 0, status: 'OCCUPIED' },
    { floor: 0, status: 'VACANT' },
    { floor: 1, status: 'OCCUPIED' },
    { floor: 1, status: 'OCCUPIED' },
  ],
  [
    { floor: 0, status: 'OCCUPIED' },
    { floor: 1, status: 'VACANT' },
    { floor: 2, status: 'OCCUPIED' },
    { floor: 3, status: 'OCCUPIED' },
    { floor: 4, status: 'OCCUPIED' },
  ],
  [
    { floor: 0, status: 'VACANT' },
    { floor: 0, status: 'OCCUPIED' },
    { floor: 0, status: 'OCCUPIED' },
    { floor: 1, status: 'OCCUPIED' },
    { floor: 1, status: 'MAINTENANCE' },
    { floor: 1, status: 'OCCUPIED' },
  ],
];

/** The frame for the pages a visitor sees before the app itself: sign up, and first password. */
@Component({
  selector: 'bms-gate',
  imports: [Brand, Facade, LanguageSwitcher, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="gate">
      <aside class="gate-art">
        <bms-brand />
        <p class="gate-pitch">{{ 'gate.pitch' | t }}</p>
        <div class="gate-street" aria-hidden="true">
          @for (units of street; track $index) {
            <bms-facade [units]="units" [scale]="2" [animate]="true" />
          }
        </div>
      </aside>
      <main class="gate-main">
        <div class="gate-card">
          <ng-content />
        </div>
        <bms-language-switcher />
      </main>
    </div>
  `,
})
export class Gate {
  protected readonly street = STREET;
}
