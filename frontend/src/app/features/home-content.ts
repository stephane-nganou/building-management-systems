import { MessageKey } from '../i18n/en';
import { Unit } from '../shared/facade';
import { IconName } from '../shared/icon';

/** One way to reach us. */
export interface Contact {
  icon: IconName;
  label: MessageKey;
  value: string;
  /** Where a click goes; without one the value is shown as plain text. */
  href?: string;
}

/**
 * How to reach us, shown on the landing page. This list is the one place to
 * change it: add, remove or reorder entries, and give a new label its key in
 * both dictionaries. The values are placeholders until launch.
 */
export const CONTACTS: Contact[] = [
  {
    icon: 'mail',
    label: 'home.contact.email',
    value: 'contact@example.com',
    href: 'mailto:contact@example.com',
  },
  {
    icon: 'phone',
    label: 'home.contact.phone',
    value: '+00 000 000 000',
    href: 'tel:+00000000000',
  },
  { icon: 'pin', label: 'home.contact.address', value: '1 Example Street, 00000 City' },
];

export interface Feature {
  icon: IconName;
  title: MessageKey;
  body: MessageKey;
}

export const FEATURES: Feature[] = [
  { icon: 'building', title: 'home.feature.buildings.title', body: 'home.feature.buildings.body' },
  { icon: 'door', title: 'home.feature.apartments.title', body: 'home.feature.apartments.body' },
  { icon: 'users', title: 'home.feature.tenants.title', body: 'home.feature.tenants.body' },
  { icon: 'receipt', title: 'home.feature.expenses.title', body: 'home.feature.expenses.body' },
  { icon: 'invoice', title: 'home.feature.invoices.title', body: 'home.feature.invoices.body' },
  {
    icon: 'assistant',
    title: 'home.feature.assistants.title',
    body: 'home.feature.assistants.body',
  },
  { icon: 'dashboard', title: 'home.feature.summary.title', body: 'home.feature.summary.body' },
  { icon: 'chart', title: 'home.feature.profitLoss.title', body: 'home.feature.profitLoss.body' },
];

/** Signing up to a first statement: a real sequence, so it is numbered. */
export const STEPS: { title: MessageKey; body: MessageKey }[] = [
  { title: 'home.how.step1.title', body: 'home.how.step1.body' },
  { title: 'home.how.step2.title', body: 'home.how.step2.body' },
  { title: 'home.how.step3.title', body: 'home.how.step3.body' },
];

export const QUESTIONS: { question: MessageKey; answer: MessageKey }[] = [
  { question: 'home.faq.q1', answer: 'home.faq.a1' },
  { question: 'home.faq.q2', answer: 'home.faq.a2' },
  { question: 'home.faq.q3', answer: 'home.faq.a3' },
  { question: 'home.faq.q4', answer: 'home.faq.a4' },
  { question: 'home.faq.q5', answer: 'home.faq.a5' },
];

function building(
  floors: number,
  perFloor: number,
  vacant: number[] = [],
  repairs: number[] = [],
): Unit[] {
  return Array.from({ length: floors * perFloor }, (_, index) => ({
    floor: Math.floor(index / perFloor),
    status: vacant.includes(index)
      ? 'VACANT'
      : repairs.includes(index)
        ? 'MAINTENANCE'
        : 'OCCUPIED',
  }));
}

/** The street across the top of the page: illustration, not anybody's data. */
export const STREET: Unit[][] = [
  building(2, 2, [1]),
  building(5, 3, [4, 11]),
  building(3, 4, [2], [9]),
  building(6, 2, [7]),
  building(4, 3, [0, 10]),
  building(2, 3),
];
