package com.resolvehub.organization.dto;

import com.resolvehub.user.domain.Role;
import java.util.UUID;

public record OrganizationUserResponse(
        UUID id,
        String name,
        String email,
        Role role
) {
}
