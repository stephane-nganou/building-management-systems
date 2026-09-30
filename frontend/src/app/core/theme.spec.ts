import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { ThemeService } from './theme';

function freshService(): ThemeService {
  TestBed.resetTestingModule();
  return TestBed.inject(ThemeService);
}

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    delete document.documentElement.dataset['theme'];
  });

  it('starts on the classic theme', () => {
    expect(freshService().theme()).toBe('classic');
  });

  it('ignores a stored value that is not a theme', () => {
    localStorage.setItem('bms.theme', 'neon');

    expect(freshService().theme()).toBe('classic');
  });

  it('remembers a choice across sessions', () => {
    freshService().use('ocean');

    expect(freshService().theme()).toBe('ocean');
  });

  it('names the theme on the root element, where the stylesheets look for it', () => {
    const themes = freshService();
    TestBed.tick();
    expect(document.documentElement.dataset['theme']).toBe('classic');

    themes.use('magic');
    TestBed.tick();
    expect(document.documentElement.dataset['theme']).toBe('magic');
  });
});
