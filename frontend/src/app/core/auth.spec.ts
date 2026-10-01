import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MeApi } from './api';
import { AuthService, authInterceptor } from './auth';

const signIn = vi.fn();

describe('authInterceptor', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.resetTestingModule();
    signIn.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { signIn } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('turns a refused request into a sign in', () => {
    TestBed.inject(HttpClient)
      .get('/api/buildings')
      .subscribe({ error: () => undefined });

    http.expectOne('/api/buildings').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(signIn).toHaveBeenCalled();
  });

  it('leaves a refused profile to the guards, so a visitor can see the landing page', () => {
    const failed = vi.fn();
    TestBed.inject(MeApi).get().subscribe({ error: failed });

    http.expectOne('/api/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(signIn).not.toHaveBeenCalled();
    expect(failed).toHaveBeenCalled();
  });
});
