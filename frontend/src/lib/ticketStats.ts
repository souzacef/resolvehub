import type { TicketResponse } from '../types/api';

export type TicketStats = {
  total: number;
  open: number;
  overdue: number;
};

export function computeTicketStats(tickets: TicketResponse[]): TicketStats {
  const overdue = tickets.filter((ticket) => ticket.overdue).length;
  const open = tickets.filter(
    (ticket) => ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED',
  ).length;

  return {
    total: tickets.length,
    open,
    overdue,
  };
}
