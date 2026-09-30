import { MessageKey } from '../i18n/en';
import { IconName } from '../shared/icon';
import { Permission } from './models';

/**
 * The screens the app has, and what a user must hold to be shown one. The
 * sidebar and the route guards read the same list, so a page can never appear
 * in the navigation while its route refuses to load.
 */
export interface NavEntry {
  path: string;
  label: MessageKey;
  icon: IconName;
  permission?: Permission;
  ownerOnly?: boolean;
  adminOnly?: boolean;
}

export const NAV_ENTRIES: NavEntry[] = [
  { path: '/dashboard', label: 'nav.dashboard', icon: 'dashboard', permission: 'REPORT_READ' },
  { path: '/buildings', label: 'nav.buildings', icon: 'building', permission: 'BUILDING_READ' },
  { path: '/apartments', label: 'nav.apartments', icon: 'door', permission: 'APARTMENT_READ' },
  { path: '/tenants', label: 'nav.tenants', icon: 'users', permission: 'TENANT_READ' },
  { path: '/expenses', label: 'nav.expenses', icon: 'receipt', permission: 'EXPENSE_READ' },
  { path: '/invoices', label: 'nav.invoices', icon: 'invoice', permission: 'INVOICE_READ' },
  { path: '/reports', label: 'nav.reports', icon: 'chart', permission: 'REPORT_READ' },
  { path: '/assistants', label: 'nav.assistants', icon: 'assistant', ownerOnly: true },
  { path: '/accounts', label: 'nav.accounts', icon: 'shield', adminOnly: true },
];
