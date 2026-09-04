import { Pipe, PipeTransform, inject } from '@angular/core';

import { TranslationService } from '../core/i18n';
import { MessageKey } from '../i18n/en';

/** The enum families the app renders. Each one is a group of message keys. */
export type EnumGroup = 'status' | 'category' | 'invoiceType' | 'invoiceStatus' | 'permission';

const money = new Map<string, Intl.NumberFormat>();
const day = new Map<string, Intl.DateTimeFormat>();

function formatter<T>(cache: Map<string, T>, locale: string, build: () => T): T {
  let existing = cache.get(locale);
  if (!existing) {
    existing = build();
    cache.set(locale, existing);
  }
  return existing;
}

/**
 * Amounts and dates follow the chosen language, so they are impure for the same
 * reason the translation pipe is: the value they are given does not change when
 * the language does.
 */
@Pipe({ name: 'money', pure: false })
export class MoneyPipe implements PipeTransform {
  private i18n = inject(TranslationService);

  transform(value: number | null | undefined): string {
    const locale = this.i18n.locale();
    return formatter(
      money,
      locale,
      () =>
        new Intl.NumberFormat(locale, {
          style: 'currency',
          currency: 'EUR',
          maximumFractionDigits: 2,
        }),
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
    ).format(new Date(value));
  }
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
