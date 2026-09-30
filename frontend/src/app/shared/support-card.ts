import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { SupportContacts } from '../core/models';
import { TranslatePipe } from './translate.pipe';

/** Whom to contact, shown wherever an owner may need a person rather than a screen. */
@Component({
  selector: 'bms-support-card',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="support-card" [attr.aria-label]="'support.title' | t">
      <h2>{{ 'support.title' | t }}</h2>
      <dl>
        <dt>{{ 'common.email' | t }}</dt>
        <dd><a [href]="'mailto:' + contacts().email">{{ contacts().email }}</a></dd>
        <dt>{{ 'support.phone' | t }}</dt>
        <dd><a [href]="'tel:' + contacts().phone.replaceAll(' ', '')">{{ contacts().phone }}</a></dd>
        <dt>{{ 'support.hours' | t }}</dt>
        <dd>{{ contacts().hours }}</dd>
      </dl>
    </section>
  `,
})
export class SupportCard {
  readonly contacts = input.required<SupportContacts>();
}
