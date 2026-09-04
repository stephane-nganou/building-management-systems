import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { Language, TranslationService } from '../core/i18n';
import { DayPipe, LabelPipe, MoneyPipe } from './money.pipe';

function pipes(language: Language) {
  TestBed.resetTestingModule();
  localStorage.setItem('bms.language', language);
  const i18n = TestBed.inject(TranslationService);
  return TestBed.runInInjectionContext(() => ({
    i18n,
    money: new MoneyPipe(),
    day: new DayPipe(),
    label: new LabelPipe(),
  }));
}

describe('MoneyPipe', () => {
  beforeEach(() => localStorage.clear());

  it('formats an amount the English way', () => {
    // A non breaking space separates amount from symbol, so match the digits only.
    expect(pipes('en').money.transform(1234.5)).toContain('1,234.50');
  });

  it('formats the same amount the French way', () => {
    expect(pipes('fr').money.transform(1234.5)).toContain('234,50');
  });

  it('treats a missing amount as zero', () => {
    const { money } = pipes('en');
    expect(money.transform(null)).toContain('0.00');
    expect(money.transform(undefined)).toContain('0.00');
  });

  it('follows a language change without being rebuilt', () => {
    const { i18n, money } = pipes('en');
    expect(money.transform(1234.5)).toContain('1,234.50');

    i18n.use('fr');
    expect(money.transform(1234.5)).toContain('234,50');
  });
});

describe('DayPipe', () => {
  beforeEach(() => localStorage.clear());

  it('puts the day before the month in both languages', () => {
    expect(pipes('en').day.transform('2026-02-09')).toBe('09/02/2026');
    expect(pipes('fr').day.transform('2026-02-09')).toBe('09/02/2026');
  });

  it('returns nothing for a missing date', () => {
    expect(pipes('en').day.transform(null)).toBe('');
  });
});

describe('LabelPipe', () => {
  beforeEach(() => localStorage.clear());

  it('reads an enum value from the dictionary', () => {
    expect(pipes('en').label.transform('COLD_WATER', 'invoiceType')).toBe('Cold water');
    expect(pipes('fr').label.transform('COLD_WATER', 'invoiceType')).toBe('Eau froide');
  });

  it('tells the two meanings of MAINTENANCE apart', () => {
    const { label } = pipes('fr');
    expect(label.transform('MAINTENANCE', 'status')).toBe('En travaux');
    expect(label.transform('MAINTENANCE', 'category')).toBe('Entretien');
  });

  it('returns nothing for a missing value', () => {
    expect(pipes('en').label.transform(undefined, 'permission')).toBe('');
  });
});
