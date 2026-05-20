import type { TicketResponse } from '../types/api';

export type TicketStats = {
  total: number;
  open: number;
  overdue: number;
  highUrgent: number;
};

function isActiveTicket(ticket: TicketResponse): boolean {
  return ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED';
}

export function computeTicketStats(tickets: TicketResponse[]): TicketStats {
  const overdue = tickets.filter((ticket) => ticket.overdue).length;
  const open = tickets.filter((ticket) => isActiveTicket(ticket)).length;
  const highUrgent = tickets.filter(
    (ticket) =>
      isActiveTicket(ticket) &&
      (ticket.priority === 'HIGH' || ticket.priority === 'URGENT'),
  ).length;

  return {
    total: tickets.length,
    open,
    overdue,
    highUrgent,
  };
}
