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
  it('counts dashboard metrics from visible tickets correctly', () => {
    const stats = computeTicketStats([
      ticket({ id: 't1', status: 'OPEN', overdue: false, priority: 'URGENT' }),
      ticket({
        id: 't2',
        status: 'IN_PROGRESS',
        overdue: true,
        priority: 'HIGH',
        assigneeId: 'agent-1',
      }),
      ticket({
        id: 't3',
        status: 'WAITING_CUSTOMER',
        overdue: true,
        priority: 'LOW',
        assigneeId: 'agent-1',
      }),
      ticket({
        id: 't4',
        status: 'RESOLVED',
        overdue: true,
        priority: 'URGENT',
        assigneeId: 'agent-2',
      }),
      ticket({ id: 't5', status: 'CLOSED', overdue: false, priority: 'HIGH' }),
    ], 'agent-1');

    expect(stats).toEqual({
      total: 5,
      open: 3,
      overdue: 3,
      highUrgent: 2,
      assignedToMe: 2,
      unassigned: 2,
      waitingCustomer: 1,
      resolved: 1,
    });
  });

  it('returns zero assigned-to-me tickets when no current user id is available', () => {
    const stats = computeTicketStats([
      ticket({ id: 't1', assigneeId: 'agent-1' }),
      ticket({ id: 't2', assigneeId: null, status: 'RESOLVED' }),
    ]);

    expect(stats).toEqual({
      total: 2,
      open: 1,
      overdue: 0,
      highUrgent: 0,
      assignedToMe: 0,
      unassigned: 1,
      waitingCustomer: 0,
      resolved: 1,
    });
  });
});
