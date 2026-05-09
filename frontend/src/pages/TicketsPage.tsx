import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import { apiRequest } from '../lib/apiClient';
import type { TicketResponse } from '../types/api';

export function TicketsPage() {
  const { canCreateTickets } = useAuth();
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
          setError('Failed to load tickets.');
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

  function formatAssignee(assigneeId: string | null) {
    if (!assigneeId) {
      return 'Unassigned';
    }
    return `${assigneeId.slice(0, 8)}...`;
  }

  return (
    <section>
      <header className="page-header">
        <h1>Tickets</h1>
        <div className="header-actions">
          {canCreateTickets ? (
            <Link className="link-button" to="/tickets/new">
              Create ticket
            </Link>
          ) : null}
        </div>
      </header>

      {isLoading ? <p className="state-panel">Loading tickets...</p> : null}
      {error ? (
        <p className="state-panel state-error" role="alert">
          {error}
        </p>
      ) : null}
      {!isLoading && !error && tickets.length === 0 ? (
        <p className="state-panel muted-text">
          No tickets were returned by the API.
        </p>
      ) : null}

      {!isLoading && !error && tickets.length > 0 ? (
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Title</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Category</th>
                <th>Assignee</th>
                <th>SLA due</th>
                <th>Overdue</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((ticket) => (
                <tr key={ticket.id}>
                  <td>
                    <Link to={`/tickets/${ticket.id}`}>{ticket.title}</Link>
                  </td>
                  <td>{ticket.status}</td>
                  <td>{ticket.priority}</td>
                  <td>{ticket.category}</td>
                  <td>{formatAssignee(ticket.assigneeId)}</td>
                  <td>{new Date(ticket.slaDueAt).toLocaleString()}</td>
                  <td>
                    <span
                      className={
                        ticket.overdue ? 'pill pill-danger' : 'pill pill-success'
                      }
                    >
                      {ticket.overdue ? 'Overdue' : 'On track'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}
