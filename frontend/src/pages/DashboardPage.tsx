import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import { apiRequest } from '../lib/apiClient';
import { computeTicketStats } from '../lib/ticketStats';
import type { TicketResponse } from '../types/api';

export function DashboardPage() {
  const { canCreateTickets, role, userId } = useAuth();
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function load() {
      try {
        const data = await apiRequest<TicketResponse[]>('/api/tickets');
        if (isMounted) {
          setTickets(data);
        }
      } catch {
        if (isMounted) {
          setError('Failed to load dashboard metrics.');
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void load();

    return () => {
      isMounted = false;
    };
  }, []);

  const stats = useMemo(() => {
    return computeTicketStats(tickets, userId);
  }, [tickets, userId]);

  const isCustomer = role === 'CUSTOMER';

  const customerResolvedCount = useMemo(
    () =>
      tickets.filter(
        (ticket) => ticket.status === 'RESOLVED' || ticket.status === 'CLOSED',
      ).length,
    [tickets],
  );

  const customerCards = [
    { label: 'My tickets', value: stats.total },
    { label: 'Open tickets', value: stats.open },
    { label: 'Resolved tickets', value: customerResolvedCount },
    { label: 'High/Urgent tickets', value: stats.highUrgent },
  ];

  const staffCards = [
    { label: 'Total tickets', value: stats.total },
    { label: 'Open tickets', value: stats.open },
    { label: 'Overdue tickets', value: stats.overdue },
    { label: 'High/Urgent priority', value: stats.highUrgent },
    { label: 'Assigned to me', value: stats.assignedToMe },
    { label: 'Unassigned tickets', value: stats.unassigned },
    { label: 'Waiting customer', value: stats.waitingCustomer },
    { label: 'Resolved tickets', value: stats.resolved },
  ];

  const cards = isCustomer ? customerCards : staffCards;

  return (
    <section>
      <header className="page-header">
        <h1>Dashboard</h1>
        <div className="header-actions">
          <Link className="link-button link-button-secondary" to="/tickets">
            View tickets
          </Link>
          {canCreateTickets ? (
            <Link className="link-button" to="/tickets/new">
              Create ticket
            </Link>
          ) : null}
        </div>
      </header>

      {isLoading ? <p className="state-panel">Loading dashboard...</p> : null}
      {error ? (
        <p className="state-panel state-error" role="alert">
          {error}
        </p>
      ) : null}
      {!isLoading && !error && tickets.length === 0 ? (
        <p className="state-panel muted-text">
          No tickets yet. Once tickets are created, summary metrics will appear
          here.
        </p>
      ) : null}

      <div className="stats-grid">
        {cards.map((card) => (
          <article className="stat-card" key={card.label}>
            <h2>{card.label}</h2>
            <strong>{card.value}</strong>
          </article>
        ))}
      </div>
    </section>
  );
}
