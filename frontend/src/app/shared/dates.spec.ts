import { describe, expect, it } from 'vitest';

import { fromLocalInput, isoDay, toLocalInput } from './dates';

describe('isoDay', () => {
  it('writes the calendar day in the browser’s own zone', () => {
    expect(isoDay(new Date(2026, 0, 5, 23, 30))).toBe('2026-01-05');
  });
});

describe('local date and time fields', () => {
  it('round trip an instant to the minute, whatever the browser’s zone', () => {
    const instant = '2026-10-10T08:30:00.000Z';

    expect(toLocalInput(instant)).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
    expect(fromLocalInput(toLocalInput(instant))).toBe(instant);
  });
});
