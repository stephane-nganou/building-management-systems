import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { Icon, IconName } from './icon';

/**
 * A square button that shows only an icon. Its name is still spoken and shown
 * as a tooltip, so a row of them stays readable without their labels.
 */
@Component({
  selector: 'button[bmsIconButton]',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'icon',
    type: 'button',
    '[attr.aria-label]': 'label()',
    '[title]': 'label()',
  },
  template: `<bms-icon [name]="icon()" />`,
})
export class IconButton {
  readonly icon = input.required<IconName>();
  readonly label = input.required<string>();
}
