package com.resolvehub.organization.service;

import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.organization.dto.OrganizationUserResponse;
import com.resolvehub.user.domain.Role;
import com.resolvehub.user.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrganizationUserService {

    private final UserRepository userRepository;

    public OrganizationUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

    private void ensureRoleCanListUsers(Role role) {
        if (role == Role.ADMIN || role == Role.MANAGER || role == Role.AGENT) {
            return;
        }

        if (role == Role.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers cannot list organization users");
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed to list organization users");
    }

    private void requirePrincipal(ResolveHubUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
}
