import { expect, test } from '@playwright/test';

import { DEMO_OWNER, signIn } from './support';

/**
 * The page is served with a strict Content-Security-Policy, and the app runs
 * under it untouched. A violation here most likely means the inline theme
 * script in index.html changed without its hash in nginx.conf.template, or a
 * build step started emitting inline handlers again.
 */
test('the app runs under its content security policy without a violation', async ({ page }) => {
  const violations: string[] = [];
  page.on('console', (message) => {
    if (message.text().includes('Content Security Policy')) {
      violations.push(message.text());
    }
  });

  const response = await page.goto('/');
  expect(response?.headers()['content-security-policy']).toContain("default-src 'self'");

  await signIn(page, DEMO_OWNER.username, DEMO_OWNER.password);
  await expect(page.locator('aside.spine')).toBeVisible();
  for (const label of ['Buildings', 'Expenses', 'Invoices', 'Profit and loss']) {
    await page.getByRole('link', { name: label }).click();
    await expect(page.getByRole('heading', { name: label, exact: false }).first()).toBeVisible();
  }
  await page.reload();
  await expect(page.locator('aside.spine')).toBeVisible();
  // The inline script ran: the theme was set before Angular started.
  await expect(page.locator('html')).toHaveAttribute('data-theme', /.+/);

  expect(violations).toEqual([]);
});
