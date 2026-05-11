import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiRequest, isApiError } from '../lib/apiClient';
import type { TicketResponse } from '../types/api';

export function TicketDetailPage() {
  const { ticketId } = useParams();
  const [ticket, setTicket] = useState<TicketResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!ticketId) {
      setErrorMessage('No ticket id was provided.');
      setIsLoading(false);
      return;
    }

    let isMounted = true;

    async function load() {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const data = await apiRequest<TicketResponse>(`/api/tickets/${ticketId}`);
        if (isMounted) {
          setTicket(data);
        }
      } catch (error) {
        if (!isMounted) {
          return;
        }

        if (isApiError(error)) {
          if (error.kind === 'network') {
            setErrorMessage(
              'Cannot reach backend. Verify API availability and VITE_API_BASE_URL.',
            );
          } else if (error.status === 403) {
            setErrorMessage(
              'You do not have permission to view this ticket in ResolveHub.',
            );
          } else if (error.status === 404) {
            setErrorMessage('Ticket not found.');
          } else {
            setErrorMessage('Failed to load ticket details. Please try again.');
          }
        } else {
          setErrorMessage('Failed to load ticket details. Please try again.');
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
  }, [ticketId]);

  function formatDate(value: string) {
    return new Date(value).toLocaleString();
  }

  function formatAssignee(assigneeId: string | null) {
    if (!assigneeId) {
      return 'Unassigned';
    }
    return `${assigneeId.slice(0, 8)}...`;
  }

  return (
    <section>
      <header className="page-header">
        <h1>Ticket Detail</h1>
        <div className="header-actions">
          <Link className="link-button link-button-secondary" to="/dashboard">
            Back to dashboard
          </Link>
          <Link className="link-button link-button-secondary" to="/tickets">
            Back to tickets
          </Link>
        </div>
      </header>

      {isLoading ? <p className="state-panel">Loading ticket details...</p> : null}
      {!isLoading && errorMessage ? (
        <p className="state-panel state-error" role="alert">
          {errorMessage}
        </p>
      ) : null}
      {!isLoading && !errorMessage && !ticket ? (
        <p className="state-panel muted-text">No ticket data was returned.</p>
      ) : null}

      {!isLoading && !errorMessage && ticket ? (
        <article className="detail-card">
          <h2>{ticket.title}</h2>
          <p className="detail-description">{ticket.description}</p>

          <dl className="detail-grid">
            <div className="detail-field">
              <dt>Status</dt>
              <dd>{ticket.status}</dd>
            </div>
            <div className="detail-field">
              <dt>Priority</dt>
              <dd>{ticket.priority}</dd>
            </div>
            <div className="detail-field">
              <dt>Category</dt>
              <dd>{ticket.category}</dd>
            </div>
            <div className="detail-field">
              <dt>Assignee</dt>
              <dd>{formatAssignee(ticket.assigneeId)}</dd>
            </div>
            <div className="detail-field">
              <dt>SLA due date</dt>
              <dd>{formatDate(ticket.slaDueAt)}</dd>
            </div>
            <div className="detail-field">
              <dt>Overdue</dt>
              <dd>
                <span
                  className={
                    ticket.overdue ? 'pill pill-danger' : 'pill pill-success'
                  }
                >
                  {ticket.overdue ? 'Overdue' : 'On track'}
                </span>
              </dd>
            </div>
            <div className="detail-field">
              <dt>Created at</dt>
              <dd>{formatDate(ticket.createdAt)}</dd>
            </div>
            <div className="detail-field">
              <dt>Updated at</dt>
              <dd>{formatDate(ticket.updatedAt)}</dd>
            </div>
          </dl>
        </article>
      ) : null}
    </section>
  );
}
