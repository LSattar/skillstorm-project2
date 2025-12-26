package com.skillstorm.reserveone.controllers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthMeController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        OAuth2User principal = token.getPrincipal();

        // We will inject this in CustomOAuth2UserService (see next section)
        UUID localUserId = null;
        Object idObj = principal.getAttributes().get("localUserId");
        if (idObj != null) {
            try {
                localUserId = UUID.fromString(String.valueOf(idObj));
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<String> roles = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .toList();

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "registrationId", token.getAuthorizedClientRegistrationId(), // "google"
                "localUserId", localUserId == null ? null : localUserId.toString(),
                "email", principal.getAttribute("email"),
                "given_name", principal.getAttribute("given_name"),
                "family_name", principal.getAttribute("family_name"),
                "sub", principal.getAttribute("sub"),
                "roles", roles));
    }
}
