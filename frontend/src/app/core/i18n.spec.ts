import { TestBed } from '@angular/core/testing';
import { HttpRequest } from '@angular/common/http';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { TranslationService, acceptLanguageInterceptor } from './i18n';
import { NAV_ENTRIES } from './navigation';
import { de } from '../i18n/de';
import { MessageKey, Messages, en } from '../i18n/en';
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

  it('starts in German for a German browser, Austrian and Swiss included', () => {
    expect(serviceWith('de-DE').language()).toBe('de');
    expect(serviceWith('de-AT').language()).toBe('de');
    expect(serviceWith('de-CH').language()).toBe('de');
  });

  it('starts in English for anything else', () => {
    expect(serviceWith('es-ES').language()).toBe('en');
    expect(serviceWith('en-US').language()).toBe('en');
  });

  it('ignores a stored choice it does not know', () => {
    localStorage.setItem('bms.language', 'xx');

    expect(serviceWith('de-DE').language()).toBe('de');
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

    i18n.use('de');
    expect(i18n.translate('buildings.title')).toBe('Gebäude');
  });

  it('asks Intl for the locale that matches the language', () => {
    const i18n = serviceWith('en-GB');
    expect(i18n.locale()).toBe('en-GB');

    i18n.use('fr');
    expect(i18n.locale()).toBe('fr-FR');

    i18n.use('de');
    expect(i18n.locale()).toBe('de-DE');
  });
});

/** The keys whose text in a dictionary is the same as the English one. */
function sameAsEnglish(dictionary: Messages): Set<string> {
  return new Set(Object.keys(en).filter((key) => dictionary[key as MessageKey] === en[key as MessageKey]));
}

/** The {placeholders} a text expects, which a translation has to keep. */
function placeholders(text: string): string[] {
  return [...text.matchAll(/\{(\w+)\}/g)].map((match) => match[1]).sort();
}

describe('the dictionaries', () => {
  // TypeScript already refuses a dictionary with a key missing. What it cannot
  // see is a key left holding the English text, which these catch.
  it('translate every key into German, bar the words that are the same', () => {
    expect(sameAsEnglish(de)).toEqual(
      new Set(['app.role.admin', 'common.name', 'common.status', 'announcements.kind.INFO']),
    );
  });

  it('keep every placeholder in every language', () => {
    for (const dictionary of [fr, de]) {
      for (const key of Object.keys(en) as MessageKey[]) {
        expect(placeholders(dictionary[key]), key).toEqual(placeholders(en[key]));
      }
    }
  });

  it('translate every key into French, bar the words that are the same', () => {
    expect(sameAsEnglish(fr)).toEqual(
      new Set([
        'nav.assistants',
        'common.total',
        'common.notes',
        'common.date',
        'common.description',
        'common.actions',
        'tenants.contact',
        'invoices.type',
        'assistants.title',
        'metrics.series.assistants',
        'announcements.kind.INFO',
        'announcements.message',
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
    const next = vi.fn((request: HttpRequest<unknown>) => request);

    for (const language of ['fr', 'de'] as const) {
      i18n.use(language);
      TestBed.runInInjectionContext(() =>
        acceptLanguageInterceptor(new HttpRequest('GET', '/api/buildings'), next as never),
      );
    }

    expect(next.mock.calls.map(([request]) => request.headers.get('Accept-Language'))).toEqual([
      'fr',
      'de',
    ]);
  });
});
