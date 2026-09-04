import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { LANGUAGES, TranslationService } from '../core/i18n';
import { TranslatePipe } from './translate.pipe';

/** Two buttons, EN and FR. Shown in the sidebar, and on the pages that have none. */
@Component({
  selector: 'bms-language-switcher',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="languages" role="group" [attr.aria-label]="'app.language' | t">
      @for (language of languages; track language) {
        <button
          type="button"
          class="language"
          [class.on]="i18n.language() === language"
          [attr.aria-pressed]="i18n.language() === language"
          (click)="i18n.use(language)"
        >
          {{ language.toUpperCase() }}
        </button>
      }
    </div>
  `,
})
export class LanguageSwitcher {
  protected readonly languages = LANGUAGES;
  protected readonly i18n = inject(TranslationService);
}
