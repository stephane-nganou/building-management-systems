import { writeFileSync } from 'node:fs';

import { Page, expect, test } from '@playwright/test';

import { registerOwner } from './support';

/**
 * Writes as the signed in owner, through the same session and CSRF cookie the
 * application itself uses.
 */
async function postJson(page: Page, path: string, data?: object): Promise<string> {
  const token = (await page.context().cookies()).find(
    (cookie) => cookie.name === 'XSRF-TOKEN',
  )!.value;
  const response = await page.request.post(path, { data, headers: { 'X-XSRF-TOKEN': token } });
  expect(response.ok(), `${path} answered ${response.status()}`).toBeTruthy();
  return (await response.json()).id;
}

async function pdf(page: Page, invoice: string): Promise<Buffer> {
  const response = await page.request.get(`/api/invoices/${invoice}/pdf`);
  expect(response.headers()['content-type']).toContain('application/pdf');
  return response.body();
}

test('an issued invoice downloads signed by the application, a draft does not', async ({
  page,
}, testInfo) => {
  await registerOwner(page, 'signed');

  const building = await postJson(page, '/api/buildings', {
    name: `Signed ${Date.now()}`,
    street: 'Hauptstrasse 1',
    city: 'Berlin',
    postalCode: '10115',
    country: 'DE',
    currency: 'EUR',
  });
  const apartment = await postJson(page, `/api/buildings/${building}/apartments`, {
    label: '1A',
    floor: 1,
    sizeSqm: 72.5,
    rooms: 3,
    bedrooms: 2,
    bathrooms: 1,
    kitchens: 1,
    toilets: 1,
    baseRent: 850,
    utilitiesAdvance: 150,
    status: 'OCCUPIED',
  });
  const tenant = await postJson(page, `/api/apartments/${apartment}/tenants`, {
    firstName: 'Alex',
    lastName: 'Meier',
    leaseStart: '2026-01-01',
    deposit: 1800,
    active: true,
  });
  const invoice = await postJson(page, '/api/invoices', {
    tenantId: tenant,
    type: 'RENT',
    periodStart: '2026-02-01',
    periodEnd: '2026-02-28',
    issueDate: '2026-02-01',
    dueDate: '2026-02-15',
  });

  expect((await pdf(page, invoice)).includes('/ETSI.CAdES.detached')).toBe(false);

  await postJson(page, `/api/invoices/${invoice}/status?status=SENT`);
  const sent = await pdf(page, invoice);
  expect(sent.includes('/ETSI.CAdES.detached')).toBe(true);

  // Kept for a look in a PDF reader, or an independent validator.
  writeFileSync(testInfo.outputPath('signed-invoice.pdf'), sent);
});
