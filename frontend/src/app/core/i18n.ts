import { HttpInterceptorFn } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal } from '@angular/core';

import { de } from '../i18n/de';
import { MessageKey, Messages, en } from '../i18n/en';
import { fr } from '../i18n/fr';

export type Language = 'en' | 'fr' | 'de';

export const LANGUAGES: readonly Language[] = ['en', 'fr', 'de'];

const DICTIONARIES: Record<Language, Messages> = { en, fr, de };

/** What `Intl` is asked for. Each puts the day before the month, as the app always has. */
const LOCALES: Record<Language, string> = { en: 'en-GB', fr: 'fr-FR', de: 'de-DE' };

const STORAGE_KEY = 'bms.language';

function supported(code: string | null): Language | undefined {
  return LANGUAGES.find((language) => language === code);
}

/** A previous choice wins; failing that, the browser's own language decides, then English. */
function initialLanguage(): Language {
  const browser = navigator.language.toLowerCase().slice(0, 2);
  return supported(localStorage.getItem(STORAGE_KEY)) ?? supported(browser) ?? 'en';
}

function interpolate(text: string, values: Record<string, string | number>): string {
  return text.replace(/\{(\w+)\}/g, (match, name) => String(values[name] ?? match));
}

/**
 * The language the app speaks, and every string it says.
 *
 * <p>Translation is a runtime lookup rather than Angular's build time i18n, so
 * one bundle serves every language and switching is immediate. The current
 * language is a signal: reading it inside a pipe registers the dependency with
 * whichever view is rendering, so a switch marks exactly those views dirty.
 */
@Injectable({ providedIn: 'root' })
export class TranslationService {
  private readonly current = signal<Language>(initialLanguage());

  readonly language = this.current.asReadonly();
  readonly locale = computed(() => LOCALES[this.current()]);

  constructor() {
    // Screen readers and the browser's own spell checking read this attribute.
    effect(() => (document.documentElement.lang = this.current()));
  }

  use(language: Language): void {
    this.current.set(language);
    localStorage.setItem(STORAGE_KEY, language);
  }

  translate(key: MessageKey, values?: Record<string, string | number>): string {
    const text = DICTIONARIES[this.current()][key] ?? key;
    return values ? interpolate(text, values) : text;
  }
}

/**
 * Tells the backend which language to answer in. Its error messages, and the
 * wording on invoice PDFs, follow this header.
 */
export const acceptLanguageInterceptor: HttpInterceptorFn = (request, next) => {
  const language = inject(TranslationService).language();
  return next(request.clone({ setHeaders: { 'Accept-Language': language } }));
};
