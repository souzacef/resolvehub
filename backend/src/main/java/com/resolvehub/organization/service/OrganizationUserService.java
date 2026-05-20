package com.resolvehub.organization.service;

import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.organization.domain.Organization;
import com.resolvehub.organization.dto.CreateOrganizationUserRequest;
import com.resolvehub.organization.dto.OrganizationUserResponse;
import com.resolvehub.organization.repository.OrganizationRepository;
import com.resolvehub.user.domain.Role;
import com.resolvehub.user.domain.User;
import com.resolvehub.user.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrganizationUserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public OrganizationUserService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<OrganizationUserResponse> listOrganizationUsers(ResolveHubUserPrincipal principal) {
        requirePrincipal(principal);
        ensureRoleCanListUsers(principal.getRole());

        return userRepository.findByOrganizationIdOrderByEmailAsc(principal.getOrganizationId())
                .stream()
                .map(user -> new OrganizationUserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole()
                ))
                .toList();
    }

    @Transactional
    public OrganizationUserResponse createOrganizationUser(
            ResolveHubUserPrincipal principal,
            CreateOrganizationUserRequest request
    ) {
        requirePrincipal(principal);
        ensureRoleCanCreateUser(principal.getRole(), request.role());

        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        Organization organization = organizationRepository.findById(principal.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization not found"));

        User user = new User();
        user.setOrganization(organization);
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus("ACTIVE");

        User createdUser = userRepository.save(user);
        return new OrganizationUserResponse(
                createdUser.getId(),
                createdUser.getName(),
                createdUser.getEmail(),
                createdUser.getRole()
        );
    }

    private void ensureRoleCanListUsers(Role role) {
        if (role == Role.ADMIN || role == Role.MANAGER || role == Role.AGENT) {
            return;
        }

        if (role == Role.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers cannot list organization users");
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed to list organization users");
    }

    private void ensureRoleCanCreateUser(Role creatorRole, Role targetRole) {
        if (creatorRole == Role.ADMIN) {
            if (targetRole == Role.CUSTOMER || targetRole == Role.AGENT || targetRole == Role.MANAGER || targetRole == Role.ADMIN) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Target role is not allowed");
        }

        if (creatorRole == Role.MANAGER) {
            if (targetRole == Role.CUSTOMER || targetRole == Role.AGENT) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Managers can only create customer or agent users");
        }

        if (creatorRole == Role.AGENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agents cannot create organization users");
        }

        if (creatorRole == Role.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers cannot create organization users");
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed to create organization users");
    }

    private void requirePrincipal(ResolveHubUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
