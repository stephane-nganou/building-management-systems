import { describe, expect, it } from 'vitest';

import { Announcement } from '../core/models';
import { announcementText, dismissalKey } from './announcements';

const ANNOUNCEMENT: Announcement = {
  id: 'a1',
  kind: 'WARNING',
  messageEn: 'Down for maintenance on Sunday',
  messageFr: 'Maintenance dimanche',
  messageDe: null,
  startsAt: '2026-10-10T08:00:00Z',
  endsAt: '2026-10-11T20:00:00Z',
  updatedAt: '2026-10-07T09:15:00.123456Z',
};

describe('announcementText', () => {
  it('speaks the reader’s language when the administrator wrote in it', () => {
    expect(announcementText(ANNOUNCEMENT, 'fr')).toBe('Maintenance dimanche');
    expect(announcementText(ANNOUNCEMENT, 'en')).toBe('Down for maintenance on Sunday');
  });

  it('falls back to English when there is no translation', () => {
    expect(announcementText(ANNOUNCEMENT, 'de')).toBe('Down for maintenance on Sunday');
  });
});

describe('dismissalKey', () => {
  it('names one version, so an edit is shown again', () => {
    const edited = { ...ANNOUNCEMENT, updatedAt: '2026-10-07T10:00:00Z' };

    expect(dismissalKey(ANNOUNCEMENT)).not.toBe(dismissalKey(edited));
    expect(dismissalKey(ANNOUNCEMENT)).toBe(dismissalKey({ ...ANNOUNCEMENT }));
  });
});
