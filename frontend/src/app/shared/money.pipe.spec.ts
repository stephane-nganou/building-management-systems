import { describe, expect, it } from 'vitest';

import { DayPipe, LabelPipe, MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  it('formats an amount as euros', () => {
    expect(pipe.transform(1234.5)).toContain('1.234,50');
  });

  it('treats a missing amount as zero', () => {
    expect(pipe.transform(null)).toContain('0,00');
    expect(pipe.transform(undefined)).toContain('0,00');
  });
});

describe('DayPipe', () => {
  const pipe = new DayPipe();

  it('formats an ISO date as day, month, year', () => {
    expect(pipe.transform('2026-02-09')).toBe('09.02.2026');
  });

  it('returns nothing for a missing date', () => {
    expect(pipe.transform(null)).toBe('');
  });
});

describe('LabelPipe', () => {
  const pipe = new LabelPipe();

  it('turns an enum value into readable words', () => {
    expect(pipe.transform('COLD_WATER')).toBe('Cold water');
    expect(pipe.transform('BUILDING_READ')).toBe('Building read');
  });

  it('returns nothing for a missing value', () => {
    expect(pipe.transform(undefined)).toBe('');
  });
});
