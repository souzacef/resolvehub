import { apiRequest } from '../../lib/apiClient';
import type { CreateTicketRequest, TicketResponse } from '../../types/api';

export async function createTicket(
  payload: CreateTicketRequest,
): Promise<TicketResponse> {
  return apiRequest<TicketResponse>('/api/tickets', {
    method: 'POST',
    body: payload,
  });
}
