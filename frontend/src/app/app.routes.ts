import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { ownerGuard, permissionGuard } from './core/guards';
import { SessionService } from './core/session';

/**
 * Every screen is gated with canMatch, so a route the caller may not use is
 * never matched and its chunk is never downloaded. Anything unmatched falls
 * through to the empty path, which lands them on the first page they may see.
 */
export const routes: Routes = [
  {
    path: 'register',
    loadComponent: () => import('./features/register').then((m) => m.RegisterPage),
  },
  {
    path: 'dashboard',
    canMatch: [permissionGuard('REPORT_READ')],
    loadComponent: () => import('./features/dashboard').then((m) => m.DashboardPage),
  },
  {
    path: 'buildings',
    canMatch: [permissionGuard('BUILDING_READ')],
    loadComponent: () => import('./features/buildings').then((m) => m.BuildingsPage),
  },
  {
    path: 'apartments',
    canMatch: [permissionGuard('APARTMENT_READ')],
    loadComponent: () => import('./features/apartments').then((m) => m.ApartmentsPage),
  },
  {
    path: 'tenants',
    canMatch: [permissionGuard('TENANT_READ')],
    loadComponent: () => import('./features/tenants').then((m) => m.TenantsPage),
  },
  {
    path: 'expenses',
    canMatch: [permissionGuard('EXPENSE_READ')],
    loadComponent: () => import('./features/expenses').then((m) => m.ExpensesPage),
  },
  {
    path: 'invoices',
    canMatch: [permissionGuard('INVOICE_READ')],
    loadComponent: () => import('./features/invoices').then((m) => m.InvoicesPage),
  },
  {
    path: 'reports',
    canMatch: [permissionGuard('REPORT_READ')],
    loadComponent: () => import('./features/reports').then((m) => m.ReportsPage),
  },
  {
    path: 'assistants',
    canMatch: [ownerGuard],
    loadComponent: () => import('./features/assistants').then((m) => m.AssistantsPage),
  },
  {
    path: 'no-access',
    loadComponent: () => import('./features/no-access').then((m) => m.NoAccessPage),
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: () => inject(SessionService).landingRoute(),
  },
  { path: '**', redirectTo: '' },
];
