package com.skillstorm.reserveone.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.models.OAuthIdentity;
import com.skillstorm.reserveone.models.OAuthProvider;
import com.skillstorm.reserveone.models.Role;
import com.skillstorm.reserveone.models.User;
import com.skillstorm.reserveone.repositories.OAuthIdentityRepository;
import com.skillstorm.reserveone.repositories.UserRepository;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    /**
     * PSEUDOCODE / INTENT (non-OIDC OAuth2 providers)
     *
     * Goal:
     * - Ensure every OAuth2 login maps to exactly one local User record.
     * - Ensure the Security principal contains DB-backed role authorities (ROLE_*).
     * - Expose the local user id to the frontend via attribute "localUserId".
     *
     * High-level login flow:
     * 1) Delegate to Spring (super.loadUser) to fetch provider attributes.
     * 2) Determine provider + providerUserId ("sub" preferred; fallback "id").
     * 3) Resolve local user:
     * - If oauth_identities has (provider, providerUserId): use that user.
     * - Else if provider email is VERIFIED:
     * - try find local user by email (load roles)
     * - if none exists: create local user with GUEST role
     * then create oauth_identity linking (provider, providerUserId) -> user
     * 4) Build authorities = DB roles as ROLE_<NAME>.
     * 5) Return DefaultOAuth2User with enriched attributes incl. "localUserId".
     */

    private static final String ATTR_LOCAL_USER_ID = "localUserId";

    private final OAuthIdentityRepository oauthRepo;
    private final UserRepository userRepo;
    private final RoleService roleService;

    public CustomOAuth2UserService(
            OAuthIdentityRepository oauthRepo,
            UserRepository userRepo,
            RoleService roleService) {

        this.oauthRepo = Objects.requireNonNull(oauthRepo, "oauthRepo must not be null");
        this.userRepo = Objects.requireNonNull(userRepo, "userRepo must not be null");
        this.roleService = Objects.requireNonNull(roleService, "roleService must not be null");
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthProvider provider = Objects.requireNonNull(mapProvider(registrationId), "provider must not be null");

        Map<String, Object> attrs = oauth2User.getAttributes();

        String providerUserId = firstNonBlank(asString(attrs.get("sub")), asString(attrs.get("id")));
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new OAuth2AuthenticationException("Missing user identifier from OAuth provider");
        }

        // Find identity first (authoritative)
        OAuthIdentity identity = oauthRepo.findByProviderAndProviderUserId(provider, providerUserId).orElse(null);

        User user;
        if (identity != null) {
            // Ensure roles are loaded for authorities
            String existingEmail = identity.getUser().getEmail();
            if (existingEmail != null) {
                user = userRepo.findWithRolesByEmail(existingEmail).orElse(identity.getUser());
            } else {
                user = identity.getUser();
            }
        } else {
            // Only link by email if verified
            Boolean emailVerified = asBoolean(attrs.get("email_verified"));
            String email = normalizeEmailOptional(asString(attrs.get("email")));

            user = null;
            if (email != null && Boolean.TRUE.equals(emailVerified)) {
                user = userRepo.findWithRolesByEmail(email).orElse(null);
            }

            if (user == null) {
                user = new User(null, null, null);

                // Store email only if verified
                if (email != null && Boolean.TRUE.equals(emailVerified)) {
                    user.setEmail(email);
                }

                user.setFirstName(asString(attrs.get("given_name")));
                user.setLastName(asString(attrs.get("family_name")));
                user.setStatus(User.Status.ACTIVE);

                Role guest = roleService.getOrCreateEntityByName("GUEST");
                user.addRole(guest);

                user = userRepo.save(user);

                final UUID savedUserId = user.getUserId();

                user = userRepo
                        .findWithRolesByUserId(Objects.requireNonNull(savedUserId, "savedUserId must not be null"))
                        .orElseThrow(() -> new IllegalStateException(
                                "User saved but could not be reloaded with roles: " + savedUserId));
            }

            // If the user already has an identity for this provider, we must avoid
            // violating uq_user_provider.
            // This can happen when seed data or previous logins created an identity with a
            // different providerUserId.
            UUID userId = Objects.requireNonNull(user.getUserId(), "userId must not be null");
            var existingByUserAndProvider = oauthRepo.findByUser_UserIdAndProvider(userId, provider);
            if (existingByUserAndProvider.isPresent()) {
                OAuthIdentity existing = existingByUserAndProvider.get();
                if (!providerUserId.equals(existing.getProviderUserId())) {
                    // Delete + flush so the unique constraint won't trip on the subsequent insert
                    oauthRepo.delete(existing);
                    oauthRepo.flush();
                } else {
                    // Already linked; nothing to do
                }
            }

            // Ensure the (provider, providerUserId) mapping exists
            OAuthIdentity newIdentity = new OAuthIdentity(user, provider, providerUserId);
            oauthRepo.save(newIdentity);
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role r : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + r.getName()));
        }

        Map<String, Object> enrichedAttrs = new HashMap<>(attrs);
        enrichedAttrs.put(ATTR_LOCAL_USER_ID, user.getUserId().toString());

        // Name attribute key: prefer sub if present, else id
        String nameAttributeKey = attrs.containsKey("sub") ? "sub" : "id";

        return new DefaultOAuth2User(authorities, enrichedAttrs, nameAttributeKey);
    }

    private static OAuthProvider mapProvider(String registrationId) {
        if (registrationId == null) {
            throw new OAuth2AuthenticationException("Missing client registrationId");
        }
        String id = registrationId.trim().toLowerCase(Locale.ROOT);
        if ("google".equals(id)) {
            return OAuthProvider.GOOGLE;
        }
        throw new OAuth2AuthenticationException("Unsupported OAuth provider: " + registrationId);
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Boolean asBoolean(Object v) {
        if (v instanceof Boolean b) {
            return b;
        }
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
        if ("true".equals(s)) {
            return Boolean.TRUE;
        }
        if ("false".equals(s)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    private static String normalizeEmailOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
