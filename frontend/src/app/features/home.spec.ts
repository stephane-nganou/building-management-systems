import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthService } from '../core/auth';
import { HomePage } from './home';
import { CONTACTS } from './home-content';

const signIn = vi.fn();

function render(): HTMLElement {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AuthService, useValue: { signIn } }],
  });
  const fixture = TestBed.createComponent(HomePage);
  fixture.detectChanges();
  return fixture.nativeElement;
}

describe('HomePage', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
    signIn.mockClear();
    localStorage.setItem('bms.language', 'en');
  });

  it('opens with one headline', () => {
    const page = render();

    const headlines = page.querySelectorAll('h1');
    expect(headlines).toHaveLength(1);
    expect(headlines[0].textContent).toBe('Your buildings, rents and receipts in one ledger');
  });

  it('sends every free month button to sign up', () => {
    const page = render();

    const trials = [...page.querySelectorAll('a')].filter((link) =>
      link.textContent?.includes('Start your free month'),
    );
    expect(trials.length).toBeGreaterThan(0);
    for (const link of trials) {
      expect(link.getAttribute('href')).toBe('/register');
    }
  });

  it('signs in and comes back to the start', () => {
    const page = render();

    page.querySelector<HTMLButtonElement>('button.home-sign-in')!.click();

    expect(signIn).toHaveBeenCalledWith('/');
  });

  it('lists every contact, as a link wherever there is one', () => {
    const page = render();

    const rows = page.querySelectorAll('.home-contacts li');
    expect(rows).toHaveLength(CONTACTS.length);
    CONTACTS.forEach((contact, index) => {
      expect(rows[index].textContent).toContain(contact.value);
      expect(rows[index].querySelector('a')?.getAttribute('href') ?? undefined).toBe(contact.href);
    });
  });

  it('reads in French for a French visitor', () => {
    localStorage.setItem('bms.language', 'fr');
    const page = render();

    expect(page.querySelector('h1')!.textContent).toBe(
      'Vos immeubles, loyers et reçus dans un seul registre',
    );
  });
});
