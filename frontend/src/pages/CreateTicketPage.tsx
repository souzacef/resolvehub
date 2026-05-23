import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import { createTicket, listOrganizationUsers } from '../features/tickets/tickets';
import { isApiError } from '../lib/apiClient';
import { validateCreateTicket } from '../features/tickets/validation';
import type {
  OrganizationUserResponse,
  TicketCategory,
  TicketPriority,
} from '../types/api';

const priorityOptions: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
const categoryOptions: TicketCategory[] = [
  'BILLING',
  'TECHNICAL',
  'ACCOUNT',
  'FEATURE_REQUEST',
  'SECURITY',
  'OTHER',
];

export function CreateTicketPage() {
  const { canCreateTickets, role } = useAuth();
  const navigate = useNavigate();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
  const [category, setCategory] = useState<TicketCategory>('TECHNICAL');
  const [requesterId, setRequesterId] = useState('');
  const [requesterSearchQuery, setRequesterSearchQuery] = useState('');
  const [organizationUsers, setOrganizationUsers] = useState<
    OrganizationUserResponse[]
  >([]);
  const [organizationUsersErrorMessage, setOrganizationUsersErrorMessage] =
    useState<string | null>(null);
  const [isOrganizationUsersLoading, setIsOrganizationUsersLoading] =
    useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  const isStaff = role === 'AGENT' || role === 'MANAGER' || role === 'ADMIN';

  const customerUsers = useMemo(
    () => organizationUsers.filter((organizationUser) => organizationUser.role === 'CUSTOMER'),
    [organizationUsers],
  );

  const requesterSearchTerm = requesterSearchQuery.trim();
  const canShowRequesterResults = requesterSearchTerm.length >= 2;

  const filteredCustomerUsers = useMemo(() => {
    const normalizedQuery = requesterSearchTerm.toLowerCase();

    if (!normalizedQuery) {
      return [];
    }

    return customerUsers.filter((organizationUser) => {
      const name = organizationUser.name?.toLowerCase() ?? '';
      const email = organizationUser.email.toLowerCase();
      return name.includes(normalizedQuery) || email.includes(normalizedQuery);
    });
  }, [customerUsers, requesterSearchTerm]);

  const selectedCustomer = useMemo(
    () => customerUsers.find((organizationUser) => organizationUser.id === requesterId) ?? null,
    [customerUsers, requesterId],
  );

  const validationErrors = useMemo(
    () =>
      validateCreateTicket(
        {
          title,
          description,
          priority,
          category,
          requesterId,
        },
        { requireRequester: isStaff },
      ),
    [title, description, priority, category, requesterId, isStaff],
  );

  useEffect(() => {
    if (!isStaff) {
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
  }, [isStaff]);

  useEffect(() => {
    if (requesterId && !customerUsers.some((organizationUser) => organizationUser.id === requesterId)) {
      setRequesterId('');
    }
  }, [customerUsers, requesterId]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (validationErrors.length > 0) {
      setSubmitted(true);
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      await createTicket({
        title: title.trim(),
        description: description.trim(),
        priority,
        category,
        requesterId: isStaff ? requesterId : undefined,
      });
      navigate('/tickets', { replace: true });
    } catch (error) {
      setSubmitError(mapCreateTicketSubmitError(error, isStaff));
    } finally {
      setIsSubmitting(false);
    }
  }

  function formatCustomerLabel(organizationUser: OrganizationUserResponse) {
    if (organizationUser.name && organizationUser.name.trim().length > 0) {
      return `${organizationUser.name.trim()} — ${organizationUser.email}`;
    }

    return organizationUser.email;
  }

  function handleSelectRequester(nextRequesterId: string) {
    setRequesterId(nextRequesterId);
    setRequesterSearchQuery('');
  }

  function clearSelectedRequester() {
    setRequesterId('');
  }

  function mapOrganizationUsersLoadError(error: unknown): string {
    if (!isApiError(error)) {
      return 'Failed to load customer users.';
    }

    if (error.kind === 'network') {
      return 'Service is unavailable. Please try again later.';
    }

    if (error.status === 403) {
      return 'You do not have permission to load organization users.';
    }

    return 'Failed to load customer users.';
  }

  function mapCreateTicketSubmitError(error: unknown, creatingAsStaff: boolean): string {
    if (!isApiError(error)) {
      return 'Failed to create ticket. Please try again.';
    }

    if (error.kind === 'network') {
      return 'Service is unavailable. Please try again later.';
    }

    if (error.status === 403) {
      return creatingAsStaff
        ? 'You do not have permission to create tickets for this requester.'
        : 'Only customers can create support tickets.';
    }

    if (error.status === 400) {
      return creatingAsStaff
        ? 'Ticket input is invalid. Please review requester, title, description, priority, and category.'
        : 'Ticket input is invalid. Please review title, description, priority, and category.';
    }

    if (error.status === 404) {
      return creatingAsStaff
        ? 'The selected customer was not found in your organization.'
        : 'Ticket requester was not found.';
    }

    return 'Failed to create ticket. Please try again.';
  }

  const showNoCustomersMessage =
    isStaff &&
    !isOrganizationUsersLoading &&
    !organizationUsersErrorMessage &&
    customerUsers.length === 0;

  const disableSubmit =
    isSubmitting ||
    (isStaff &&
      (isOrganizationUsersLoading ||
        Boolean(organizationUsersErrorMessage) ||
        customerUsers.length === 0));

  if (!canCreateTickets) {
    return (
      <section>
        <header className="page-header">
          <h1>Create Ticket</h1>
          <Link className="link-button link-button-secondary" to="/tickets">
            Back to tickets
          </Link>
        </header>

        <p className="state-panel state-error" role="alert">
          You do not have permission to create tickets.
        </p>
      </section>
    );
  }

  return (
    <section>
      <header className="page-header">
        <h1>Create Ticket</h1>
        <Link className="link-button link-button-secondary" to="/tickets">
          Back to tickets
        </Link>
      </header>

      <form className="form-card" onSubmit={handleSubmit} noValidate>
        {isStaff ? (
          <>
            <p className="muted-text">Customer requester</p>
            {isOrganizationUsersLoading ? (
              <p className="state-panel">Loading customer users...</p>
            ) : null}
            {!isOrganizationUsersLoading && organizationUsersErrorMessage ? (
              <p className="state-panel state-error" role="alert">
                {organizationUsersErrorMessage}
              </p>
            ) : null}
            {!isOrganizationUsersLoading && !organizationUsersErrorMessage ? (
              <div className="requester-selector">
                {selectedCustomer ? (
                  <div className="requester-selected state-panel">
                    <p className="muted-text">Selected customer</p>
                    <p>{formatCustomerLabel(selectedCustomer)}</p>
                    <button
                      type="button"
                      className="link-button link-button-secondary requester-clear-button"
                      onClick={clearSelectedRequester}
                    >
                      Clear selection
                    </button>
                  </div>
                ) : null}

                <label htmlFor="ticket-requester-search">Search customers</label>
                <input
                  id="ticket-requester-search"
                  name="ticket-requester-search"
                  type="search"
                  value={requesterSearchQuery}
                  onChange={(event) => setRequesterSearchQuery(event.target.value)}
                  placeholder="Search by customer name or email"
                  disabled={customerUsers.length === 0}
                />

                {customerUsers.length > 0 ? (
                  requesterSearchTerm.length === 0 ? (
                    <p className="state-panel muted-text">
                      Search by customer name or email to select a requester.
                    </p>
                  ) : !canShowRequesterResults ? (
                    <p className="state-panel muted-text">
                      Type at least 2 characters to search customers.
                    </p>
                  ) : filteredCustomerUsers.length > 0 ? (
                    <ul className="requester-results" aria-label="Customer search results">
                      {filteredCustomerUsers.map((organizationUser) => (
                        <li key={organizationUser.id}>
                          <button
                            type="button"
                            className={
                              requesterId === organizationUser.id
                                ? 'requester-option is-selected'
                                : 'requester-option'
                            }
                            onClick={() => handleSelectRequester(organizationUser.id)}
                          >
                            {formatCustomerLabel(organizationUser)}
                          </button>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <div className="state-panel muted-text">
                      <p>No customers found.</p>
                      <Link
                        className="link-button link-button-secondary requester-create-customer"
                        to="/organization/users"
                      >
                        Create customer
                      </Link>
                    </div>
                  )
                ) : null}
              </div>
            ) : null}
            {showNoCustomersMessage ? (
              <p className="state-panel muted-text">
                Create a customer user before creating a ticket on their behalf.
              </p>
            ) : null}
          </>
        ) : null}

        <label htmlFor="ticket-title">Title</label>
        <input
          id="ticket-title"
          name="title"
          maxLength={200}
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Short summary of the issue"
          required
        />

        <label htmlFor="ticket-description">Description</label>
        <textarea
          id="ticket-description"
          name="description"
          maxLength={5000}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          placeholder="Describe the issue, impact, and any relevant details"
          required
          rows={7}
        />

        <div className="form-grid">
          <div>
            <label htmlFor="ticket-priority">Priority</label>
            <select
              id="ticket-priority"
              name="priority"
              value={priority}
              onChange={(event) => setPriority(event.target.value as TicketPriority)}
            >
              {priorityOptions.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="ticket-category">Category</label>
            <select
              id="ticket-category"
              name="category"
              value={category}
              onChange={(event) => setCategory(event.target.value as TicketCategory)}
            >
              {categoryOptions.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </div>
        </div>

        {submitted && validationErrors.length > 0 ? (
          <div className="state-panel state-error" role="alert">
            <strong>Please fix the following:</strong>
            <ul>
              {validationErrors.map((message) => (
                <li key={message}>{message}</li>
              ))}
            </ul>
          </div>
        ) : null}

        {submitError ? (
          <p className="state-panel state-error" role="alert">
            {submitError}
          </p>
        ) : null}

        <div className="form-actions">
          <button type="submit" disabled={disableSubmit}>
            {isSubmitting ? 'Creating...' : 'Create ticket'}
          </button>
        </div>
      </form>
    </section>
  );
}
