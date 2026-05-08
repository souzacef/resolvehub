package com.resolvehub.ticket.dto;

import java.util.UUID;

public record UpdateTicketAssigneeRequest(
        UUID assigneeId
) {
}
