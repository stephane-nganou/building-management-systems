import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { THEMES, Theme, ThemeService } from '../core/theme';
import { MessageKey } from '../i18n/en';
import { TranslatePipe } from './translate.pipe';

const LABELS: Record<Theme, MessageKey> = {
  classic: 'theme.classic',
  magic: 'theme.magic',
  ocean: 'theme.ocean',
};

/** One swatch per theme, each in its own colours, and the name of the one in use. */
@Component({
  selector: 'bms-theme-switcher',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="themes" role="group" [attr.aria-label]="'app.theme' | t">
      @for (theme of themes; track theme) {
        <button
          type="button"
          class="swatch {{ theme }}"
          [attr.aria-pressed]="appearance.theme() === theme"
          [attr.aria-label]="labels[theme] | t"
          [title]="labels[theme] | t"
          (click)="appearance.use(theme)"
        ></button>
      }
      <span class="theme-name" aria-hidden="true">{{ labels[appearance.theme()] | t }}</span>
    </div>
  `,
})
export class ThemeSwitcher {
  protected readonly themes = THEMES;
  protected readonly labels = LABELS;
  protected readonly appearance = inject(ThemeService);
}
