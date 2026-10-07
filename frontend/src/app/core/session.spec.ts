import { TestBed } from '@angular/core/testing';
import { CanMatchFn, UrlTree, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MeApi } from './api';
import { AuthService } from './auth';
import {
  adminGuard,
  authGuard,
  ownerGuard,
  passwordChangeGuard,
  permissionGuard,
  suspendedGuard,
  visitorGuard,
} from './guards';
import { Me, Permission } from './models';
import { SessionService } from './session';

function profile(owner: boolean, permissions: Permission[], mustChangePassword = false): Me {
  return {
    id: 'u1',
    email: 'someone@example.com',
    name: 'Someone',
    owner,
    admin: false,
    permissions,
    mustChangePassword,
    suspended: false,
    subscription: owner ? { status: 'ACTIVE', endsOn: '2026-12-31' } : null,
    assistingFor: [],
  };
}

function administrator(): Me {
  return { ...profile(false, []), admin: true };
}

const signIn = vi.fn();

/**
 * A session is a cookie the page cannot read, so "signed out" is not a flag to
 * check but a profile request that comes back refused.
 */
function sessionFor(me: Me | null): SessionService {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: { signIn, signOut: vi.fn() } },
      {
        provide: MeApi,
        useValue: { get: () => (me === null ? throwError(() => new Error('401')) : of(me)) },
      },
    ],
  });
  return TestBed.inject(SessionService);
}

/** The router passes a route, its segments and a snapshot; none of it matters here. */
const matchArgs = [{}, [], {}] as unknown as Parameters<CanMatchFn>;

function runGuard(guard: CanMatchFn): Promise<boolean | UrlTree> {
  return Promise.resolve(
    TestBed.runInInjectionContext(() => guard(...matchArgs)) as boolean | UrlTree,
  );
}

describe('SessionService', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
    signIn.mockClear();
  });

  it('gives an owner every screen, with the overview first', async () => {
    const session = sessionFor(profile(true, ['REPORT_READ', 'BUILDING_READ']));
    await session.load();

    expect(session.owner()).toBe(true);
    expect(session.landingRoute()).toBe('/dashboard');
    expect(session.visibleEntries().map((entry) => entry.path)).toContain('/assistants');
  });

  it('shows an assistant only the screens they were granted', async () => {
    const session = sessionFor(profile(false, ['EXPENSE_READ']));
    await session.load();

    expect(session.visibleEntries().map((entry) => entry.path)).toEqual(['/expenses']);
    expect(session.can('EXPENSE_READ')).toBe(true);
    expect(session.can('INVOICE_READ')).toBe(false);
  });

  it('keeps the assistants screen away from assistants', async () => {
    const session = sessionFor(profile(false, ['BUILDING_READ']));
    await session.load();

    expect(session.visibleEntries().map((entry) => entry.path)).not.toContain('/assistants');
  });

  it('lands an assistant with no grants on the empty page', async () => {
    const session = sessionFor(profile(false, []));
    await session.load();

    expect(session.landingRoute()).toBe('/no-access');
  });

  it('shows an administrator the accounts, the metrics and the announcements, and nothing that belongs to an owner', async () => {
    const session = sessionFor(administrator());
    await session.load();

    expect(session.visibleEntries().map((entry) => entry.path)).toEqual([
      '/accounts',
      '/metrics',
      '/announcements',
    ]);
    expect(session.landingRoute()).toBe('/accounts');
  });

  it('keeps the accounts screen away from owners and assistants', async () => {
    const session = sessionFor(profile(true, ['REPORT_READ', 'BUILDING_READ']));
    await session.load();

    expect(session.visibleEntries().map((entry) => entry.path)).not.toContain('/accounts');
  });

  it('tells an owner when their own subscription ran out', async () => {
    const session = sessionFor({ ...profile(true, []), subscription: { status: 'EXPIRED', endsOn: '2026-09-29' } });
    await session.load();

    expect(session.ownSubscriptionEnded()).toBe('2026-09-29');
    expect(session.readOnlyOwners()).toEqual([]);
  });

  it('says nothing while an owner is paid up', async () => {
    const session = sessionFor(profile(true, []));
    await session.load();

    expect(session.ownSubscriptionEnded()).toBeNull();
  });

  it('tells lapsed owners, whose data stays readable, from suspended ones, whose data is closed', async () => {
    const session = sessionFor({
      ...profile(false, ['BUILDING_READ']),
      assistingFor: [
        { ownerId: 'o1', ownerName: 'Olivia', permissions: ['BUILDING_READ'], ownerStatus: 'EXPIRED' },
        { ownerId: 'o2', ownerName: 'Oscar', permissions: ['BUILDING_READ'], ownerStatus: 'ACTIVE' },
        { ownerId: 'o3', ownerName: 'Opal', permissions: ['BUILDING_READ'], ownerStatus: 'SUSPENDED' },
      ],
    });
    await session.load();

    expect(session.readOnlyOwners()).toEqual(['Olivia']);
    expect(session.suspendedOwners()).toEqual(['Opal']);
  });

  it('reads a refused profile as nobody being signed in', async () => {
    const session = sessionFor(null);
    await session.load();

    expect(session.signedIn()).toBe(false);
  });

  it('fetches the profile once however many guards ask for it', async () => {
    const me = profile(true, ['REPORT_READ']);
    const get = vi.fn(() => of(me));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { signIn, signOut: vi.fn() } },
        { provide: MeApi, useValue: { get } },
      ],
    });
    const session = TestBed.inject(SessionService);

    await Promise.all([session.load(), session.load(), session.load()]);

    expect(get).toHaveBeenCalledTimes(1);
  });

  it('reads the profile again once it is asked to reload', async () => {
    const get = vi.fn(() => of(profile(true, ['REPORT_READ'])));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { signIn, signOut: vi.fn() } },
        { provide: MeApi, useValue: { get } },
      ],
    });
    const session = TestBed.inject(SessionService);

    await session.load();
    await session.reload();

    expect(get).toHaveBeenCalledTimes(2);
  });
});

describe('route guards', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
    signIn.mockClear();
  });

  it('sends a signed out visitor to sign in, without matching the route', async () => {
    sessionFor(null);

    expect(await runGuard(authGuard)).toBe(false);
    expect(signIn).toHaveBeenCalled();
  });

  it('lets a signed in user through, having loaded their profile first', async () => {
    const session = sessionFor(profile(false, ['TENANT_READ']));

    expect(await runGuard(authGuard)).toBe(true);
    expect(signIn).not.toHaveBeenCalled();
    // The guard, not the caller, is what made the permissions available.
    expect(session.can('TENANT_READ')).toBe(true);
  });

  it('matches a route only when the permission is held', async () => {
    sessionFor(profile(false, ['TENANT_READ']));

    expect(await runGuard(permissionGuard('TENANT_READ'))).toBe(true);
    expect(await runGuard(permissionGuard('INVOICE_READ'))).toBe(false);
  });

  it('reserves owner routes for owners', async () => {
    sessionFor(profile(false, ['BUILDING_READ']));
    expect(await runGuard(ownerGuard)).toBe(false);

    TestBed.resetTestingModule();
    sessionFor(profile(true, ['BUILDING_READ']));
    expect(await runGuard(ownerGuard)).toBe(true);
  });

  it('holds an account still on a handed over password at the password screen', async () => {
    sessionFor(profile(false, ['EXPENSE_READ'], true));

    const result = await runGuard(authGuard);

    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toBe('/password');
  });

  it('offers the password screen only while it is required', async () => {
    sessionFor(profile(false, ['EXPENSE_READ'], true));
    expect(await runGuard(passwordChangeGuard)).toBe(true);

    TestBed.resetTestingModule();
    sessionFor(profile(false, ['EXPENSE_READ']));
    expect(await runGuard(passwordChangeGuard)).toBe(false);
  });

  it('reserves administrator routes for administrators', async () => {
    sessionFor(profile(true, ['BUILDING_READ']));
    expect(await runGuard(adminGuard)).toBe(false);

    TestBed.resetTestingModule();
    sessionFor(administrator());
    expect(await runGuard(adminGuard)).toBe(true);
  });

  it('holds a suspended account at the screen that says so', async () => {
    sessionFor({ ...profile(true, ['BUILDING_READ']), suspended: true });

    const result = await runGuard(authGuard);

    expect(String(result)).toBe('/suspended');
    expect(await runGuard(suspendedGuard)).toBe(true);
  });

  it('offers the suspended screen only while there is a suspension', async () => {
    sessionFor(profile(true, ['BUILDING_READ']));

    expect(await runGuard(suspendedGuard)).toBe(false);
  });

  it('shows a signed out visitor the landing page, without asking them to sign in', async () => {
    sessionFor(null);

    expect(await runGuard(visitorGuard)).toBe(true);
    expect(signIn).not.toHaveBeenCalled();
  });

  it('passes over the landing page for anybody signed in', async () => {
    sessionFor(profile(true, ['BUILDING_READ']));

    expect(await runGuard(visitorGuard)).toBe(false);
  });
});
