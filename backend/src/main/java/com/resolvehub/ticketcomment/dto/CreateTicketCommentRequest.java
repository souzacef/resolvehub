package com.resolvehub.ticketcomment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketCommentRequest(
        @NotBlank @Size(max = 3000) String body,
        Boolean internal
) {
}
