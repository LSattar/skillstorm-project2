package com.skillstorm.fincen_project2_backend.controllers;

import java.util.Objects;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.constants.RoleConstants;
import com.skillstorm.fincen_project2_backend.dto.oauthidentities.CreateOAuthIdentityRequest;
import com.skillstorm.fincen_project2_backend.dto.oauthidentities.OAuthIdentityResponse;
import com.skillstorm.fincen_project2_backend.models.User;
import com.skillstorm.fincen_project2_backend.services.OAuthIdentityService;
import com.skillstorm.fincen_project2_backend.services.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/oauthidentities")
@Validated
public class OAuthIdentityController {

    private final OAuthIdentityService oauthIdentityService;
    private final UserService userService;

    public OAuthIdentityController(@NonNull OAuthIdentityService oauthIdentityService,
            @NonNull UserService userService) {

        this.oauthIdentityService = Objects.requireNonNull(oauthIdentityService, "oauthIdentityService cannot be null");
        this.userService = Objects.requireNonNull(userService, "userService cannot be null");
    }

    @PostMapping
    public ResponseEntity<OAuthIdentityResponse> create(
            @RequestParam @NonNull UUID userId,
            @Valid @RequestBody @NonNull CreateOAuthIdentityRequest req,
            Authentication authentication) {

        // Authorization: non-admins can only link OAuth identities to themselves
        if (!hasRole(authentication, RoleConstants.ROLE_ADMIN)) {
            User authenticatedUser = userService.getEntityByEmail(authentication.getName());
            if (!userId.equals(authenticatedUser.getUserId())) {
                throw new AccessDeniedException("Cannot link OAuth identity to another user.");
            }
        }

        User user = userService.getEntityById(userId);
        OAuthIdentityResponse created = oauthIdentityService.create(user, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(role));
    }
}
