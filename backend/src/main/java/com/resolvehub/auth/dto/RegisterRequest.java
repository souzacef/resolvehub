package com.resolvehub.auth.dto;

import com.resolvehub.common.validation.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String organizationName,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank
        @Size(max = 72)
        @Pattern(regexp = PasswordPolicy.PASSWORD_REGEX, message = PasswordPolicy.MESSAGE)
        String password
) {
}
