import { Routes } from '@angular/router';

import { authGuard, ownerGuard, permissionGuard } from './core/guards';

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
    canMatch: [authGuard, permissionGuard('REPORT_READ')],
    loadComponent: () => import('./features/dashboard').then((m) => m.DashboardPage),
  },
  {
    path: 'buildings',
    canMatch: [authGuard, permissionGuard('BUILDING_READ')],
    loadComponent: () => import('./features/buildings').then((m) => m.BuildingsPage),
  },
  {
    path: 'apartments',
    canMatch: [authGuard, permissionGuard('APARTMENT_READ')],
    loadComponent: () => import('./features/apartments').then((m) => m.ApartmentsPage),
  },
  {
    path: 'tenants',
    canMatch: [authGuard, permissionGuard('TENANT_READ')],
    loadComponent: () => import('./features/tenants').then((m) => m.TenantsPage),
  },
  {
    path: 'expenses',
    canMatch: [authGuard, permissionGuard('EXPENSE_READ')],
    loadComponent: () => import('./features/expenses').then((m) => m.ExpensesPage),
  },
  {
    path: 'invoices',
    canMatch: [authGuard, permissionGuard('INVOICE_READ')],
    loadComponent: () => import('./features/invoices').then((m) => m.InvoicesPage),
  },
  {
    path: 'reports',
    canMatch: [authGuard, permissionGuard('REPORT_READ')],
    loadComponent: () => import('./features/reports').then((m) => m.ReportsPage),
  },
  {
    path: 'assistants',
    canMatch: [authGuard, ownerGuard],
    loadComponent: () => import('./features/assistants').then((m) => m.AssistantsPage),
  },
  {
    path: 'no-access',
    canMatch: [authGuard],
    loadComponent: () => import('./features/no-access').then((m) => m.NoAccessPage),
  },
  {
    path: '',
    pathMatch: 'full',
    canMatch: [authGuard],
    loadComponent: () => import('./features/landing').then((m) => m.LandingPage),
  },
  { path: '**', redirectTo: '' },
];
