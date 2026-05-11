import { apiRequest } from '../../lib/apiClient';
import type {
  CreateTicketCommentRequest,
  CreateTicketRequest,
  TicketCommentResponse,
  TicketResponse,
} from '../../types/api';

export async function createTicket(
  payload: CreateTicketRequest,
): Promise<TicketResponse> {
  return apiRequest<TicketResponse>('/api/tickets', {
    method: 'POST',
    body: payload,
  });
}

export async function getTicket(ticketId: string): Promise<TicketResponse> {
  return apiRequest<TicketResponse>(`/api/tickets/${ticketId}`);
}

export async function listTicketComments(
  ticketId: string,
): Promise<TicketCommentResponse[]> {
  return apiRequest<TicketCommentResponse[]>(`/api/tickets/${ticketId}/comments`);
}

export async function createTicketComment(
  ticketId: string,
  payload: CreateTicketCommentRequest,
): Promise<TicketCommentResponse> {
  return apiRequest<TicketCommentResponse>(`/api/tickets/${ticketId}/comments`, {
    method: 'POST',
    body: payload,
  });
}
