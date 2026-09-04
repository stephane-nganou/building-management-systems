import { MessageKey } from '../i18n/en';
import { Permission } from './models';

/**
 * The screens the app has, and what a user must hold to be shown one. The
 * sidebar and the route guards read the same list, so a page can never appear
 * in the navigation while its route refuses to load.
 */
export interface NavEntry {
  path: string;
  label: MessageKey;
  permission?: Permission;
  ownerOnly?: boolean;
}

export const NAV_ENTRIES: NavEntry[] = [
  { path: '/dashboard', label: 'nav.dashboard', permission: 'REPORT_READ' },
  { path: '/buildings', label: 'nav.buildings', permission: 'BUILDING_READ' },
  { path: '/apartments', label: 'nav.apartments', permission: 'APARTMENT_READ' },
  { path: '/tenants', label: 'nav.tenants', permission: 'TENANT_READ' },
  { path: '/expenses', label: 'nav.expenses', permission: 'EXPENSE_READ' },
  { path: '/invoices', label: 'nav.invoices', permission: 'INVOICE_READ' },
  { path: '/reports', label: 'nav.reports', permission: 'REPORT_READ' },
  { path: '/assistants', label: 'nav.assistants', ownerOnly: true },
];
