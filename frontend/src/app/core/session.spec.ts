import { TestBed } from '@angular/core/testing';
import { CanMatchFn } from '@angular/router';
import Keycloak from 'keycloak-js';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MeApi } from './api';
import { ownerGuard, permissionGuard } from './guards';
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

  it('leaves the registration page alone when signed out', async () => {
    history.pushState({}, '', '/register');
    const session = sessionFor(null);
    await session.load();

    expect(signIn).not.toHaveBeenCalled();
    expect(session.user()).toBeNull();
    expect(session.can('BUILDING_READ')).toBe(false);
  });

  it('sends a signed out visitor of any other page to sign in', async () => {
    history.pushState({}, '', '/buildings');
    await sessionFor(null).load();

    expect(signIn).toHaveBeenCalled();
  });
});

describe('route guards', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('matches a route only when the permission is held', async () => {
    const session = sessionFor(profile(false, ['TENANT_READ']));
    await session.load();

    expect(TestBed.runInInjectionContext(() => permissionGuard('TENANT_READ')(...matchArgs))).toBe(
      true,
    );
    expect(TestBed.runInInjectionContext(() => permissionGuard('INVOICE_READ')(...matchArgs))).toBe(
      false,
    );
  });

  it('reserves owner routes for owners', async () => {
    const assistant = sessionFor(profile(false, ['BUILDING_READ']));
    await assistant.load();
    expect(TestBed.runInInjectionContext(() => ownerGuard(...matchArgs))).toBe(false);

    TestBed.resetTestingModule();
    const owner = sessionFor(profile(true, ['BUILDING_READ']));
    await owner.load();
    expect(TestBed.runInInjectionContext(() => ownerGuard(...matchArgs))).toBe(true);
  });
});
