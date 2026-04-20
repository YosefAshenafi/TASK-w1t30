import { Page, APIRequestContext } from '@playwright/test';

export const API = process.env['API_URL'] ?? 'http://localhost:8080';

export async function apiLogin(request: APIRequestContext, username: string, password: string) {
  const res = await request.post(`${API}/api/v1/auth/login`, {
    data: { username, password, deviceFingerprint: 'e2e-test-fingerprint' },
  });
  return res.json();
}

export async function adminApproveUser(request: APIRequestContext, adminToken: string, userId: string) {
  await request.post(`${API}/api/v1/admin/users/${userId}/approve`, {
    headers: { Authorization: `Bearer ${adminToken}` },
  });
}

export async function loginViaUI(page: Page, username: string, password: string) {
  await page.goto('/login');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: /sign in/i }).click();
}
