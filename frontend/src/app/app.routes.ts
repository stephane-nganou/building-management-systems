import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard').then((m) => m.DashboardPage),
  },
  {
    path: 'buildings',
    loadComponent: () => import('./features/buildings').then((m) => m.BuildingsPage),
  },
  {
    path: 'apartments',
    loadComponent: () => import('./features/apartments').then((m) => m.ApartmentsPage),
  },
  {
    path: 'tenants',
    loadComponent: () => import('./features/tenants').then((m) => m.TenantsPage),
  },
  {
    path: 'expenses',
    loadComponent: () => import('./features/expenses').then((m) => m.ExpensesPage),
  },
  {
    path: 'invoices',
    loadComponent: () => import('./features/invoices').then((m) => m.InvoicesPage),
  },
  {
    path: 'reports',
    loadComponent: () => import('./features/reports').then((m) => m.ReportsPage),
  },
  {
    path: 'assistants',
    loadComponent: () => import('./features/assistants').then((m) => m.AssistantsPage),
  },
  { path: '**', redirectTo: 'dashboard' },
];
