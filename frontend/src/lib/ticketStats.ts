import type { TicketResponse } from '../types/api';

export type TicketStats = {
  total: number;
  open: number;
  overdue: number;
  highUrgent: number;
  assignedToMe: number;
  unassigned: number;
  waitingCustomer: number;
  resolved: number;
};

function isActiveTicket(ticket: TicketResponse): boolean {
  return ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED';
}

export function computeTicketStats(
  tickets: TicketResponse[],
  currentUserId?: string | null,
): TicketStats {
  const overdue = tickets.filter((ticket) => ticket.overdue).length;
  const open = tickets.filter((ticket) => isActiveTicket(ticket)).length;
  const highUrgent = tickets.filter(
    (ticket) =>
      isActiveTicket(ticket) &&
      (ticket.priority === 'HIGH' || ticket.priority === 'URGENT'),
  ).length;
  const assignedToMe = currentUserId
    ? tickets.filter((ticket) => ticket.assigneeId === currentUserId).length
    : 0;
  const unassigned = tickets.filter((ticket) => !ticket.assigneeId).length;
  const waitingCustomer = tickets.filter(
    (ticket) => ticket.status === 'WAITING_CUSTOMER',
  ).length;
  const resolved = tickets.filter((ticket) => ticket.status === 'RESOLVED').length;

  return {
    total: tickets.length,
    open,
    overdue,
    highUrgent,
    assignedToMe,
    unassigned,
    waitingCustomer,
    resolved,
  };
}
