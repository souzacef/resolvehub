import { describe, expect, it } from 'vitest';
import { computeTicketStats } from './ticketStats';
import type { TicketResponse } from '../types/api';

function ticket(overrides: Partial<TicketResponse>): TicketResponse {
  return {
    id: '1',
    organizationId: 'org',
    requesterId: 'req',
    assigneeId: null,
    title: 'Sample',
    description: 'Sample description',
    status: 'OPEN',
    priority: 'MEDIUM',
    category: 'OTHER',
    slaDueAt: new Date().toISOString(),
    overdue: false,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  };
}

describe('computeTicketStats', () => {
  it('counts total, open, overdue, and high/urgent tickets correctly', () => {
    const stats = computeTicketStats([
      ticket({ id: 't1', status: 'OPEN', overdue: false, priority: 'URGENT' }),
      ticket({ id: 't2', status: 'IN_PROGRESS', overdue: true, priority: 'HIGH' }),
      ticket({ id: 't3', status: 'RESOLVED', overdue: true, priority: 'MEDIUM' }),
      ticket({ id: 't4', status: 'CLOSED', overdue: false, priority: 'LOW' }),
    ]);

    expect(stats).toEqual({
      total: 4,
      open: 2,
      overdue: 2,
      highUrgent: 2,
    });
  });
});
