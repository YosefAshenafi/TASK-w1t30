import { test, expect } from './coverage.fixture';
import { API, apiLogin, adminApproveUser, loginViaUI } from './helpers';

/**
 * Flow 1: Register → PENDING → admin approves → login → home
 */
test.describe('Registration and approval flow', () => {
  const username = `e2e_student_${Date.now()}`;
  let userId: string;
  let adminToken: string;

  test('1a: register new student routes to login with registered flag', async ({ page, request }) => {
    await page.goto('/register');
    await page.getByLabel('Username').fill(username);
    await page.getByLabel('Display name').fill('E2E Student');
    await page.getByLabel('Role').selectOption('STUDENT');
    await page.getByLabel('Password', { exact: true }).fill('E2eTest@123!');
    await page.getByLabel('Confirm password').fill('E2eTest@123!');
    await page.getByRole('button', { name: /create account/i }).click();

    await expect(page).toHaveURL(/\/login\?registered=1/);
  });

  test('1b: admin fetches user list to find new student', async ({ request }) => {
    const loginRes = await apiLogin(request, 'admin', 'Admin@123!');
    adminToken = loginRes.accessToken;
    expect(adminToken).toBeTruthy();

    const usersRes = await request.get(`${API}/api/v1/admin/users`, {
      headers: { Authorization: `Bearer ${adminToken}` },
      params: { status: 'PENDING' },
    });
    const body = await usersRes.json();
    const found = body.content.find((u: { username: string; id: string }) => u.username === username);
    expect(found).toBeTruthy();
    userId = found.id;
  });

  test('1c: admin approves student account', async ({ request }) => {
    await adminApproveUser(request, adminToken, userId);
    const userRes = await request.get(`${API}/api/v1/admin/users/${userId}`, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    const user = await userRes.json();
    expect(user.status).toBe('ACTIVE');
  });

  test('1d: student logs in and reaches home page', async ({ page }) => {
    await loginViaUI(page, username, 'E2eTest@123!');
    await expect(page).toHaveURL(/home/);
    await expect(page.getByRole('navigation')).toBeVisible();
  });
});
