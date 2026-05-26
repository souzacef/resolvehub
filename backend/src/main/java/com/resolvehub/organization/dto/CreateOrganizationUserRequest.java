package com.resolvehub.organization.dto;

import com.resolvehub.common.validation.PasswordPolicy;
import com.resolvehub.user.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank
        @Size(max = 72)
        @Pattern(regexp = PasswordPolicy.PASSWORD_REGEX, message = PasswordPolicy.MESSAGE)
        String password,
        @NotNull Role role
) {
}
