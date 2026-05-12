import { apiRequest } from '../../lib/apiClient';
import type {
  AuditLogResponse,
  CreateTicketCommentRequest,
  CreateTicketRequest,
  OrganizationUserResponse,
  TicketClassificationSuggestion,
  TicketCommentResponse,
  TicketResponse,
  UpdateTicketAssigneeRequest,
  UpdateTicketStatusRequest,
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

export async function updateTicketStatus(
  ticketId: string,
  payload: UpdateTicketStatusRequest,
): Promise<TicketResponse> {
  return apiRequest<TicketResponse>(`/api/tickets/${ticketId}/status`, {
    method: 'PATCH',
    body: payload,
  });
}

export async function updateTicketAssignee(
  ticketId: string,
  payload: UpdateTicketAssigneeRequest,
): Promise<TicketResponse> {
  return apiRequest<TicketResponse>(`/api/tickets/${ticketId}/assignee`, {
    method: 'PATCH',
    body: payload,
  });
}

export async function requestTicketAiClassification(
  ticketId: string,
): Promise<TicketClassificationSuggestion> {
  return apiRequest<TicketClassificationSuggestion>(
    `/api/tickets/${ticketId}/ai/classification`,
    {
      method: 'POST',
    },
  );
}

export async function listTicketAuditLogs(
  ticketId: string,
): Promise<AuditLogResponse[]> {
  return apiRequest<AuditLogResponse[]>(`/api/tickets/${ticketId}/audit-logs`);
}

export async function listOrganizationUsers(): Promise<OrganizationUserResponse[]> {
  return apiRequest<OrganizationUserResponse[]>('/api/organization/users');
}
