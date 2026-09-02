import { describe, expect, it, vi } from 'vitest';
import { isBackendUp } from './backendHealth';

describe('isBackendUp', () => {
  it('returns true only when the actuator reports UP', async () => {
    const fetchImplementation = vi.fn<typeof fetch>().mockResolvedValue(
      new Response(JSON.stringify({ status: 'UP' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    await expect(isBackendUp(undefined, fetchImplementation)).resolves.toBe(true);
    expect(fetchImplementation).toHaveBeenCalledOnce();
    expect(String(fetchImplementation.mock.calls[0][0])).toContain('/actuator/health');
  });

  it('returns false for non-UP responses and network failures', async () => {
    const notReady = vi.fn<typeof fetch>().mockResolvedValue(
      new Response(JSON.stringify({ status: 'DOWN' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    const unavailable = vi.fn<typeof fetch>().mockRejectedValue(new Error('offline'));

    await expect(isBackendUp(undefined, notReady)).resolves.toBe(false);
    await expect(isBackendUp(undefined, unavailable)).resolves.toBe(false);
  });
});
