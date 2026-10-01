import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { Language, TranslationService } from '../core/i18n';
import { DayPipe, LabelPipe, MoneyPipe, totalsByCurrency } from './money.pipe';

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
    expect(pipes('en').money.transform(1234.5, 'EUR')).toContain('1,234.50');
  });

  it('formats the same amount the French way', () => {
    expect(pipes('fr').money.transform(1234.5, 'EUR')).toContain('234,50');
  });

  it('formats the same amount the German way', () => {
    expect(pipes('de').money.transform(1234.5, 'EUR')).toContain('1.234,50');
  });

  it('treats a missing amount as zero', () => {
    const { money } = pipes('en');
    expect(money.transform(null, 'EUR')).toContain('0.00');
    expect(money.transform(undefined, 'EUR')).toContain('0.00');
  });

  it('follows a language change without being rebuilt', () => {
    const { i18n, money } = pipes('en');
    expect(money.transform(1234.5, 'EUR')).toContain('1,234.50');

    i18n.use('fr');
    expect(money.transform(1234.5, 'EUR')).toContain('234,50');
  });

  it('writes each amount in the currency it was given', () => {
    const { money } = pipes('en');
    expect(money.transform(1234.5, 'EUR')).toContain('€');
    expect(money.transform(1234.5, 'USD')).toContain('$');
    expect(money.transform(1234.5, 'CHF')).toContain('CHF');
  });

  /** The CFA francs have no cents, so there is nothing after the decimal point. */
  it('writes the CFA francs in whole francs', () => {
    const { money } = pipes('fr');
    const amount = money.transform(150000.5, 'XAF');

    expect(amount).toContain('FCFA');
    expect(amount.replace(/\s/g, '')).toContain('150001');
    expect(amount).not.toContain(',');
  });
});

describe('totalsByCurrency', () => {
  it('adds amounts up per currency and never across them', () => {
    const expenses = [
      { currency: 'EUR' as const, amount: 100 },
      { currency: 'XAF' as const, amount: 50000 },
      { currency: 'EUR' as const, amount: 20.5 },
    ];

    expect(totalsByCurrency(expenses, (expense) => expense.amount)).toEqual([
      { currency: 'EUR', amount: 120.5 },
      { currency: 'XAF', amount: 50000 },
    ]);
  });

  it('has no line at all for nothing', () => {
    expect(totalsByCurrency([], () => 0)).toEqual([]);
  });
});

describe('DayPipe', () => {
  beforeEach(() => localStorage.clear());

  it('puts the day before the month in every language', () => {
    expect(pipes('en').day.transform('2026-02-09')).toBe('09/02/2026');
    expect(pipes('fr').day.transform('2026-02-09')).toBe('09/02/2026');
    expect(pipes('de').day.transform('2026-02-09')).toBe('09.02.2026');
  });

  it('returns nothing for a missing date', () => {
    expect(pipes('en').day.transform(null)).toBe('');
  });

  /** Read as UTC midnight, a bare date showed the day before anywhere west of Greenwich. */
  it('shows a calendar date as that day in any time zone', () => {
    expect(pipes('en').day.transform('2026-10-29')).toBe('29/10/2026');
  });
});

describe('LabelPipe', () => {
  beforeEach(() => localStorage.clear());

  it('reads an enum value from the dictionary', () => {
    expect(pipes('en').label.transform('COLD_WATER', 'invoiceType')).toBe('Cold water');
    expect(pipes('fr').label.transform('COLD_WATER', 'invoiceType')).toBe('Eau froide');
    expect(pipes('de').label.transform('COLD_WATER', 'invoiceType')).toBe('Kaltwasser');
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
