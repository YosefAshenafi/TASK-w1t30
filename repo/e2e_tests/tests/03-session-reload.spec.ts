import { test, expect } from './coverage.fixture';
import { loginViaUI } from './helpers';

/**
 * Flow 3: Continue-Session after simulated browser reload — IndexedDB cache restores state.
 */
test.describe('Session continuity after reload', () => {
  const studentUser = 'student_e2e_reload';
  let sessionUrl: string;

  test('3a: start a session and add one set', async ({ page }) => {
    await loginViaUI(page, studentUser, 'E2eTest@123!');
    await page.getByRole('link', { name: /sessions/i }).click();
    await page.getByRole('button', { name: /new session/i }).click();
    await page.getByLabel(/course/i).selectOption({ index: 1 });
    await page.getByRole('button', { name: /start/i }).click();

    sessionUrl = page.url();
    await expect(page).toHaveURL(/run/);

    await page.getByRole('button', { name: /\+ Add set/i }).click();
    await expect(page.getByText(/Set 1/i)).toBeVisible();
  });

  test('3b: reload the page and session state is restored', async ({ page }) => {
    await loginViaUI(page, studentUser, 'E2eTest@123!');
    await page.goto(sessionUrl);

    await expect(page).toHaveURL(/run/);
    await expect(page.getByText(/Set 1/i)).toBeVisible();
    await expect(page.getByText(/Session in progress/i)).toBeVisible();
  });

  test('3c: can continue adding sets after reload', async ({ page }) => {
    await loginViaUI(page, studentUser, 'E2eTest@123!');
    await page.goto(sessionUrl);

    await page.getByRole('button', { name: /\+ Add set/i }).click();
    await expect(page.getByText(/Set 2/i)).toBeVisible();
  });
});
