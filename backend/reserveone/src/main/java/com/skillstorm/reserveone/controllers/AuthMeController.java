package com.skillstorm.reserveone.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.reserveone.models.User;
import com.skillstorm.reserveone.repositories.UserRepository;

@RestController
@RequestMapping("/auth")
public class AuthMeController {

    private final UserRepository userRepo;

    public AuthMeController(UserRepository userRepo) {
        this.userRepo = Objects.requireNonNull(userRepo, "userRepo must not be null");
    }

    /**
     * PSEUDOCODE / INTENT (SPA session introspection)
     *
     * Goal:
     * - Provide a single endpoint the Angular app can call (with
     * credentials/cookies)
     * to determine whether a user is logged in and what authorities the session
     * has.
     *
     * Response behavior:
     * - If no OAuth2AuthenticationToken exists: return 401 { authenticated:false }
     * - If authenticated:
     * - read "localUserId" from principal attributes (set by
     * CustomOidcUserService/CustomOAuth2UserService)
     * - include identity attributes like email/given_name/family_name/sub
     * - include authorities:
     * - token.getAuthorities(): what Security uses for access decisions (roles +
     * scopes)
     * - principal.getAuthorities(): what the principal exposes (usually matches)
     */

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

        List<String> authAuthorities = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .toList();

        List<String> principalAuthorities = AuthorityUtils.authorityListToSet(principal.getAuthorities()).stream()
                .toList();

        // Use a mutable map so optional attributes can be null without throwing.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("authenticated", true);
        body.put("registrationId", token.getAuthorizedClientRegistrationId());
        body.put("localUserId", localUserId == null ? null : localUserId.toString());
        body.put("localUserIdRaw", idObj == null ? null : String.valueOf(idObj));
        Object principalEmail = principal.getAttribute("email");
        Object givenName = principal.getAttribute("given_name");
        Object familyName = principal.getAttribute("family_name");

        String dbEmail = null;
        String dbFirst = null;
        String dbLast = null;

        User dbUser = null;
        if (localUserId != null) {
            dbUser = userRepo.findWithRolesByUserId(localUserId).orElse(null);
        }
        if (dbUser != null) {
            dbEmail = dbUser.getEmail();
            dbFirst = dbUser.getFirstName();
            dbLast = dbUser.getLastName();
        }

        List<String> dbRoles = List.of();
        if (dbUser != null) {
            dbRoles = dbUser.getRoles().stream()
                    .map(r -> r == null ? null : r.getName())
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .sorted()
                    .collect(Collectors.toList());
        }

        body.put("email", dbEmail != null ? dbEmail : principalEmail);
        body.put("given_name", dbFirst != null ? dbFirst : givenName);
        body.put("family_name", dbLast != null ? dbLast : familyName);
        // convenient aliases for SPA
        body.put("firstName", dbFirst != null ? dbFirst : givenName);
        body.put("lastName", dbLast != null ? dbLast : familyName);
        body.put("sub", principal.getAttribute("sub"));
        body.put("roles", authAuthorities);
        body.put("principalAuthorities", principalAuthorities);
        body.put("dbRoles", dbRoles);

        return ResponseEntity.ok(body);
    }
}
