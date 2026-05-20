package com.resolvehub.organization.controller;

import com.resolvehub.common.security.ResolveHubUserPrincipal;
import com.resolvehub.organization.dto.CreateOrganizationUserRequest;
import com.resolvehub.organization.dto.OrganizationUserResponse;
import com.resolvehub.organization.service.OrganizationUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organization")
public class OrganizationUserController {

    private final OrganizationUserService organizationUserService;

    public OrganizationUserController(OrganizationUserService organizationUserService) {
        this.organizationUserService = organizationUserService;
    }

    @GetMapping("/users")
    public List<OrganizationUserResponse> listOrganizationUsers(
            @AuthenticationPrincipal ResolveHubUserPrincipal principal
    ) {
        return organizationUserService.listOrganizationUsers(principal);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationUserResponse createOrganizationUser(
            @AuthenticationPrincipal ResolveHubUserPrincipal principal,
            @Valid @RequestBody CreateOrganizationUserRequest request
    ) {
        return organizationUserService.createOrganizationUser(principal, request);
    }
}
