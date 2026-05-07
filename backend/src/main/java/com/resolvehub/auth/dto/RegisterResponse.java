package com.resolvehub.auth.dto;

import com.resolvehub.user.domain.Role;
import java.util.UUID;

public record RegisterResponse(
        UUID organizationId,
        UUID userId,
        String email,
        Role role
) {
}
