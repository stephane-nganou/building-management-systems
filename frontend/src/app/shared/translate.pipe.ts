import { Pipe, PipeTransform, inject } from '@angular/core';

import { TranslationService } from '../core/i18n';
import { MessageKey } from '../i18n/en';

/**
 * `{{ 'buildings.title' | t }}`, with optional placeholders:
 * `{{ 'app.role.assisting' | t: { count: 2 } }}`.
 *
 * <p>Impure on purpose. A pure pipe caches on its argument, so the key staying
 * the same while the language changes would keep the old wording on screen.
 */
@Pipe({ name: 't', pure: false })
export class TranslatePipe implements PipeTransform {
  private i18n = inject(TranslationService);

  transform(key: MessageKey, values?: Record<string, string | number>): string {
    return this.i18n.translate(key, values);
  }
}
