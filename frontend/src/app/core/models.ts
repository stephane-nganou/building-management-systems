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
  lines: InvoiceLine[];
}

export interface ProfitLossReport {
  from: string;
  to: string;
  totalIncome: number;
  totalExpenses: number;
  netResult: number;
  buildings: {
    buildingId: string;
    buildingName: string;
    income: number;
    expenses: number;
    netResult: number;
  }[];
  expensesByCategory: { category: ExpenseCategory; amount: number }[];
}

export interface DashboardSummary {
  buildingCount: number;
  apartmentCount: number;
  occupiedApartments: number;
  vacantApartments: number;
  activeTenants: number;
  monthlyRentRoll: number;
  yearToDateIncome: number;
  yearToDateExpenses: number;
  yearToDateNet: number;
}

export interface Assistant {
  id: string;
  assistantId: string;
  name: string;
  email: string;
  permissions: Permission[];
}

export interface Me {
  id: string;
  email: string;
  name: string;
  assistingFor: { ownerId: string; ownerName: string; permissions: Permission[] }[];
}
