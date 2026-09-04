import { TestBed } from '@angular/core/testing';
import { HttpRequest } from '@angular/common/http';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { TranslationService, acceptLanguageInterceptor } from './i18n';
import { NAV_ENTRIES } from './navigation';
import { en } from '../i18n/en';
import { fr } from '../i18n/fr';

function serviceWith(browserLanguage: string): TranslationService {
  vi.spyOn(navigator, 'language', 'get').mockReturnValue(browserLanguage);
  TestBed.resetTestingModule();
  return TestBed.inject(TranslationService);
}

describe('TranslationService', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('starts in French for a French browser', () => {
    expect(serviceWith('fr-FR').language()).toBe('fr');
  });

  it('starts in English for anything else', () => {
    expect(serviceWith('de-DE').language()).toBe('en');
    expect(serviceWith('en-US').language()).toBe('en');
  });

  it('remembers a choice across sessions, ahead of the browser', () => {
    serviceWith('en-GB').use('fr');

    expect(serviceWith('en-GB').language()).toBe('fr');
  });

  it('interpolates placeholders', () => {
    const i18n = serviceWith('en-GB');

    expect(i18n.translate('app.role.assisting', { count: 2 })).toBe('Assisting 2 owner(s)');
  });

  it('says the same thing in the other language', () => {
    const i18n = serviceWith('en-GB');
    expect(i18n.translate('buildings.title')).toBe('Buildings');

    i18n.use('fr');
    expect(i18n.translate('buildings.title')).toBe('Immeubles');
  });

  it('asks Intl for the locale that matches the language', () => {
    const i18n = serviceWith('en-GB');
    expect(i18n.locale()).toBe('en-GB');

    i18n.use('fr');
    expect(i18n.locale()).toBe('fr-FR');
  });
});

describe('the dictionaries', () => {
  it('translate every key, bar the words that are the same in both languages', () => {
    // TypeScript already refuses a French dictionary with a key missing. What it
    // cannot see is a key left holding the English text, which this catches.
    const identical = Object.keys(en).filter(
      (key) => fr[key as keyof typeof fr] === en[key as keyof typeof en],
    );

    expect(new Set(identical)).toEqual(
      new Set([
        'nav.assistants',
        'common.total',
        'common.notes',
        'common.date',
        'common.description',
        'tenants.contact',
        'invoices.type',
        'assistants.title',
      ]),
    );
  });

  it('has a label for every screen in the navigation', () => {
    for (const entry of NAV_ENTRIES) {
      expect(en[entry.label]).toBeTruthy();
    }
  });
});

describe('acceptLanguageInterceptor', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('tells the backend which language to answer in', () => {
    const i18n = serviceWith('en-GB');
    i18n.use('fr');
    const next = vi.fn((request: HttpRequest<unknown>) => request);

    TestBed.runInInjectionContext(() =>
      acceptLanguageInterceptor(new HttpRequest('GET', '/api/buildings'), next as never),
    );

    expect(next.mock.calls[0][0].headers.get('Accept-Language')).toBe('fr');
  });
});
