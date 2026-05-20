export type LoginRequest = {
  email: string;
  password: string;
};

export type RegisterRequest = {
  organizationName: string;
  name: string;
  email: string;
  password: string;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
};

export type RegisterResponse = {
  organizationId: string;
  userId: string;
  email: string;
  role: UserRole;
};

export type UserRole = 'CUSTOMER' | 'AGENT' | 'MANAGER' | 'ADMIN';

export type TicketStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'WAITING_CUSTOMER'
  | 'RESOLVED'
  | 'CLOSED';

export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export type TicketCategory =
  | 'BILLING'
  | 'TECHNICAL'
  | 'ACCOUNT'
  | 'FEATURE_REQUEST'
  | 'SECURITY'
  | 'OTHER';

export type TicketResponse = {
  id: string;
  organizationId: string;
  requesterId: string;
  assigneeId: string | null;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  category: TicketCategory;
  slaDueAt: string;
  overdue: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CreateTicketRequest = {
  title: string;
  description: string;
  priority: TicketPriority;
  category: TicketCategory;
};

export type TicketCommentResponse = {
  id: string;
  ticketId: string;
  authorId: string;
  body: string;
  internal: boolean;
  createdAt: string;
};

export type CreateTicketCommentRequest = {
  body: string;
  internal?: boolean;
};

export type UpdateTicketStatusRequest = {
  status: TicketStatus;
};

export type UpdateTicketAssigneeRequest = {
  assigneeId: string | null;
};

export type UpdateTicketClassificationRequest = {
  category: TicketCategory;
  priority: TicketPriority;
};

export type TicketClassificationSuggestion = {
  suggestedCategory: TicketCategory;
  suggestedPriority: TicketPriority;
  reasoning: string;
};

export type AuditLogResponse = {
  id: string;
  organizationId: string;
  actorId: string | null;
  ticketId: string | null;
  action: string;
  details: string;
  createdAt: string;
};

export type OrganizationUserResponse = {
  id: string;
  name: string | null;
  email: string;
  role: UserRole;
};

export type CreateOrganizationUserRequest = {
  name: string;
  email: string;
  password: string;
  role: UserRole;
};
