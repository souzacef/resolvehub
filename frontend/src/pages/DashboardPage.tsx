import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiRequest } from '../lib/apiClient';
import type { TicketResponse } from '../types/api';

export function DashboardPage() {
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
    const overdueCount = tickets.filter((ticket) => ticket.overdue).length;
    const openCount = tickets.filter(
      (ticket) => ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED',
    ).length;

    return {
      total: tickets.length,
      open: openCount,
      overdue: overdueCount,
    };
  }, [tickets]);

  return (
    <section>
      <header className="page-header">
        <h1>Dashboard</h1>
        <Link className="link-button" to="/tickets">
          View tickets
        </Link>
      </header>

      {isLoading ? <p>Loading dashboard...</p> : null}
      {error ? <p className="error-text">{error}</p> : null}

      <div className="stats-grid">
        <article className="stat-card">
          <h2>Total tickets</h2>
          <strong>{stats.total}</strong>
        </article>
        <article className="stat-card">
          <h2>Open tickets</h2>
          <strong>{stats.open}</strong>
        </article>
        <article className="stat-card">
          <h2>Overdue tickets</h2>
          <strong>{stats.overdue}</strong>
        </article>
      </div>
    </section>
  );
}
