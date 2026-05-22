import type { CreateTicketRequest } from '../../types/api';

export type CreateTicketFormValues = CreateTicketRequest;

type CreateTicketValidationOptions = {
  requireRequester?: boolean;
};

export function validateCreateTicket(
  values: CreateTicketFormValues,
  options: CreateTicketValidationOptions = {},
): string[] {
  const errors: string[] = [];

  if (!values.title.trim()) {
    errors.push('Title is required.');
  } else if (values.title.trim().length > 200) {
    errors.push('Title must be 200 characters or less.');
  }

  if (!values.description.trim()) {
    errors.push('Description is required.');
  } else if (values.description.trim().length > 5000) {
    errors.push('Description must be 5000 characters or less.');
  }

  if (!values.priority) {
    errors.push('Priority is required.');
  }

  if (!values.category) {
    errors.push('Category is required.');
  }

  if (options.requireRequester && !values.requesterId) {
    errors.push('Requester is required.');
  }

  return errors;
}
