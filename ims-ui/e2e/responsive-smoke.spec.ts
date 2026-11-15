import { expect, test, type Page } from '@playwright/test';

/** Primary list routes — high traffic / finance / ops. */
const LIST_ROUTES = [
  '/dashboard',
  '/students',
  '/faculty',
  '/staff',
  '/courses',
  '/batches',
  '/admissions',
  '/enquiries',
  '/enrollments',
  '/outstanding',
  '/refunds',
  '/finance-reports',
  '/reports/students',
  '/attendance',
  '/timetable',
  '/exams',
  '/users',
  '/imports',
];

async function assertNoPageHorizontalScroll(page: Page) {
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement;
    const body = document.body;
    return {
      doc: doc.scrollWidth - doc.clientWidth,
      body: body.scrollWidth - body.clientWidth,
    };
  });
  expect(overflow.doc, `documentElement horizontal overflow ${overflow.doc}px`).toBeLessThan(8);
  expect(overflow.body, `body horizontal overflow ${overflow.body}px`).toBeLessThan(8);
}

test.describe('Responsive smoke', () => {
  test('shell has no page-level horizontal scroll after login', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await assertNoPageHorizontalScroll(page);
  });

  for (const route of LIST_ROUTES) {
    test(`${route} has no page-level horizontal scroll`, async ({ page }) => {
      await page.goto(route);
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(250);
      await assertNoPageHorizontalScroll(page);
    });
  }

  test('mobile drawer opens at ≤767', async ({ page }, testInfo) => {
    test.skip(!testInfo.project.name.includes('mobile'), 'drawer check is mobile-only');
    await page.goto('/dashboard');
    await page.waitForLoadState('domcontentloaded');
    const open = page.locator('button.header__menu');
    await expect(open).toBeVisible();
    await open.click();
    await expect(page.locator('.shell--drawer-open')).toBeVisible({ timeout: 5_000 });
    await assertNoPageHorizontalScroll(page);
  });
});
