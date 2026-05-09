import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiRequest } from '../lib/apiClient';
import { computeTicketStats } from '../lib/ticketStats';
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
    return computeTicketStats(tickets);
  }, [tickets]);

  return (
    <section>
      <header className="page-header">
        <h1>Dashboard</h1>
        <div className="header-actions">
          <Link className="link-button link-button-secondary" to="/tickets">
            View tickets
          </Link>
          <Link className="link-button" to="/tickets/new">
            Create ticket
          </Link>
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
        <article className="stat-card">
          <h2>High/Urgent priority</h2>
          <strong>{stats.highUrgent}</strong>
        </article>
      </div>
    </section>
  );
}
