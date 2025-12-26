package com.skillstorm.reserveone.controllers;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.reserveone.dto.oauthidentities.CreateOAuthIdentityRequest;
import com.skillstorm.reserveone.dto.oauthidentities.OAuthIdentityResponse;
import com.skillstorm.reserveone.models.User;
import com.skillstorm.reserveone.services.OAuthIdentityService;
import com.skillstorm.reserveone.services.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/oauthidentities")
@Validated
public class OAuthIdentityController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ATTR_LOCAL_USER_ID = "localUserId";

    private final OAuthIdentityService oauthIdentityService;
    private final UserService userService;

    public OAuthIdentityController(
            @NonNull OAuthIdentityService oauthIdentityService,
            @NonNull UserService userService) {

        this.oauthIdentityService = Objects.requireNonNull(oauthIdentityService, "oauthIdentityService cannot be null");
        this.userService = Objects.requireNonNull(userService, "userService cannot be null");
    }

    @PostMapping
    public ResponseEntity<OAuthIdentityResponse> create(
            @RequestParam @NonNull UUID userId,
            @Valid @RequestBody @NonNull CreateOAuthIdentityRequest req,
            Authentication authentication) {

        // Non-admins can only link identities to themselves
        if (!hasRole(authentication, ROLE_ADMIN)) {
            UUID authenticatedUserId = extractLocalUserId(authentication);
            if (authenticatedUserId == null) {
                throw new AccessDeniedException("Missing local user id in session principal.");
            }
            if (!userId.equals(authenticatedUserId)) {
                throw new AccessDeniedException("Cannot link OAuth identity to another user.");
            }
        }

        User user = userService.getEntityById(userId);
        OAuthIdentityResponse created = oauthIdentityService.create(user, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private static UUID extractLocalUserId(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            return null;
        }

        OAuth2User principal = token.getPrincipal();
        Object v = principal.getAttributes().get(ATTR_LOCAL_USER_ID);
        if (v == null) {
            return null;
        }

        try {
            return UUID.fromString(String.valueOf(v));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
