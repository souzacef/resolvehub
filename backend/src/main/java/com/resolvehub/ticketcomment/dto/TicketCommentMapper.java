package com.resolvehub.ticketcomment.dto;

import com.resolvehub.ticketcomment.domain.TicketComment;
import org.springframework.stereotype.Component;

@Component
public class TicketCommentMapper {

    public TicketCommentResponse toResponse(TicketComment comment) {
        return new TicketCommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                comment.getAuthor().getId(),
                comment.getBody(),
                comment.isInternal(),
                comment.getCreatedAt()
        );
    }
}
