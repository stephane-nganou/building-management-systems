import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Apartment,
  Assistant,
  Building,
  DashboardSummary,
  Expense,
  Invoice,
  InvoiceStatus,
  Me,
  Permission,
  ProfitLossReport,
  Registration,
  Tenant,
} from './models';

/** Drops null and undefined so optional filters are simply left off the query. */
function params(source: Record<string, string | null | undefined>): HttpParams {
  let result = new HttpParams();
  for (const [key, value] of Object.entries(source)) {
    if (value !== null && value !== undefined && value !== '') {
      result = result.set(key, value);
    }
  }
  return result;
}

@Injectable({ providedIn: 'root' })
export class BuildingsApi {
  private http = inject(HttpClient);

  list(): Observable<Building[]> {
    return this.http.get<Building[]>('/api/buildings');
  }

  get(id: string): Observable<Building> {
    return this.http.get<Building>(`/api/buildings/${id}`);
  }

  create(body: unknown): Observable<Building> {
    return this.http.post<Building>('/api/buildings', body);
  }

  update(id: string, body: unknown): Observable<Building> {
    return this.http.put<Building>(`/api/buildings/${id}`, body);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`/api/buildings/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class ApartmentsApi {
  private http = inject(HttpClient);

  list(buildingId?: string): Observable<Apartment[]> {
    return this.http.get<Apartment[]>('/api/apartments', { params: params({ buildingId }) });
  }

  create(buildingId: string, body: unknown): Observable<Apartment> {
    return this.http.post<Apartment>(`/api/buildings/${buildingId}/apartments`, body);
  }

  update(id: string, body: unknown): Observable<Apartment> {
    return this.http.put<Apartment>(`/api/apartments/${id}`, body);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`/api/apartments/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class TenantsApi {
  private http = inject(HttpClient);

  list(apartmentId?: string): Observable<Tenant[]> {
    return this.http.get<Tenant[]>('/api/tenants', { params: params({ apartmentId }) });
  }

  create(apartmentId: string, body: unknown): Observable<Tenant> {
    return this.http.post<Tenant>(`/api/apartments/${apartmentId}/tenants`, body);
  }

  update(id: string, body: unknown): Observable<Tenant> {
    return this.http.put<Tenant>(`/api/tenants/${id}`, body);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`/api/tenants/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class ExpensesApi {
  private http = inject(HttpClient);

  search(filters: {
    buildingId?: string;
    apartmentId?: string;
    from?: string;
    to?: string;
  }): Observable<Expense[]> {
    return this.http.get<Expense[]>('/api/expenses', { params: params(filters) });
  }

  create(body: unknown): Observable<Expense> {
    return this.http.post<Expense>('/api/expenses', body);
  }

  update(id: string, body: unknown): Observable<Expense> {
    return this.http.put<Expense>(`/api/expenses/${id}`, body);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`/api/expenses/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class InvoicesApi {
  private http = inject(HttpClient);

  search(filters: {
    buildingId?: string;
    status?: InvoiceStatus | '';
    from?: string;
    to?: string;
  }): Observable<Invoice[]> {
    return this.http.get<Invoice[]>('/api/invoices', { params: params(filters) });
  }

  create(body: unknown): Observable<Invoice> {
    return this.http.post<Invoice>('/api/invoices', body);
  }

  changeStatus(id: string, status: InvoiceStatus): Observable<Invoice> {
    return this.http.post<Invoice>(`/api/invoices/${id}/status`, null, {
      params: params({ status }),
    });
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`/api/invoices/${id}`);
  }

  downloadPdf(id: string): Observable<Blob> {
    return this.http.get(`/api/invoices/${id}/pdf`, { responseType: 'blob' });
  }
}

@Injectable({ providedIn: 'root' })
export class ReportsApi {
  private http = inject(HttpClient);

  summary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>('/api/reports/summary');
  }

  profitLoss(from: string, to: string, buildingId?: string): Observable<ProfitLossReport> {
    return this.http.get<ProfitLossReport>('/api/reports/profit-loss', {
      params: params({ from, to, buildingId }),
    });
  }
}

@Injectable({ providedIn: 'root' })
export class AssistantsApi {
  private http = inject(HttpClient);

  list(): Observable<Assistant[]> {
    return this.http.get<Assistant[]>('/api/assistants');
  }

  permissions(): Observable<Permission[]> {
    return this.http.get<Permission[]>('/api/assistants/permissions');
  }

  grant(
    email: string,
    firstName: string,
    lastName: string,
    permissions: Permission[],
  ): Observable<Assistant> {
    return this.http.post<Assistant>('/api/assistants', {
      email,
      firstName,
      lastName,
      permissions,
    });
  }

  update(id: string, permissions: Permission[]): Observable<Assistant> {
    return this.http.put<Assistant>(`/api/assistants/${id}`, { permissions });
  }

  resetPassword(id: string): Observable<Assistant> {
    return this.http.post<Assistant>(`/api/assistants/${id}/password`, null);
  }

  revoke(id: string): Observable<void> {
    return this.http.delete<void>(`/api/assistants/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private http = inject(HttpClient);

  /** The only call made without a session. */
  register(body: Registration): Observable<unknown> {
    return this.http.post('/api/auth/register', body);
  }

  /** Replaces the caller's own password. Signing in is a navigation, not a call. */
  changePassword(newPassword: string): Observable<void> {
    return this.http.post<void>('/api/auth/password', { newPassword });
  }
}

@Injectable({ providedIn: 'root' })
export class MeApi {
  private http = inject(HttpClient);

  get(): Observable<Me> {
    return this.http.get<Me>('/api/me');
  }
}
