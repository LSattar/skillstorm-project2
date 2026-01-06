package com.skillstorm.reserveone.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.models.OAuthIdentity;
import com.skillstorm.reserveone.models.OAuthProvider;
import com.skillstorm.reserveone.models.Role;
import com.skillstorm.reserveone.models.User;
import com.skillstorm.reserveone.repositories.OAuthIdentityRepository;
import com.skillstorm.reserveone.repositories.UserRepository;

@Service
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    /**
     * PSEUDOCODE / INTENT (OIDC providers like Google)
     *
     * Why this exists:
     * - Google uses OIDC, so Spring Security uses an OIDC user service, not the
     * plain OAuth2 user service.
     * - We must enrich the OIDC principal with DB roles and a "localUserId"
     * attribute.
     *
     * High-level login flow:
     * 1) Delegate to Spring's OidcUserService to get idToken/userInfo/attributes.
     * 2) Determine provider + providerUserId ("sub" preferred; fallback "id").
     * 3) Resolve local user:
     * - If oauth_identities has (provider, providerUserId): use that user (reload
     * with roles when possible).
     * - Else if provider email is VERIFIED:
     * - try find local user by email (load roles)
     * - if none exists: create local user with GUEST role
     * then create oauth_identity linking (provider, providerUserId) -> user
     * 4) Build authorities:
     * - keep original OIDC authorities (e.g., OIDC_USER + SCOPE_*)
     * - add DB roles as ROLE_<NAME>
     * 5) Return an OidcUser that preserves tokens/userInfo but overrides
     * getAttributes()
     * to include "localUserId".
     *
     * IMPORTANT:
     * - For Google, profile/email fields are frequently in the ID Token claims,
     * not in oidcUser.getAttributes() (UserInfo).
     * - So we merge ID token claims + UserInfo attributes before reading fields,
     * ensuring new Google users get saved with email/name in the DB.
     */

    private static final Logger log = LoggerFactory.getLogger(CustomOidcUserService.class);

    private static final String ATTR_LOCAL_USER_ID = "localUserId";

    private final OidcUserService delegate = new OidcUserService();

    private final OAuthIdentityRepository oauthRepo;
    private final UserRepository userRepo;
    private final RoleService roleService;

    public CustomOidcUserService(
            OAuthIdentityRepository oauthRepo,
            UserRepository userRepo,
            RoleService roleService) {

        this.oauthRepo = Objects.requireNonNull(oauthRepo, "oauthRepo must not be null");
        this.userRepo = Objects.requireNonNull(userRepo, "userRepo must not be null");
        this.roleService = Objects.requireNonNull(roleService, "roleService must not be null");
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthProvider provider = mapProvider(registrationId);

        // Merge ID token claims + userInfo attributes (Google often uses ID token for
        // email/profile).
        Map<String, Object> attrs = mergedClaims(oidcUser);

        String providerUserId = firstNonBlank(asString(attrs.get("sub")), asString(attrs.get("id")));
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new OAuth2AuthenticationException("Missing user identifier from OAuth provider");
        }

        log.info("OIDC login: provider={}, sub={}, email={}", registrationId, providerUserId, attrs.get("email"));

        OAuthIdentity identity = oauthRepo
                .findByProviderAndProviderUserId(Objects.requireNonNull(provider), providerUserId)
                .orElse(null);

        // Pull profile fields once from merged attrs
        Boolean emailVerified = asBoolean(attrs.get("email_verified"));
        String email = normalizeEmailOptional(asString(attrs.get("email")));
        String givenName = asString(attrs.get("given_name"));
        String familyName = asString(attrs.get("family_name"));

        User user;
        if (identity != null) {
            String existingEmail = identity.getUser().getEmail();
            if (existingEmail != null) {
                user = userRepo.findWithRolesByEmail(existingEmail).orElse(identity.getUser());
            } else {
                user = identity.getUser();
            }
        } else {
            user = null;

            // If provider email is verified, try to link to an existing local user by email
            if (email != null && Boolean.TRUE.equals(emailVerified)) {
                user = userRepo.findWithRolesByEmail(email).orElse(null);
            }

            // Create a brand new local user if none exists
            if (user == null) {
                user = new User(null, null, null);

                // Prefer verified email, but if email_verified is missing, still store the
                // email if present
                // (prevents "new Google users" failing when claims are only in the ID token).
                if (email != null && (emailVerified == null || Boolean.TRUE.equals(emailVerified))) {
                    user.setEmail(email);
                }

                user.setFirstName(givenName);
                user.setLastName(familyName);
                user.setStatus(User.Status.ACTIVE);

                Role guest = roleService.getOrCreateEntityByName("GUEST");
                user.addRole(guest);

                user = userRepo.saveAndFlush(user);

                final UUID savedUserId = user.getUserId();

                user = userRepo.findWithRolesByUserId(Objects.requireNonNull(savedUserId))
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
                    log.warn(
                            "Replacing existing OAuthIdentity for userId={} provider={} (old sub={}, new sub={})",
                            userId,
                            provider,
                            existing.getProviderUserId(),
                            providerUserId);
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

        // Upsert Google profile info into the local user record (ensures new users get
        // stored values)
        boolean changed = false;

        if (email != null && (emailVerified == null || Boolean.TRUE.equals(emailVerified))) {
            String currentEmail = user.getEmail();
            if (currentEmail == null || !currentEmail.equalsIgnoreCase(email)) {
                user.setEmail(email);
                changed = true;
            }
        }

        if (givenName != null && !givenName.isBlank()
                && (user.getFirstName() == null || user.getFirstName().isBlank())) {
            user.setFirstName(givenName);
            changed = true;
        }

        if (familyName != null && !familyName.isBlank()
                && (user.getLastName() == null || user.getLastName().isBlank())) {
            user.setLastName(familyName);
            changed = true;
        }

        if (changed) {
            user = userRepo.saveAndFlush(user);
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        // Keep the OIDC-provided authorities (OIDC_USER, scopes)
        authorities.addAll(oidcUser.getAuthorities());
        // Add DB roles
        for (Role r : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + r.getName()));
        }

        Map<String, Object> enrichedAttrs = new HashMap<>(attrs);
        enrichedAttrs.put(ATTR_LOCAL_USER_ID, user.getUserId().toString());

        // DefaultOidcUser will expose idToken/userInfo; attributes come from the
        // UserInfo endpoint,
        // but we override getAttributes() so downstream code sees merged claims +
        // localUserId.
        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "sub") {
            @Override
            public Map<String, Object> getAttributes() {
                return enrichedAttrs;
            }
        };
    }

    private static Map<String, Object> mergedClaims(OidcUser oidcUser) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (oidcUser.getIdToken() != null && oidcUser.getIdToken().getClaims() != null) {
            out.putAll(oidcUser.getIdToken().getClaims());
        }
        // Let UserInfo attrs win if present
        if (oidcUser.getAttributes() != null) {
            out.putAll(oidcUser.getAttributes());
        }
        return out;
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