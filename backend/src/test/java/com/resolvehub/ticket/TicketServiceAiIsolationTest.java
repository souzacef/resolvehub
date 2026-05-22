package com.resolvehub.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.resolvehub.ai.service.TicketAiClassifier;
import com.resolvehub.audit.service.AuditLogService;
import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.organization.domain.Organization;
import com.resolvehub.ticket.domain.Ticket;
import com.resolvehub.ticket.domain.TicketCategory;
import com.resolvehub.ticket.domain.TicketPriority;
import com.resolvehub.ticket.domain.TicketStatus;
import com.resolvehub.ticket.dto.CreateTicketRequest;
import com.resolvehub.ticket.dto.TicketMapper;
import com.resolvehub.ticket.dto.TicketResponse;
import com.resolvehub.ticket.repository.TicketRepository;
import com.resolvehub.ticket.service.SlaDeadlineCalculator;
import com.resolvehub.ticket.service.TicketService;
import com.resolvehub.user.domain.Role;
import com.resolvehub.user.domain.User;
import com.resolvehub.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketServiceAiIsolationTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private SlaDeadlineCalculator slaDeadlineCalculator;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private TicketAiClassifier ticketAiClassifier;

    @Test
    void ticketCreationStillWorksWhenAiClassifierIsUnavailable() {
        TicketService ticketService = new TicketService(
                ticketRepository,
                userRepository,
                ticketMapper,
                slaDeadlineCalculator,
                auditLogService,
                ticketAiClassifier
        );

        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ResolveHubUserPrincipal principal = new ResolveHubUserPrincipal(
                userId,
                organizationId,
                "customer@example.com",
                "hash",
                Role.CUSTOMER
        );

        Organization organization = new Organization();
        organization.setName("Acme");
        organization.setStatus("ACTIVE");

        User requester = new User();
        requester.setOrganization(organization);
        requester.setName("Customer");
        requester.setEmail("customer@example.com");
        requester.setPasswordHash("hash");
        requester.setRole(Role.CUSTOMER);
        requester.setStatus("ACTIVE");

        CreateTicketRequest request = new CreateTicketRequest(
                "Cannot login",
                "User cannot login after reset",
                TicketPriority.HIGH,
                TicketCategory.TECHNICAL,
                null
        );

        OffsetDateTime dueAt = OffsetDateTime.now().plusHours(8);
        TicketResponse expectedResponse = new TicketResponse(
                UUID.randomUUID(),
                organizationId,
                userId,
                null,
                "Cannot login",
                "User cannot login after reset",
                TicketStatus.OPEN,
                TicketPriority.HIGH,
                TicketCategory.TECHNICAL,
                dueAt,
                false,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(userRepository.findByIdAndOrganizationId(userId, organizationId)).thenReturn(Optional.of(requester));
        when(slaDeadlineCalculator.calculateDueAt(any(), any())).thenReturn(dueAt);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(expectedResponse);
        TicketResponse actual = ticketService.createTicket(principal, request);

        assertEquals(expectedResponse, actual);
        verify(ticketAiClassifier, never()).classify(any());
    }
}
