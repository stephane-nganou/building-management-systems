import { Injectable, effect, signal } from '@angular/core';

export type Theme = 'classic' | 'magic' | 'ocean';

export const THEMES: readonly Theme[] = ['classic', 'magic', 'ocean'];

const STORAGE_KEY = 'bms.theme';

function initialTheme(): Theme {
  const stored = localStorage.getItem(STORAGE_KEY);
  return THEMES.includes(stored as Theme) ? (stored as Theme) : 'classic';
}

/**
 * How the app looks. The stylesheets do the work: this only names the theme
 * on the root element, where `themes.css` picks it up, and remembers it in
 * this browser.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly current = signal<Theme>(initialTheme());

  readonly theme = this.current.asReadonly();

  constructor() {
    effect(() => (document.documentElement.dataset['theme'] = this.current()));
  }

  use(theme: Theme): void {
    this.current.set(theme);
    localStorage.setItem(STORAGE_KEY, theme);
  }
}
