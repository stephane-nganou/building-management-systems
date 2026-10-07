import { Pipe, PipeTransform, inject } from '@angular/core';

import { TranslationService } from '../core/i18n';
import { Currency } from '../core/models';
import { MessageKey } from '../i18n/en';

/** The enum families the app renders. Each one is a group of message keys. */
export type EnumGroup =
  | 'status'
  | 'category'
  | 'invoiceType'
  | 'invoiceStatus'
  | 'permission'
  | 'subscriptionStatus';

const money = new Map<string, Intl.NumberFormat>();
const day = new Map<string, Intl.DateTimeFormat>();
const when = new Map<string, Intl.DateTimeFormat>();

function formatter<T>(cache: Map<string, T>, key: string, build: () => T): T {
  let existing = cache.get(key);
  if (!existing) {
    existing = build();
    cache.set(key, existing);
  }
  return existing;
}

/**
 * Amounts and dates follow the chosen language, so they are impure for the same
 * reason the translation pipe is: the value they are given does not change when
 * the language does. An amount is written in the currency of the building it
 * belongs to, with that currency's own digits: none for the CFA francs.
 */
/** Adds amounts up per currency, in the order the currencies first appear. Never across currencies. */
export function totalsByCurrency<T extends { currency: Currency }>(
  items: readonly T[],
  amount: (item: T) => number,
): { currency: Currency; amount: number }[] {
  const totals = new Map<Currency, number>();
  for (const item of items) {
    totals.set(item.currency, (totals.get(item.currency) ?? 0) + amount(item));
  }
  return [...totals].map(([currency, sum]) => ({ currency, amount: sum }));
}

@Pipe({ name: 'money', pure: false })
export class MoneyPipe implements PipeTransform {
  private i18n = inject(TranslationService);

  transform(value: number | null | undefined, currency: Currency): string {
    const locale = this.i18n.locale();
    return formatter(
      money,
      `${locale} ${currency}`,
      () => new Intl.NumberFormat(locale, { style: 'currency', currency }),
    ).format(value ?? 0);
  }
}

@Pipe({ name: 'day', pure: false })
export class DayPipe implements PipeTransform {
  private i18n = inject(TranslationService);

  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }
    const locale = this.i18n.locale();
    return formatter(
      day,
      locale,
      () => new Intl.DateTimeFormat(locale, { day: '2-digit', month: '2-digit', year: 'numeric' }),
    ).format(parse(value));
  }
}

/** An instant, such as when an announcement starts, as a day and time in the browser's own zone. */
@Pipe({ name: 'when', pure: false })
export class WhenPipe implements PipeTransform {
  private i18n = inject(TranslationService);

  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }
    const locale = this.i18n.locale();
    return formatter(
      when,
      locale,
      () => new Intl.DateTimeFormat(locale, { dateStyle: 'short', timeStyle: 'short' }),
    ).format(new Date(value));
  }
}

/**
 * A bare date such as 2026-10-29 is a calendar day, not an instant. The Date
 * constructor reads it as UTC midnight, which is the evening before anywhere
 * west of Greenwich; with a time and no offset it is read as local instead.
 */
function parse(value: string): Date {
  return new Date(value.length === 10 ? `${value}T00:00` : value);
}

/**
 * Renders an enum value the backend sent, such as `MAINTENANCE`. The group is
 * needed because the same value means different things in different families:
 * an apartment under `MAINTENANCE` is under works, while an expense in that
 * category is upkeep, and French has a separate word for each.
 */
@Pipe({ name: 'label', pure: false })
export class LabelPipe implements PipeTransform {
  private i18n = inject(TranslationService);

  transform(value: string | null | undefined, group: EnumGroup): string {
    return value ? this.i18n.translate(`enum.${group}.${value}` as MessageKey) : '';
  }
}
