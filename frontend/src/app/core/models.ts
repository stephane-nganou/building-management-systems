export type ApartmentStatus = 'VACANT' | 'OCCUPIED' | 'MAINTENANCE';

export type ExpenseCategory =
  | 'MAINTENANCE'
  | 'REPAIR'
  | 'UTILITIES'
  | 'INSURANCE'
  | 'TAX'
  | 'MANAGEMENT'
  | 'RENOVATION'
  | 'OTHER';

/** The currencies a building can keep its books in; the backend's CurrencyCode. */
export const CURRENCIES = ['EUR', 'XAF', 'XOF', 'USD', 'GBP', 'CHF'] as const;
export type Currency = (typeof CURRENCIES)[number];

export type InvoiceType = 'RENT' | 'COLD_WATER';
export type InvoiceStatus = 'DRAFT' | 'SENT' | 'PAID' | 'CANCELLED';

export type Permission =
  | 'BUILDING_READ'
  | 'BUILDING_WRITE'
  | 'APARTMENT_READ'
  | 'APARTMENT_WRITE'
  | 'TENANT_READ'
  | 'TENANT_WRITE'
  | 'EXPENSE_READ'
  | 'EXPENSE_WRITE'
  | 'INVOICE_READ'
  | 'INVOICE_WRITE'
  | 'REPORT_READ';

export interface Building {
  id: string;
  name: string;
  street: string | null;
  city: string | null;
  postalCode: string | null;
  country: string | null;
  notes: string | null;
  currency: Currency;
  apartmentCount: number;
}

export interface Apartment {
  id: string;
  buildingId: string;
  buildingName: string;
  label: string;
  floor: number | null;
  sizeSqm: number | null;
  rooms: number;
  bedrooms: number;
  bathrooms: number;
  kitchens: number;
  toilets: number;
  baseRent: number;
  utilitiesAdvance: number;
  status: ApartmentStatus;
  currency: Currency;
}

export interface Tenant {
  id: string;
  apartmentId: string;
  apartmentLabel: string;
  buildingId: string;
  buildingName: string;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
  leaseStart: string;
  leaseEnd: string | null;
  deposit: number | null;
  active: boolean;
  currency: Currency;
}

export interface Expense {
  id: string;
  buildingId: string;
  buildingName: string;
  apartmentId: string | null;
  apartmentLabel: string | null;
  category: ExpenseCategory;
  amount: number;
  incurredOn: string;
  description: string;
  vendor: string | null;
  currency: Currency;
}

export interface InvoiceLine {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  unit: string | null;
  amount: number;
}

export interface Invoice {
  id: string;
  invoiceNumber: string;
  tenantId: string;
  tenantName: string;
  apartmentId: string;
  apartmentLabel: string;
  buildingId: string;
  buildingName: string;
  type: InvoiceType;
  status: InvoiceStatus;
  periodStart: string;
  periodEnd: string;
  issueDate: string;
  dueDate: string;
  notes: string | null;
  total: number;
  currency: Currency;
  lines: InvoiceLine[];
}

/** Amounts in one currency. Reports give one of these per currency, never a sum across them. */
export interface ProfitLossTotals {
  currency: Currency;
  income: number;
  expenses: number;
  netResult: number;
}

export interface ProfitLossReport {
  from: string;
  to: string;
  totals: ProfitLossTotals[];
  buildings: {
    buildingId: string;
    buildingName: string;
    currency: Currency;
    income: number;
    expenses: number;
    netResult: number;
  }[];
  expensesByCategory: { category: ExpenseCategory; currency: Currency; amount: number }[];
}

export interface DashboardSummary {
  buildingCount: number;
  apartmentCount: number;
  occupiedApartments: number;
  vacantApartments: number;
  activeTenants: number;
  totals: {
    currency: Currency;
    monthlyRentRoll: number;
    yearToDateIncome: number;
    yearToDateExpenses: number;
    yearToDateNet: number;
  }[];
}

export interface Assistant {
  id: string;
  assistantId: string;
  name: string;
  email: string;
  permissions: Permission[];
  /** Only ever sent back on the response that created it or reset it. */
  temporaryPassword: string | null;
}

/** Only an ACTIVE owner's data can be changed; the rest is read only. */
export type SubscriptionStatus = 'ACTIVE' | 'EXPIRED' | 'SUSPENDED';

export interface Me {
  id: string;
  email: string;
  name: string;
  owner: boolean;
  /** Runs the service. An administrator owns no data and assists nobody. */
  admin: boolean;
  permissions: Permission[];
  /** True while this account still holds a password its owner chose for it. */
  mustChangePassword: boolean;
  /** True once an administrator has suspended this account; nothing else answers it. */
  suspended: boolean;
  /** An owner's own standing; null for anyone else. */
  subscription: { status: SubscriptionStatus; endsOn: string | null } | null;
  assistingFor: {
    ownerId: string;
    ownerName: string;
    permissions: Permission[];
    ownerStatus: SubscriptionStatus;
  }[];
}

/** An owner, as the administrator sees them. */
export interface Account {
  id: string;
  email: string;
  name: string;
  createdAt: string;
  buildings: number;
  assistants: number;
  status: SubscriptionStatus;
  /** The last day of the current period, or of the last one. */
  endsOn: string | null;
  /** Only ever sent back on the response that created the account. */
  temporaryPassword: string | null;
}

/** Both days are included. */
export interface SubscriptionPeriod {
  id: string;
  startsOn: string;
  endsOn: string;
  note: string | null;
}

export interface Registration {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
}
