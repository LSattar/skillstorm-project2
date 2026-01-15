package com.skillstorm.reserveone.services;

import java.time.Instant;
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
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
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

    private static final Logger log = LoggerFactory.getLogger(CustomOidcUserService.class);

    private static final String ATTR_LOCAL_USER_ID = "localUserId";
    private static final String ATTR_SUB = "sub";
    private static final String ATTR_ID = "id";
    private static final String ATTR_EMAIL = "email";
    private static final String ATTR_EMAIL_VERIFIED = "email_verified";
    private static final String ATTR_GIVEN_NAME = "given_name";
    private static final String ATTR_FAMILY_NAME = "family_name";

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

        // Merge ID token claims + userInfo attributes
        Map<String, Object> attrs = mergedClaims(oidcUser);

        // Provider user id: prefer "sub" then fallback to "id"
        String providerUserId = firstNonBlank(asString(attrs.get(ATTR_SUB)), asString(attrs.get(ATTR_ID)));
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new OAuth2AuthenticationException("Missing user identifier from OAuth provider");
        }
        providerUserId = providerUserId.trim();

        // Pull profile fields
        Boolean emailVerified = asBoolean(attrs.get(ATTR_EMAIL_VERIFIED));
        String email = normalizeEmailOptional(asString(attrs.get(ATTR_EMAIL)));
        String givenName = asString(attrs.get(ATTR_GIVEN_NAME));
        String familyName = asString(attrs.get(ATTR_FAMILY_NAME));

        log.info("OIDC login: provider={}, sub={}, email={}", registrationId, providerUserId, email);

        OAuthIdentity identity = oauthRepo
                .findByProviderAndProviderUserId(Objects.requireNonNull(provider), providerUserId)
                .orElse(null);

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

            // Link to an existing local user by verified email
            if (email != null && Boolean.TRUE.equals(emailVerified)) {
                user = userRepo.findWithRolesByEmail(email).orElse(null);
            }

            // Create new local user
            if (user == null) {
                user = new User(null, null, null);

                if (email != null && (emailVerified == null || Boolean.TRUE.equals(emailVerified))) {
                    user.setEmail(email);
                }

                user.setFirstName(givenName);
                user.setLastName(familyName);
                user.setStatus(User.Status.ACTIVE);

                Role guest = roleService.getOrCreateEntityByName("GUEST");
                user.addRole(guest);

                user = userRepo.saveAndFlush(user);

                final UUID savedUserId = Objects.requireNonNull(user.getUserId(), "userId must not be null");
                user = userRepo.findWithRolesByUserId(savedUserId)
                        .orElseThrow(() -> new IllegalStateException(
                                "User saved but could not be reloaded with roles: " + savedUserId));
            }

            // Enforce unique user+provider mapping
            UUID userId = Objects.requireNonNull(user.getUserId(), "userId must not be null");
            var existingByUserAndProvider = oauthRepo.findByUser_UserIdAndProvider(userId, provider);
            if (existingByUserAndProvider.isPresent()) {
                OAuthIdentity existing = existingByUserAndProvider.get();
                if (!providerUserId.equals(existing.getProviderUserId())) {
                    log.warn("Replacing OAuthIdentity for userId={} provider={} (old sub={}, new sub={})",
                            userId, provider, existing.getProviderUserId(), providerUserId);
                    oauthRepo.delete(existing);
                    oauthRepo.flush();
                }
            }

            oauthRepo.save(new OAuthIdentity(user, provider, providerUserId));
        }

        // Upsert profile info
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

        // --------------------------
        // SESSION-SAFE PRINCIPAL:
        // only store Strings/booleans/numbers in attributes
        // and rebuild token/userInfo from that safe map.
        // --------------------------
        Map<String, Object> safeClaims = new LinkedHashMap<>();

        // Required for DefaultOidcUser(nameAttributeKey="sub")
        safeClaims.put(ATTR_SUB, providerUserId);

        // Useful profile fields (keep serializable)
        if (email != null)
            safeClaims.put(ATTR_EMAIL, email);
        if (emailVerified != null)
            safeClaims.put(ATTR_EMAIL_VERIFIED, emailVerified);
        if (givenName != null && !givenName.isBlank())
            safeClaims.put(ATTR_GIVEN_NAME, givenName);
        if (familyName != null && !familyName.isBlank())
            safeClaims.put(ATTR_FAMILY_NAME, familyName);

        // Your local user id as String (do NOT put UUID object)
        UUID localUserId = Objects.requireNonNull(user.getUserId(), "userId must not be null");
        safeClaims.put(ATTR_LOCAL_USER_ID, localUserId.toString());

        // Authorities ONLY from DB roles (keeps it simple + serializable)
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role r : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + r.getName()));
        }
        // Optional: ensure at least one authority exists
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_GUEST"));
        }

        // Rebuild a clean idToken using safe claims
        OidcIdToken original = oidcUser.getIdToken();
        String tokenValue = (original != null ? original.getTokenValue() : "n/a");
        Instant issuedAt = (original != null ? original.getIssuedAt() : Instant.now());
        Instant expiresAt = (original != null ? original.getExpiresAt() : issuedAt.plusSeconds(3600));

        OidcIdToken rebuiltIdToken = new OidcIdToken(tokenValue, issuedAt, expiresAt, new HashMap<>(safeClaims));
        OidcUserInfo rebuiltUserInfo = new OidcUserInfo(new HashMap<>(safeClaims));

        return new DefaultOidcUser(authorities, rebuiltIdToken, rebuiltUserInfo, ATTR_SUB);
    }

    private static Map<String, Object> mergedClaims(OidcUser oidcUser) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (oidcUser.getIdToken() != null && oidcUser.getIdToken().getClaims() != null) {
            out.putAll(oidcUser.getIdToken().getClaims());
        }
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
        if (v instanceof Boolean b)
            return b;
        if (v == null)
            return null;
        String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
        if ("true".equals(s))
            return Boolean.TRUE;
        if ("false".equals(s))
            return Boolean.FALSE;
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank())
            return a.trim();
        if (b != null && !b.isBlank())
            return b.trim();
        return null;
    }

    private static String normalizeEmailOptional(String value) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        if (trimmed.isBlank())
            return null;
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
