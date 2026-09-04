import { TestBed } from '@angular/core/testing';
import { CanMatchFn } from '@angular/router';
import Keycloak from 'keycloak-js';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MeApi } from './api';
import { authGuard, ownerGuard, permissionGuard } from './guards';
import { Me, Permission } from './models';
import { SessionService } from './session';

function profile(owner: boolean, permissions: Permission[]): Me {
  return {
    id: 'u1',
    email: 'someone@example.com',
    name: 'Someone',
    owner,
    permissions,
    assistingFor: [],
  };
}

const signIn = vi.fn(() => Promise.resolve());

function sessionFor(me: Me | null): SessionService {
  TestBed.configureTestingModule({
    providers: [
      { provide: Keycloak, useValue: { authenticated: me !== null, login: signIn } },
      { provide: MeApi, useValue: { get: () => of(me) } },
    ],
  });
  return TestBed.inject(SessionService);
}

/** The router passes a route, its segments and a snapshot; none of it matters here. */
const matchArgs = [{}, [], {}] as unknown as Parameters<CanMatchFn>;

function runGuard(guard: CanMatchFn): Promise<boolean> {
  return Promise.resolve(TestBed.runInInjectionContext(() => guard(...matchArgs)) as boolean);
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

  it('fetches the profile once however many guards ask for it', async () => {
    const me = profile(true, ['REPORT_READ']);
    const get = vi.fn(() => of(me));
    TestBed.configureTestingModule({
      providers: [
        { provide: Keycloak, useValue: { authenticated: true, login: signIn } },
        { provide: MeApi, useValue: { get } },
      ],
    });
    const session = TestBed.inject(SessionService);

    await Promise.all([session.load(), session.load(), session.load()]);

    expect(get).toHaveBeenCalledTimes(1);
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
});
