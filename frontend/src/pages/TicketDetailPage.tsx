import { FormEvent, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import {
  createTicketComment,
  getTicket,
  listTicketComments,
} from '../features/tickets/tickets';
import { isApiError } from '../lib/apiClient';
import type { TicketCommentResponse, TicketResponse } from '../types/api';

export function TicketDetailPage() {
  const { role } = useAuth();
  const { ticketId } = useParams();
  const [ticket, setTicket] = useState<TicketResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [comments, setComments] = useState<TicketCommentResponse[]>([]);
  const [commentsErrorMessage, setCommentsErrorMessage] = useState<string | null>(
    null,
  );
  const [isCommentsLoading, setIsCommentsLoading] = useState(false);
  const [commentBody, setCommentBody] = useState('');
  const [isInternalComment, setIsInternalComment] = useState(false);
  const [commentErrorMessage, setCommentErrorMessage] = useState<string | null>(null);
  const [isSubmittingComment, setIsSubmittingComment] = useState(false);

  const canCreateInternalComments =
    role === 'AGENT' || role === 'MANAGER' || role === 'ADMIN';

  useEffect(() => {
    if (!ticketId) {
      setErrorMessage('Ticket id was not provided.');
      setIsLoading(false);
      return;
    }
    const requiredTicketId = ticketId;

    let isMounted = true;

    async function load() {
      setIsLoading(true);
      setErrorMessage(null);
      setTicket(null);
      setComments([]);
      setCommentsErrorMessage(null);
      setCommentErrorMessage(null);

      try {
        const data = await getTicket(requiredTicketId);
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

  useEffect(() => {
    if (!ticketId || !ticket) {
      return;
    }
    const requiredTicketId = ticketId;

    let isMounted = true;

    async function loadComments() {
      setIsCommentsLoading(true);
      setCommentsErrorMessage(null);

      try {
        const data = await listTicketComments(requiredTicketId);
        if (isMounted) {
          setComments(data);
        }
      } catch (error) {
        if (!isMounted) {
          return;
        }
        setCommentsErrorMessage(mapCommentLoadError(error));
      } finally {
        if (isMounted) {
          setIsCommentsLoading(false);
        }
      }
    }

    void loadComments();

    return () => {
      isMounted = false;
    };
  }, [ticketId, ticket]);

  async function handleCommentSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!ticketId) {
      setCommentErrorMessage('Ticket id was not provided.');
      return;
    }
    const requiredTicketId = ticketId;

    const trimmedBody = commentBody.trim();
    if (trimmedBody.length === 0) {
      setCommentErrorMessage('Comment body is required.');
      return;
    }

    setIsSubmittingComment(true);
    setCommentErrorMessage(null);

    try {
      await createTicketComment(requiredTicketId, {
        body: trimmedBody,
        internal: canCreateInternalComments ? isInternalComment : false,
      });

      setCommentBody('');
      setIsInternalComment(false);

      const refreshedComments = await listTicketComments(requiredTicketId);
      setComments(refreshedComments);
    } catch (error) {
      setCommentErrorMessage(mapCommentSubmitError(error));
    } finally {
      setIsSubmittingComment(false);
    }
  }

  function formatDate(value: string) {
    return new Date(value).toLocaleString();
  }

  function formatAssignee(assigneeId: string | null) {
    if (!assigneeId) {
      return 'Unassigned';
    }
    return `${assigneeId.slice(0, 8)}...`;
  }

  function formatAuthor(authorId: string) {
    return `${authorId.slice(0, 8)}...`;
  }

  function mapCommentLoadError(error: unknown): string {
    if (!isApiError(error)) {
      return 'Failed to load comments. Please try again.';
    }

    if (error.kind === 'network') {
      return 'Cannot reach backend. Verify API availability and VITE_API_BASE_URL.';
    }

    if (error.status === 403) {
      return 'You do not have permission to view comments for this ticket.';
    }

    if (error.status === 404) {
      return 'Ticket not found for comments.';
    }

    return 'Failed to load comments. Please try again.';
  }

  function mapCommentSubmitError(error: unknown): string {
    if (!isApiError(error)) {
      return 'Failed to add comment. Please try again.';
    }

    if (error.kind === 'network') {
      return 'Cannot reach backend. Verify API availability and VITE_API_BASE_URL.';
    }

    if (error.status === 403) {
      return 'You do not have permission to add comments on this ticket.';
    }

    if (error.status === 400) {
      return 'Invalid comment input or ticket is closed. Please review and try again.';
    }

    if (error.status === 404) {
      return 'Ticket not found for adding comments.';
    }

    return 'Failed to add comment. Please try again.';
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

      {!isLoading && !errorMessage && ticket ? (
        <section className="comments-section">
          <div className="comments-header">
            <h2>Comments</h2>
          </div>

          {isCommentsLoading ? (
            <p className="state-panel">Loading comments...</p>
          ) : null}
          {!isCommentsLoading && commentsErrorMessage ? (
            <p className="state-panel state-error" role="alert">
              {commentsErrorMessage}
            </p>
          ) : null}
          {!isCommentsLoading && !commentsErrorMessage && comments.length === 0 ? (
            <p className="state-panel muted-text">No comments yet.</p>
          ) : null}

          {!isCommentsLoading && !commentsErrorMessage && comments.length > 0 ? (
            <div className="comments-list">
              {comments.map((comment) => (
                <article key={comment.id} className="comment-card">
                  <div className="comment-meta">
                    <span>{formatAuthor(comment.authorId)}</span>
                    <span>
                      {comment.internal ? (
                        <span className="pill pill-danger">Internal</span>
                      ) : (
                        <span className="pill pill-success">Public</span>
                      )}
                    </span>
                    <span>{formatDate(comment.createdAt)}</span>
                  </div>
                  <p className="comment-body">{comment.body}</p>
                </article>
              ))}
            </div>
          ) : null}

          <form className="comment-form" onSubmit={handleCommentSubmit} noValidate>
            <label htmlFor="ticket-comment-body">Add comment</label>
            <textarea
              id="ticket-comment-body"
              name="body"
              maxLength={3000}
              value={commentBody}
              onChange={(event) => setCommentBody(event.target.value)}
              placeholder="Write a comment for this ticket"
              rows={5}
              required
            />

            {canCreateInternalComments ? (
              <label className="comment-checkbox">
                <input
                  type="checkbox"
                  checked={isInternalComment}
                  onChange={(event) => setIsInternalComment(event.target.checked)}
                />
                Internal comment
              </label>
            ) : null}

            {commentErrorMessage ? (
              <p className="state-panel state-error" role="alert">
                {commentErrorMessage}
              </p>
            ) : null}

            <div className="form-actions">
              <button type="submit" disabled={isSubmittingComment}>
                {isSubmittingComment ? 'Adding comment...' : 'Add comment'}
              </button>
            </div>
          </form>
        </section>
      ) : null}
    </section>
  );
}
