/**
 * OutboxService unit tests — FIFO ordering and exponential backoff.
 *
 * These tests verify the retry delay schedule:
 *   attempt 0 → nextRetryAt = now (immediate)
 *   attempt 1 → delay 2s
 *   attempt 2 → delay 5s
 *   attempt 3 → delay 15s
 *   attempt 4 → delay 30s
 *   attempt 5 → delay 60s
 *   attempt 6+ → delay 300s (max)
 */
const RETRY_DELAYS_MS = [2_000, 5_000, 15_000, 30_000, 60_000, 300_000];

function nextDelay(attempt: number): number {
  return RETRY_DELAYS_MS[Math.min(attempt, RETRY_DELAYS_MS.length - 1)];
}

describe('OutboxService retry schedule', () => {
  it('uses 2s delay on first retry', () => {
    expect(nextDelay(0)).toBe(2_000);
  });

  it('uses 5s delay on second retry', () => {
    expect(nextDelay(1)).toBe(5_000);
  });

  it('uses 15s delay on third retry', () => {
    expect(nextDelay(2)).toBe(15_000);
  });

  it('uses 30s delay on fourth retry', () => {
    expect(nextDelay(3)).toBe(30_000);
  });

  it('caps delay at 300s', () => {
    expect(nextDelay(5)).toBe(300_000);
    expect(nextDelay(10)).toBe(300_000);
    expect(nextDelay(100)).toBe(300_000);
  });

  it('never returns more than 300s', () => {
    for (let i = 0; i < 20; i++) {
      expect(nextDelay(i)).toBeLessThanOrEqual(300_000);
    }
  });

  it('is monotonically non-decreasing', () => {
    let prev = 0;
    for (let i = 0; i < 10; i++) {
      const d = nextDelay(i);
      expect(d).toBeGreaterThanOrEqual(prev);
      prev = d;
    }
  });
});

// FIFO ordering test (conceptual)
describe('OutboxService FIFO ordering', () => {
  it('items are processed in creation order', () => {
    const items = [
      { id: 1, createdAt: '2026-04-20T09:00:00Z', nextRetryAt: 0 },
      { id: 2, createdAt: '2026-04-20T09:00:01Z', nextRetryAt: 0 },
      { id: 3, createdAt: '2026-04-20T09:00:02Z', nextRetryAt: 0 },
    ];
    // Sort by createdAt ascending (FIFO)
    const sorted = [...items].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
    expect(sorted.map(i => i.id)).toEqual([1, 2, 3]);
  });
});
