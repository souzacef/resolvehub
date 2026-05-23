import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import { listOrganizationUsers } from '../features/tickets/tickets';
import { apiRequest } from '../lib/apiClient';
import type {
  OrganizationUserResponse,
  TicketCategory,
  TicketPriority,
  TicketResponse,
  TicketStatus,
} from '../types/api';

const statusFilters: Array<TicketStatus | 'ALL'> = [
  'ALL',
  'OPEN',
  'IN_PROGRESS',
  'WAITING_CUSTOMER',
  'RESOLVED',
  'CLOSED',
];

const priorityFilters: Array<TicketPriority | 'ALL'> = [
  'ALL',
  'LOW',
  'MEDIUM',
  'HIGH',
  'URGENT',
];

const categoryFilters: Array<TicketCategory | 'ALL'> = [
  'ALL',
  'BILLING',
  'TECHNICAL',
  'ACCOUNT',
  'FEATURE_REQUEST',
  'SECURITY',
  'OTHER',
];

export function TicketsPage() {
  const { canCreateTickets, role } = useAuth();
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [organizationUsers, setOrganizationUsers] = useState<
    OrganizationUserResponse[]
  >([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<TicketStatus | 'ALL'>('ALL');
  const [priorityFilter, setPriorityFilter] = useState<TicketPriority | 'ALL'>(
    'ALL',
  );
  const [categoryFilter, setCategoryFilter] = useState<TicketCategory | 'ALL'>(
    'ALL',
  );
  const [overdueOnly, setOverdueOnly] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function load() {
      setIsLoading(true);
      setError(null);
      try {
        const listPath = overdueOnly ? '/api/tickets?overdue=true' : '/api/tickets';
        const data = await apiRequest<TicketResponse[]>(listPath);
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
  }, [role, overdueOnly]);

  const filteredTickets = useMemo(() => {
    const normalizedSearch = searchQuery.trim().toLowerCase();

    return tickets.filter((ticket) => {
      const matchesSearch =
        normalizedSearch.length === 0 ||
        ticket.ticketNumber.toLowerCase().includes(normalizedSearch) ||
        ticket.title.toLowerCase().includes(normalizedSearch) ||
        ticket.description.toLowerCase().includes(normalizedSearch);

      const matchesStatus =
        statusFilter === 'ALL' || ticket.status === statusFilter;
      const matchesPriority =
        priorityFilter === 'ALL' || ticket.priority === priorityFilter;
      const matchesCategory =
        categoryFilter === 'ALL' || ticket.category === categoryFilter;

      return matchesSearch && matchesStatus && matchesPriority && matchesCategory;
    });
  }, [tickets, searchQuery, statusFilter, priorityFilter, categoryFilter]);

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

  function resetFilters() {
    setSearchQuery('');
    setStatusFilter('ALL');
    setPriorityFilter('ALL');
    setCategoryFilter('ALL');
    setOverdueOnly(false);
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

      <section className="comment-form" aria-label="Ticket filters">
        <label htmlFor="ticket-search">Search tickets</label>
        <input
          id="ticket-search"
          name="ticket-search"
          type="search"
          value={searchQuery}
          onChange={(event) => setSearchQuery(event.target.value)}
          placeholder="Search ticket number, title, or description"
        />

        <div className="form-grid">
          <div>
            <label htmlFor="ticket-status-filter">Status</label>
            <select
              id="ticket-status-filter"
              name="ticket-status-filter"
              value={statusFilter}
              onChange={(event) =>
                setStatusFilter(event.target.value as TicketStatus | 'ALL')
              }
            >
              {statusFilters.map((status) => (
                <option key={status} value={status}>
                  {status === 'ALL' ? 'All statuses' : status}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="ticket-priority-filter">Priority</label>
            <select
              id="ticket-priority-filter"
              name="ticket-priority-filter"
              value={priorityFilter}
              onChange={(event) =>
                setPriorityFilter(event.target.value as TicketPriority | 'ALL')
              }
            >
              {priorityFilters.map((priority) => (
                <option key={priority} value={priority}>
                  {priority === 'ALL' ? 'All priorities' : priority}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="ticket-category-filter">Category</label>
            <select
              id="ticket-category-filter"
              name="ticket-category-filter"
              value={categoryFilter}
              onChange={(event) =>
                setCategoryFilter(event.target.value as TicketCategory | 'ALL')
              }
            >
              {categoryFilters.map((category) => (
                <option key={category} value={category}>
                  {category === 'ALL' ? 'All categories' : category}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="ticket-overdue-filter">Overdue</label>
            <select
              id="ticket-overdue-filter"
              name="ticket-overdue-filter"
              value={overdueOnly ? 'OVERDUE_ONLY' : 'ALL'}
              onChange={(event) =>
                setOverdueOnly(event.target.value === 'OVERDUE_ONLY')
              }
            >
              <option value="ALL">All tickets</option>
              <option value="OVERDUE_ONLY">Overdue only</option>
            </select>
          </div>
        </div>

        <div className="form-actions">
          <button type="button" onClick={resetFilters}>
            Clear filters
          </button>
        </div>
      </section>

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
      {!isLoading && !error && tickets.length > 0 && filteredTickets.length === 0 ? (
        <p className="state-panel muted-text">
          No tickets match the current filters.
        </p>
      ) : null}

      {!isLoading && !error && filteredTickets.length > 0 ? (
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Ticket</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Category</th>
                {showAssigneeColumn ? <th>Assignee</th> : null}
                <th>{isCustomer ? 'Expected response by' : 'SLA due at'}</th>
                <th>{isCustomer ? 'Response status' : 'Overdue'}</th>
              </tr>
            </thead>
            <tbody>
              {filteredTickets.map((ticket) => (
                <tr key={ticket.id}>
                  <td>
                    <Link to={`/tickets/${ticket.id}`}>
                      {ticket.ticketNumber} — {ticket.title}
                    </Link>
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
                      ticket.overdue ? 'Past expected response' : 'On track'
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
