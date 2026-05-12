import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import {
  createTicketComment,
  getTicket,
  listOrganizationUsers,
  listTicketAuditLogs,
  listTicketComments,
  requestTicketAiClassification,
  updateTicketAssignee,
  updateTicketStatus,
} from '../features/tickets/tickets';
import { isApiError } from '../lib/apiClient';
import type {
  AuditLogResponse,
  OrganizationUserResponse,
  TicketClassificationSuggestion,
  TicketCommentResponse,
  TicketResponse,
  TicketStatus,
} from '../types/api';

const staffStatusTransitions: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ['IN_PROGRESS', 'CLOSED'],
  IN_PROGRESS: ['WAITING_CUSTOMER', 'RESOLVED', 'CLOSED'],
  WAITING_CUSTOMER: ['IN_PROGRESS', 'RESOLVED'],
  RESOLVED: ['CLOSED', 'IN_PROGRESS'],
  CLOSED: [],
};

const allTicketStatuses: TicketStatus[] = [
  'OPEN',
  'IN_PROGRESS',
  'WAITING_CUSTOMER',
  'RESOLVED',
  'CLOSED',
];

type CustomerStatusAction = {
  nextStatus: TicketStatus;
  label: string;
};

export function TicketDetailPage() {
  const { role, userId } = useAuth();
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
  const [selectedStatus, setSelectedStatus] = useState<TicketStatus | ''>('');
  const [statusErrorMessage, setStatusErrorMessage] = useState<string | null>(null);
  const [statusSuccessMessage, setStatusSuccessMessage] = useState<string | null>(
    null,
  );
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);
  const [assignmentErrorMessage, setAssignmentErrorMessage] = useState<string | null>(
    null,
  );
  const [assignmentSuccessMessage, setAssignmentSuccessMessage] = useState<
    string | null
  >(null);
  const [isUpdatingAssignment, setIsUpdatingAssignment] = useState(false);
  const [selectedAssigneeId, setSelectedAssigneeId] = useState('');
  const [organizationUsers, setOrganizationUsers] = useState<
    OrganizationUserResponse[]
  >([]);
  const [organizationUsersErrorMessage, setOrganizationUsersErrorMessage] =
    useState<string | null>(null);
  const [isOrganizationUsersLoading, setIsOrganizationUsersLoading] =
    useState(false);
  const [aiSuggestion, setAiSuggestion] =
    useState<TicketClassificationSuggestion | null>(null);
  const [aiErrorMessage, setAiErrorMessage] = useState<string | null>(null);
  const [isAiLoading, setIsAiLoading] = useState(false);
  const [auditLogs, setAuditLogs] = useState<AuditLogResponse[]>([]);
  const [auditLogsErrorMessage, setAuditLogsErrorMessage] = useState<string | null>(
    null,
  );
  const [isAuditLogsLoading, setIsAuditLogsLoading] = useState(false);

  const canCreateInternalComments =
    role === 'AGENT' || role === 'MANAGER' || role === 'ADMIN';
  const canUseStaffStatusWorkflow =
    role === 'AGENT' || role === 'MANAGER' || role === 'ADMIN';
  const canManageAssignments = role === 'ADMIN' || role === 'MANAGER';
  const canUseAgentSelfAssignment = role === 'AGENT' && Boolean(userId);
  const isCustomer = role === 'CUSTOMER';

  const organizationUsersById = useMemo(() => {
    const usersById = new Map<string, OrganizationUserResponse>();
    for (const organizationUser of organizationUsers) {
      usersById.set(organizationUser.id, organizationUser);
    }
    return usersById;
  }, [organizationUsers]);

  const staffAssignableUsers = useMemo(
    () =>
      organizationUsers.filter(
        (organizationUser) => organizationUser.role !== 'CUSTOMER',
      ),
    [organizationUsers],
  );

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
      setSelectedStatus('');
      setStatusErrorMessage(null);
      setStatusSuccessMessage(null);
      setAssignmentErrorMessage(null);
      setAssignmentSuccessMessage(null);
      setSelectedAssigneeId('');
      setAiSuggestion(null);
      setAiErrorMessage(null);
      setIsAiLoading(false);
      setAuditLogs([]);
      setAuditLogsErrorMessage(null);
      setIsAuditLogsLoading(false);

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
    if (isCustomer) {
      setOrganizationUsers([]);
      setOrganizationUsersErrorMessage(null);
      setIsOrganizationUsersLoading(false);
      return;
    }

    let isMounted = true;

    async function loadOrganizationUsers() {
      setIsOrganizationUsersLoading(true);
      setOrganizationUsersErrorMessage(null);

      try {
        const data = await listOrganizationUsers();
        if (isMounted) {
          setOrganizationUsers(data);
        }
      } catch (error) {
        if (!isMounted) {
          return;
        }
        setOrganizationUsers([]);
        setOrganizationUsersErrorMessage(mapOrganizationUsersLoadError(error));
      } finally {
        if (isMounted) {
          setIsOrganizationUsersLoading(false);
        }
      }
    }

    void loadOrganizationUsers();

    return () => {
      isMounted = false;
    };
  }, [isCustomer]);

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

  useEffect(() => {
    if (!ticketId || !ticket || isCustomer) {
      return;
    }
    const requiredTicketId = ticketId;

    let isMounted = true;

    async function loadAuditLogs() {
      setIsAuditLogsLoading(true);
      setAuditLogsErrorMessage(null);

      try {
        const data = await listTicketAuditLogs(requiredTicketId);
        if (isMounted) {
          setAuditLogs(data);
        }
      } catch (error) {
        if (!isMounted) {
          return;
        }
        setAuditLogsErrorMessage(mapAuditLogLoadError(error));
      } finally {
        if (isMounted) {
          setIsAuditLogsLoading(false);
        }
      }
    }

    void loadAuditLogs();

    return () => {
      isMounted = false;
    };
  }, [ticketId, ticket, isCustomer]);

  useEffect(() => {
    if (!ticket || !canManageAssignments) {
      setSelectedAssigneeId('');
      return;
    }

    setSelectedAssigneeId(ticket.assigneeId ?? '');
  }, [ticket, canManageAssignments]);

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

  async function handleStatusSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!ticketId) {
      setStatusErrorMessage('Ticket id was not provided.');
      return;
    }
    const requiredTicketId = ticketId;

    if (!ticket) {
      setStatusErrorMessage('Ticket details are not loaded yet.');
      return;
    }

    if (!selectedStatus) {
      setStatusErrorMessage('Select a new status before updating.');
      return;
    }

    const allowedTransitions = getStaffStatusTransitions(ticket.status);
    if (!allowedTransitions.includes(selectedStatus)) {
      setStatusErrorMessage('That status transition is not allowed for your role.');
      return;
    }

    await performStatusUpdate(requiredTicketId, selectedStatus);
  }

  async function handleCustomerStatusAction(nextStatus: TicketStatus) {
    if (!ticketId) {
      setStatusErrorMessage('Ticket id was not provided.');
      return;
    }
    const requiredTicketId = ticketId;
    await performStatusUpdate(requiredTicketId, nextStatus);
  }

  async function performStatusUpdate(
    requiredTicketId: string,
    nextStatus: TicketStatus,
  ) {
    setIsUpdatingStatus(true);
    setStatusErrorMessage(null);
    setStatusSuccessMessage(null);

    try {
      await updateTicketStatus(requiredTicketId, { status: nextStatus });
      const refreshedTicket = await getTicket(requiredTicketId);
      setTicket(refreshedTicket);
      setSelectedStatus('');
      setStatusSuccessMessage(`Ticket status updated to ${refreshedTicket.status}.`);
    } catch (error) {
      setStatusErrorMessage(mapStatusSubmitError(error));
    } finally {
      setIsUpdatingStatus(false);
    }
  }

  async function handleAgentAssignToMe() {
    if (!ticketId) {
      setAssignmentErrorMessage('Ticket id was not provided.');
      return;
    }
    if (!userId) {
      setAssignmentErrorMessage('Your user id is not available in the current token.');
      return;
    }

    await performAssignmentUpdate(ticketId, userId, 'Ticket assigned to you.');
  }

  async function handleAssignSelectedStaffUser() {
    if (!ticketId) {
      setAssignmentErrorMessage('Ticket id was not provided.');
      return;
    }

    if (!selectedAssigneeId) {
      setAssignmentErrorMessage('Select a staff user before assigning.');
      return;
    }

    await performAssignmentUpdate(
      ticketId,
      selectedAssigneeId,
      `Ticket assigned to ${formatUserLabel(selectedAssigneeId)}.`,
    );
  }

  async function handleUnassignTicket() {
    if (!ticketId) {
      setAssignmentErrorMessage('Ticket id was not provided.');
      return;
    }

    await performAssignmentUpdate(ticketId, null, 'Ticket unassigned.');
  }

  async function performAssignmentUpdate(
    requiredTicketId: string,
    assigneeId: string | null,
    successMessage: string,
  ) {
    setIsUpdatingAssignment(true);
    setAssignmentErrorMessage(null);
    setAssignmentSuccessMessage(null);

    try {
      await updateTicketAssignee(requiredTicketId, {
        assigneeId,
      });
      const refreshedTicket = await getTicket(requiredTicketId);
      setTicket(refreshedTicket);
      setAssignmentSuccessMessage(successMessage);
    } catch (error) {
      setAssignmentErrorMessage(mapAssignmentSubmitError(error));
    } finally {
      setIsUpdatingAssignment(false);
    }
  }

  async function handleAiClassificationRequest() {
    if (!ticketId) {
      setAiErrorMessage('Ticket id was not provided.');
      return;
    }

    setIsAiLoading(true);
    setAiErrorMessage(null);

    try {
      const suggestion = await requestTicketAiClassification(ticketId);
      setAiSuggestion(suggestion);
    } catch (error) {
      setAiErrorMessage(mapAiClassificationError(error));
    } finally {
      setIsAiLoading(false);
    }
  }

  function formatDate(value: string) {
    return new Date(value).toLocaleString();
  }

  function shortenId(id: string) {
    return `${id.slice(0, 8)}...`;
  }

  function formatUserLabel(userId: string) {
    const user = organizationUsersById.get(userId);
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

  function formatAuthor(authorId: string) {
    return formatUserLabel(authorId);
  }

  function formatActor(actorId: string | null) {
    if (!actorId) {
      return 'System';
    }
    return formatUserLabel(actorId);
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

  function mapStatusSubmitError(error: unknown): string {
    if (!isApiError(error)) {
      return 'Failed to update ticket status. Please try again.';
    }

    if (error.kind === 'network') {
      return 'Cannot reach backend. Verify API availability and VITE_API_BASE_URL.';
    }

    if (error.status === 400) {
      return 'Invalid status transition. Please choose an allowed next status.';
    }

    if (error.status === 403) {
      return 'You do not have permission to update this ticket status.';
    }

    if (error.status === 404) {
      return 'Ticket not found for status update.';
    }

    return 'Failed to update ticket status. Please try again.';
  }

  function mapAssignmentSubmitError(error: unknown): string {
    if (!isApiError(error)) {
      return 'Failed to update assignment. Please try again.';
    }

    if (error.kind === 'network') {
      return 'Cannot reach backend. Verify API availability and VITE_API_BASE_URL.';
    }

    if (error.status === 403) {
      return 'You do not have permission to assign this ticket.';
    }

    if (error.status === 400) {
      return 'This ticket cannot be assigned with the selected assignee.';
    }

    if (error.status === 404) {
      return 'Ticket or assignee was not found.';
    }

    return 'Failed to update assignment. Please try again.';
  }

  function mapAiClassificationError(error: unknown): string {
    if (!isApiError(error)) {
      return 'Failed to request AI classification. Please try again.';
    }

    if (error.kind === 'network') {
      return 'Backend is unavailable.';
    }

    if (error.status === 403) {
      return 'You do not have permission to request AI classification.';
    }

    if (
      error.status === 502 ||
      (typeof error.responseBody === 'string' &&
        error.responseBody.includes('AI classification provider failed'))
    ) {
      return 'AI provider is unavailable. Try again later.';
    }

    return 'Failed to request AI classification. Please try again.';
  }

  function mapAuditLogLoadError(error: unknown): string {
    if (!isApiError(error)) {
      return 'Failed to load audit logs. Please try again.';
    }

    if (error.kind === 'network') {
      return 'Backend is unavailable.';
    }

    if (error.status === 403) {
      return 'You do not have permission to view audit logs.';
    }

    if (error.status === 404) {
      return 'Ticket audit logs were not found.';
    }

    return 'Failed to load audit logs. Please try again.';
  }

  function mapOrganizationUsersLoadError(error: unknown): string {
    if (!isApiError(error)) {
      return 'Failed to load organization users.';
    }

    if (error.kind === 'network') {
      return 'Cannot reach backend. Verify API availability and VITE_API_BASE_URL.';
    }

    if (error.status === 403) {
      return 'You do not have permission to view organization users.';
    }

    return 'Failed to load organization users.';
  }

  function getStaffStatusTransitions(currentStatus: TicketStatus): TicketStatus[] {
    return staffStatusTransitions[currentStatus];
  }

  function getCustomerStatusAction(
    currentStatus: TicketStatus,
  ): CustomerStatusAction | null {
    if (currentStatus === 'OPEN') {
      return { nextStatus: 'CLOSED', label: 'Close ticket' };
    }

    if (currentStatus === 'RESOLVED') {
      return { nextStatus: 'IN_PROGRESS', label: 'Reopen ticket' };
    }

    return null;
  }

  const staffStatusTransitionsForTicket =
    ticket && canUseStaffStatusWorkflow
      ? getStaffStatusTransitions(ticket.status)
      : [];
  const customerStatusAction =
    ticket && role === 'CUSTOMER' ? getCustomerStatusAction(ticket.status) : null;
  const showCustomerStatusSection =
    role === 'CUSTOMER' &&
    Boolean(customerStatusAction || statusErrorMessage || statusSuccessMessage);
  const canAgentAssignToSelf =
    canUseAgentSelfAssignment &&
    Boolean(ticket && !ticket.assigneeId && ticket.status !== 'CLOSED');
  const showAssignmentSection =
    !isCustomer &&
    Boolean(
      ticket &&
        (canManageAssignments ||
          canAgentAssignToSelf ||
          assignmentErrorMessage ||
          assignmentSuccessMessage),
    );
  const showAiClassificationSection = canUseStaffStatusWorkflow;
  const showAuditLogSection = canUseStaffStatusWorkflow;

  const isClosedTicket = ticket?.status === 'CLOSED';
  const canSubmitStaffAssignment =
    canManageAssignments &&
    !isClosedTicket &&
    selectedAssigneeId.length > 0 &&
    selectedAssigneeId !== (ticket?.assigneeId ?? '');

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
            {!isCustomer ? (
              <div className="detail-field">
                <dt>Assignee</dt>
                <dd>{formatAssignee(ticket.assigneeId)}</dd>
              </div>
            ) : null}
            <div className="detail-field">
              <dt>{isCustomer ? 'Expected response by' : 'SLA due at'}</dt>
              <dd>{formatDate(ticket.slaDueAt)}</dd>
            </div>
            <div className="detail-field">
              <dt>{isCustomer ? 'SLA status' : 'Overdue'}</dt>
              <dd>
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

      {!isLoading && !errorMessage && ticket && canUseStaffStatusWorkflow ? (
        <section className="comments-section">
          <div className="comments-header">
            <h2>Status Workflow</h2>
          </div>

          <p className="muted-text">
            Current status: <strong>{ticket.status}</strong>
          </p>

          {staffStatusTransitionsForTicket.length === 0 ? (
            <p className="state-panel muted-text">
              This ticket has no available status transitions.
            </p>
          ) : (
            <form className="comment-form" onSubmit={handleStatusSubmit} noValidate>
              <label htmlFor="ticket-status-select">Change status</label>
              <select
                id="ticket-status-select"
                name="status"
                value={selectedStatus}
                onChange={(event) => {
                  const value = event.target.value;
                  setSelectedStatus(
                    value && allTicketStatuses.includes(value as TicketStatus)
                      ? (value as TicketStatus)
                      : '',
                  );
                  setStatusErrorMessage(null);
                  setStatusSuccessMessage(null);
                }}
              >
                <option value="">Select next status</option>
                {staffStatusTransitionsForTicket.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>

              {statusErrorMessage ? (
                <p className="state-panel state-error" role="alert">
                  {statusErrorMessage}
                </p>
              ) : null}
              {statusSuccessMessage ? (
                <p className="state-panel" role="status">
                  {statusSuccessMessage}
                </p>
              ) : null}

              <div className="form-actions">
                <button
                  type="submit"
                  disabled={isUpdatingStatus || selectedStatus.length === 0}
                >
                  {isUpdatingStatus ? 'Updating status...' : 'Update status'}
                </button>
              </div>
            </form>
          )}
        </section>
      ) : null}

      {!isLoading && !errorMessage && ticket && showCustomerStatusSection ? (
        <section className="comments-section">
          <div className="comments-header">
            <h2>Ticket Status</h2>
          </div>

          <p className="muted-text">
            Current status: <strong>{ticket.status}</strong>
          </p>

          {customerStatusAction ? (
            <div className="comment-form">
              <div className="form-actions">
                <button
                  type="button"
                  onClick={() =>
                    void handleCustomerStatusAction(customerStatusAction.nextStatus)
                  }
                  disabled={isUpdatingStatus}
                >
                  {isUpdatingStatus
                    ? 'Updating status...'
                    : customerStatusAction.label}
                </button>
              </div>
            </div>
          ) : null}

          {statusErrorMessage ? (
            <p className="state-panel state-error" role="alert">
              {statusErrorMessage}
            </p>
          ) : null}
          {statusSuccessMessage ? (
            <p className="state-panel" role="status">
              {statusSuccessMessage}
            </p>
          ) : null}
        </section>
      ) : null}

      {!isLoading && !errorMessage && ticket && showAssignmentSection ? (
        <section className="comments-section">
          <div className="comments-header">
            <h2>Assignment</h2>
          </div>

          <p className="muted-text">
            Current assignee: <strong>{formatAssignee(ticket.assigneeId)}</strong>
          </p>

          {canManageAssignments ? (
            <div className="comment-form">
              {isOrganizationUsersLoading ? (
                <p className="state-panel">Loading organization users...</p>
              ) : null}

              {!isOrganizationUsersLoading && organizationUsersErrorMessage ? (
                <p className="state-panel state-error" role="alert">
                  {organizationUsersErrorMessage}
                </p>
              ) : null}

              {!isOrganizationUsersLoading &&
              !organizationUsersErrorMessage &&
              staffAssignableUsers.length === 0 ? (
                <p className="muted-text">
                  No staff users are available for assignment in this organization.
                </p>
              ) : null}

              {!isOrganizationUsersLoading &&
              !organizationUsersErrorMessage &&
              staffAssignableUsers.length > 0 ? (
                <>
                  {isClosedTicket ? (
                    <p className="muted-text">Closed tickets cannot be assigned.</p>
                  ) : null}

                  <label htmlFor="ticket-assignee-select">Assign to staff user</label>
                  <select
                    id="ticket-assignee-select"
                    name="assigneeId"
                    value={selectedAssigneeId}
                    onChange={(event) => {
                      setSelectedAssigneeId(event.target.value);
                      setAssignmentErrorMessage(null);
                      setAssignmentSuccessMessage(null);
                    }}
                    disabled={Boolean(isClosedTicket)}
                  >
                    <option value="">Select staff user</option>
                    {staffAssignableUsers.map((organizationUser) => (
                      <option key={organizationUser.id} value={organizationUser.id}>
                        {organizationUser.name && organizationUser.name.trim().length > 0
                          ? `${organizationUser.name.trim()} (${organizationUser.email})`
                          : organizationUser.email}{' '}
                        - {organizationUser.role}
                      </option>
                    ))}
                  </select>

                  <div className="form-actions">
                    <button
                      type="button"
                      onClick={() => void handleAssignSelectedStaffUser()}
                      disabled={isUpdatingAssignment || !canSubmitStaffAssignment}
                    >
                      {isUpdatingAssignment
                        ? 'Updating assignment...'
                        : 'Assign ticket'}
                    </button>
                  </div>
                </>
              ) : null}

              {ticket.assigneeId ? (
                <div className="form-actions">
                  <button
                    type="button"
                    onClick={() => void handleUnassignTicket()}
                    disabled={isUpdatingAssignment || Boolean(isClosedTicket)}
                  >
                    {isUpdatingAssignment ? 'Updating assignment...' : 'Unassign ticket'}
                  </button>
                </div>
              ) : (
                <p className="muted-text">This ticket is currently unassigned.</p>
              )}
            </div>
          ) : null}

          {!canManageAssignments && role === 'AGENT' && canAgentAssignToSelf ? (
            <div className="comment-form">
              <div className="form-actions">
                <button
                  type="button"
                  onClick={() => void handleAgentAssignToMe()}
                  disabled={isUpdatingAssignment}
                >
                  {isUpdatingAssignment ? 'Updating assignment...' : 'Assign to me'}
                </button>
              </div>
            </div>
          ) : null}

          {assignmentErrorMessage ? (
            <p className="state-panel state-error" role="alert">
              {assignmentErrorMessage}
            </p>
          ) : null}
          {assignmentSuccessMessage ? (
            <p className="state-panel" role="status">
              {assignmentSuccessMessage}
            </p>
          ) : null}
        </section>
      ) : null}

      {!isLoading && !errorMessage && ticket && showAiClassificationSection ? (
        <section className="comments-section">
          <div className="comments-header">
            <h2>AI Classification</h2>
          </div>

          <div className="comment-form">
            <p className="muted-text">
              AI suggestions are advisory and do not automatically change ticket
              category or priority.
            </p>
            <div className="form-actions">
              <button
                type="button"
                onClick={() => void handleAiClassificationRequest()}
                disabled={isAiLoading}
              >
                {isAiLoading
                  ? 'Requesting AI suggestion...'
                  : 'Suggest classification with AI'}
              </button>
            </div>
          </div>

          {aiErrorMessage ? (
            <p className="state-panel state-error" role="alert">
              {aiErrorMessage}
            </p>
          ) : null}

          {aiSuggestion ? (
            <article className="comment-card">
              <div className="comment-meta">
                <span>
                  Suggested category: <strong>{aiSuggestion.suggestedCategory}</strong>
                </span>
                <span>
                  Suggested priority: <strong>{aiSuggestion.suggestedPriority}</strong>
                </span>
              </div>
              <p className="comment-body">{aiSuggestion.reasoning}</p>
            </article>
          ) : null}
        </section>
      ) : null}

      {!isLoading && !errorMessage && ticket && showAuditLogSection ? (
        <section className="comments-section">
          <div className="comments-header">
            <h2>Audit Log</h2>
          </div>

          {isAuditLogsLoading ? (
            <p className="state-panel">Loading audit logs...</p>
          ) : null}
          {!isAuditLogsLoading && auditLogsErrorMessage ? (
            <p className="state-panel state-error" role="alert">
              {auditLogsErrorMessage}
            </p>
          ) : null}
          {!isAuditLogsLoading &&
          !auditLogsErrorMessage &&
          auditLogs.length === 0 ? (
            <p className="state-panel muted-text">
              No audit log entries are available for this ticket.
            </p>
          ) : null}

          {!isAuditLogsLoading && !auditLogsErrorMessage && auditLogs.length > 0 ? (
            <div className="comments-list">
              {auditLogs.map((auditLog) => (
                <article key={auditLog.id} className="comment-card">
                  <div className="comment-meta">
                    <span>
                      Action: <strong>{auditLog.action}</strong>
                    </span>
                    <span>Actor: {formatActor(auditLog.actorId)}</span>
                    <span>{formatDate(auditLog.createdAt)}</span>
                  </div>
                  <p className="comment-body">{auditLog.details}</p>
                </article>
              ))}
            </div>
          ) : null}
        </section>
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
