import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ConfirmService } from './confirm';
import { ToastService } from './toasts';

describe('ConfirmService', () => {
  beforeEach(() => {
    localStorage.setItem('bms.language', 'en');
    TestBed.resetTestingModule();
  });

  it('asks in the words of a delete and answers with what was chosen', async () => {
    const confirm = TestBed.inject(ConfirmService);

    const answer = confirm.delete('Hauptstrasse 1');
    expect(confirm.pending()?.heading).toBe('Delete Hauptstrasse 1?');
    expect(confirm.pending()?.action).toBe('Delete');

    confirm.settle(true);
    await expect(answer).resolves.toBe(true);
    expect(confirm.pending()).toBeNull();
  });

  it('treats a dialog closed any other way as a no', async () => {
    const confirm = TestBed.inject(ConfirmService);

    const answer = confirm.delete('1A');
    confirm.settle(false);
    // Closing the dialog settles again as it goes; that must not change the answer.
    confirm.settle(true);

    await expect(answer).resolves.toBe(false);
  });
});

describe('ToastService', () => {
  it('shows a message and lets it go on its own', () => {
    vi.useFakeTimers();
    const toasts = TestBed.inject(ToastService);

    toasts.show('Hauptstrasse 1 saved');
    expect(toasts.toasts().map((toast) => toast.text)).toEqual(['Hauptstrasse 1 saved']);

    vi.advanceTimersByTime(3500);
    expect(toasts.toasts()).toEqual([]);
    vi.useRealTimers();
  });
});
