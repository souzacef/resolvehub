import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import { listOrganizationUsers } from '../features/tickets/tickets';
import { apiRequest } from '../lib/apiClient';
import type { OrganizationUserResponse, TicketResponse } from '../types/api';

export function TicketsPage() {
  const { canCreateTickets, role } = useAuth();
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [organizationUsers, setOrganizationUsers] = useState<
    OrganizationUserResponse[]
  >([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function load() {
      try {
        const data = await apiRequest<TicketResponse[]>('/api/tickets');
        let users: OrganizationUserResponse[] = [];
        if (role !== 'CUSTOMER') {
          try {
            users = await listOrganizationUsers();
          } catch {
            users = [];
          }
        }

        if (isMounted) {
          setTickets(data);
          setOrganizationUsers(users);
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
  }, [role]);

  function shortenId(id: string) {
    return `${id.slice(0, 8)}...`;
  }

  function formatUserLabel(userId: string) {
    const user = organizationUsers.find((candidate) => candidate.id === userId);
    if (!user) {
      return shortenId(userId);
    }

    if (user.name && user.name.trim().length > 0) {
      return `${user.name.trim()} (${user.email})`;
    }

    return user.email;
  }

  function formatAssignee(assigneeId: string | null) {
    if (!assigneeId) {
      return 'Unassigned';
    }
    return formatUserLabel(assigneeId);
  }

  const showAssigneeColumn = role !== 'CUSTOMER';
  const isCustomer = role === 'CUSTOMER';

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
                {showAssigneeColumn ? <th>Assignee</th> : null}
                <th>{isCustomer ? 'Expected response by' : 'SLA due at'}</th>
                <th>{isCustomer ? 'SLA status' : 'Overdue'}</th>
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
                  {showAssigneeColumn ? (
                    <td>{formatAssignee(ticket.assigneeId)}</td>
                  ) : null}
                  <td>{new Date(ticket.slaDueAt).toLocaleString()}</td>
                  <td>
                    {isCustomer ? (
                      ticket.overdue ? 'Overdue' : 'On track'
                    ) : (
                      <span
                        className={
                          ticket.overdue ? 'pill pill-danger' : 'pill pill-success'
                        }
                      >
                        {ticket.overdue ? 'Overdue' : 'On track'}
                      </span>
                    )}
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
