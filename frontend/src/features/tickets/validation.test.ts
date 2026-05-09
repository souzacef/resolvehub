import { describe, expect, it } from 'vitest';
import { validateCreateTicket } from './validation';

describe('validateCreateTicket', () => {
  it('returns errors for missing required fields', () => {
    const errors = validateCreateTicket({
      title: ' ',
      description: ' ',
      priority: 'MEDIUM',
      category: 'TECHNICAL',
    });

    expect(errors).toContain('Title is required.');
    expect(errors).toContain('Description is required.');
  });

  it('returns no errors for valid form values', () => {
    const errors = validateCreateTicket({
      title: 'Cannot login after password reset',
      description: 'The user sees a 500 error while trying to sign in.',
      priority: 'HIGH',
      category: 'TECHNICAL',
    });

    expect(errors).toEqual([]);
  });
});
