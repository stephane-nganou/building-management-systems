import { describe, expect, it } from 'vitest';

import { Announcement } from '../core/models';
import { phase } from './announcements';

const WINDOW = {
  startsAt: '2026-10-10T08:00:00Z',
  endsAt: '2026-10-11T20:00:00Z',
} as Announcement;

describe('phase', () => {
  it('is scheduled before the start, showing in between and ended from the end on', () => {
    expect(phase(WINDOW, Date.parse('2026-10-10T07:59:59Z'))).toBe('scheduled');
    expect(phase(WINDOW, Date.parse('2026-10-10T08:00:00Z'))).toBe('showing');
    expect(phase(WINDOW, Date.parse('2026-10-11T19:59:59Z'))).toBe('showing');
    expect(phase(WINDOW, Date.parse('2026-10-11T20:00:00Z'))).toBe('ended');
  });
});
