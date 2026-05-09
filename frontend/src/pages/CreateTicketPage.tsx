import { FormEvent, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { createTicket } from '../features/tickets/tickets';
import { validateCreateTicket } from '../features/tickets/validation';
import type { TicketCategory, TicketPriority } from '../types/api';

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
  const navigate = useNavigate();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
  const [category, setCategory] = useState<TicketCategory>('TECHNICAL');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  const validationErrors = useMemo(
    () =>
      validateCreateTicket({
        title,
        description,
        priority,
        category,
      }),
    [title, description, priority, category],
  );

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
      });
      navigate('/tickets', { replace: true });
    } catch {
      setSubmitError(
        'Failed to create ticket. Please verify input and backend availability.',
      );
    } finally {
      setIsSubmitting(false);
    }
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
          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Creating...' : 'Create ticket'}
          </button>
        </div>
      </form>
    </section>
  );
}
