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
import java.lang.reflect.Field;
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
    void ticketCreationStillWorksWhenAiClassifierIsUnavailable() throws Exception {
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
        UUID ticketId = UUID.randomUUID();
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
        setEntityId(organization, organizationId);

        User requester = new User();
        requester.setOrganization(organization);
        requester.setName("Customer");
        requester.setEmail("customer@example.com");
        requester.setPasswordHash("hash");
        requester.setRole(Role.CUSTOMER);
        requester.setStatus("ACTIVE");
        setEntityId(requester, userId);

        CreateTicketRequest request = new CreateTicketRequest(
                "Cannot login",
                "User cannot login after reset",
                TicketPriority.HIGH,
                TicketCategory.TECHNICAL,
                null
        );

        OffsetDateTime dueAt = OffsetDateTime.now().plusHours(8);
        OffsetDateTime createdAt = OffsetDateTime.now();
        Ticket persistedTicket = new Ticket();
        persistedTicket.setOrganization(organization);
        persistedTicket.setRequester(requester);
        persistedTicket.setTitle("Cannot login");
        persistedTicket.setDescription("User cannot login after reset");
        persistedTicket.setStatus(TicketStatus.OPEN);
        persistedTicket.setPriority(TicketPriority.HIGH);
        persistedTicket.setCategory(TicketCategory.TECHNICAL);
        persistedTicket.setCreatedAt(createdAt);
        persistedTicket.setSlaDueAt(dueAt);
        setEntityId(persistedTicket, ticketId);
        setTicketNumber(persistedTicket, "RH-1001");

        TicketResponse expectedResponse = new TicketResponse(
                ticketId,
                "RH-1001",
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
                createdAt,
                createdAt
        );

        when(userRepository.findByIdAndOrganizationId(userId, organizationId)).thenReturn(Optional.of(requester));
        when(slaDeadlineCalculator.calculateDueAt(any(), any())).thenReturn(dueAt);
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenReturn(persistedTicket);
        when(ticketRepository.findByIdAndOrganizationId(ticketId, organizationId)).thenReturn(Optional.of(persistedTicket));
        when(ticketMapper.toResponse(persistedTicket)).thenReturn(expectedResponse);

        TicketResponse actual = ticketService.createTicket(principal, request);

        assertEquals(expectedResponse, actual);
        verify(ticketAiClassifier, never()).classify(any());
    }

    private void setEntityId(Object entity, UUID id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    private void setTicketNumber(Ticket ticket, String ticketNumber) throws Exception {
        Field field = Ticket.class.getDeclaredField("ticketNumber");
        field.setAccessible(true);
        field.set(ticket, ticketNumber);
    }
}
