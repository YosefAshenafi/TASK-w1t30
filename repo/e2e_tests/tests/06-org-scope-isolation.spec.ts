import { test, expect } from './coverage.fixture';
import { API, apiLogin } from './helpers';

/**
 * Flow 6: Corporate Mentor scope isolation — Org-A mentor cannot see Org-B data.
 *
 * Seed fixtures used:
 *   mentor_org_a   — CORPORATE_MENTOR in MERIDIAN org
 *   mentor_org_b   — CORPORATE_MENTOR in PARTNER org
 *   student1       — STUDENT in MERIDIAN org
 *   student_org_b  — STUDENT in PARTNER org
 */
test.describe('Organisation scope isolation', () => {
  let mentorAToken: string;
  let mentorBToken: string;
  let orgAStudentId: string;
  let orgBStudentId: string;

  test.beforeAll(async ({ request }) => {
    const [resA, resB] = await Promise.all([
      apiLogin(request, 'mentor_org_a', 'E2eTest@123!'),
      apiLogin(request, 'mentor_org_b', 'E2eTest@123!'),
    ]);
    mentorAToken = resA.accessToken;
    mentorBToken = resB.accessToken;

    const adminRes = await apiLogin(request, 'admin', 'Admin@123!');
    const adminToken = adminRes.accessToken;

    const [orgARes, orgBRes] = await Promise.all([
      request.get(`${API}/api/v1/admin/users`, {
        headers: { Authorization: `Bearer ${adminToken}` },
        params: { orgCode: 'MERIDIAN', role: 'STUDENT', size: 10 },
      }),
      request.get(`${API}/api/v1/admin/users`, {
        headers: { Authorization: `Bearer ${adminToken}` },
        params: { orgCode: 'PARTNER', role: 'STUDENT', size: 10 },
      }),
    ]);
    const orgABody = await orgARes.json();
    const orgBBody = await orgBRes.json();
    orgAStudentId = orgABody.content?.[0]?.id;
    orgBStudentId = orgBBody.content?.[0]?.id;
  });

  test('6a: Org-A mentor can list sessions for Org-A student', async ({ request }) => {
    test.skip(!orgAStudentId, 'Seed missing Org-A student');
    const res = await request.get(`${API}/api/v1/sessions`, {
      headers: { Authorization: `Bearer ${mentorAToken}` },
      params: { learnerId: orgAStudentId },
    });
    expect(res.status()).toBe(200);
  });

  test('6b: Org-A mentor cannot see Org-B student analytics (403)', async ({ request }) => {
    test.skip(!orgBStudentId, 'Seed missing Org-B student');
    const res = await request.get(`${API}/api/v1/analytics/mastery-trends`, {
      headers: { Authorization: `Bearer ${mentorAToken}` },
      params: { learnerId: orgBStudentId },
    });
    expect(res.status()).toBe(403);
  });

  test('6c: Org-B mentor cannot see Org-A student analytics (403)', async ({ request }) => {
    test.skip(!orgAStudentId, 'Seed missing Org-A student');
    const res = await request.get(`${API}/api/v1/analytics/mastery-trends`, {
      headers: { Authorization: `Bearer ${mentorBToken}` },
      params: { learnerId: orgAStudentId },
    });
    expect(res.status()).toBe(403);
  });

  test('6d: Org-B mentor can see own org student analytics', async ({ request }) => {
    test.skip(!orgBStudentId, 'Seed missing Org-B student');
    const res = await request.get(`${API}/api/v1/analytics/mastery-trends`, {
      headers: { Authorization: `Bearer ${mentorBToken}` },
      params: { learnerId: orgBStudentId },
    });
    expect(res.status()).toBe(200);
  });
});
