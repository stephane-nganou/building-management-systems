import { Permission } from './models';

/**
 * The screens the app has, and what a user must hold to be shown one. The
 * sidebar and the route guards read the same list, so a page can never appear
 * in the navigation while its route refuses to load.
 */
export interface NavEntry {
  path: string;
  label: string;
  permission?: Permission;
  ownerOnly?: boolean;
}

export const NAV_ENTRIES: NavEntry[] = [
  { path: '/dashboard', label: 'Overview', permission: 'REPORT_READ' },
  { path: '/buildings', label: 'Buildings', permission: 'BUILDING_READ' },
  { path: '/apartments', label: 'Apartments', permission: 'APARTMENT_READ' },
  { path: '/tenants', label: 'Tenants', permission: 'TENANT_READ' },
  { path: '/expenses', label: 'Expenses', permission: 'EXPENSE_READ' },
  { path: '/invoices', label: 'Invoices', permission: 'INVOICE_READ' },
  { path: '/reports', label: 'Profit and loss', permission: 'REPORT_READ' },
  { path: '/assistants', label: 'Assistants', ownerOnly: true },
];
