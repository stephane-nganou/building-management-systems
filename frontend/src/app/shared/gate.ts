import { ChangeDetectionStrategy, Component } from '@angular/core';

import { Unit } from './facade';
import { Brand } from './brand';
import { Facade } from './facade';
import { LanguageSwitcher } from './language-switcher';
import { ThemeSwitcher } from './theme-switcher';
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

/** The frame for the pages seen instead of the app itself: sign up, first password, and a suspension. */
@Component({
  selector: 'bms-gate',
  imports: [Brand, Facade, LanguageSwitcher, ThemeSwitcher, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="gate">
      <aside class="gate-art night">
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
        <div class="gate-settings">
          <bms-language-switcher />
          <bms-theme-switcher />
        </div>
      </main>
    </div>
  `,
})
export class Gate {
  protected readonly street = STREET;
}
