package com.skillstorm.fincen_project2_backend.services;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.dto.oauthidentities.CreateOAuthIdentityRequest;
import com.skillstorm.fincen_project2_backend.dto.oauthidentities.OAuthIdentityResponse;
import com.skillstorm.fincen_project2_backend.exceptions.ResourceConflictException;
import com.skillstorm.fincen_project2_backend.exceptions.ResourceNotFoundException;
import com.skillstorm.fincen_project2_backend.mappers.OAuthIdentityMapper;
import com.skillstorm.fincen_project2_backend.models.OAuthIdentity;
import com.skillstorm.fincen_project2_backend.models.OAuthIdentity.Provider;
import com.skillstorm.fincen_project2_backend.models.User;
import com.skillstorm.fincen_project2_backend.repositories.OAuthIdentityRepository;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthIdentityService {

    private final OAuthIdentityRepository repo;
    private final OAuthIdentityMapper mapper;

    public OAuthIdentityService(@NonNull OAuthIdentityRepository repo, @NonNull OAuthIdentityMapper mapper) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * Link an OAuth provider identity to a user.
     *
     * Enforces:
     * - provider + providerUserId must be unique (uq_provider_user)
     * - user can only have one identity per provider (uq_user_provider)
     *
     * Notes:
     * - DataIntegrityViolationException should bubble up to GlobalExceptionHandler.
     */
    @Transactional
    public OAuthIdentityResponse create(@NonNull User user, @NonNull CreateOAuthIdentityRequest req) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(req, "req must not be null");

        Provider provider = Objects.requireNonNull(req.provider(), "Provider is required.");
        String providerUserId = normalizeRequired(req.providerUserId(), "Provider user id is required.");

        // Fail fast: one provider per user
        UUID userId = Objects.requireNonNull(user.getUserId(), "userId must not be null");
        if (repo.existsByUser_UserIdAndProvider(userId, provider)) {
            throw new ResourceConflictException("User already has an identity for this provider.");
        }

        // Fail fast: provider+providerUserId cannot be linked to multiple users
        Optional<OAuthIdentity> existing = repo.findByProviderAndProviderUserId(provider, providerUserId);
        if (existing.isPresent()) {
            throw new ResourceConflictException("This provider identity is already linked to a user.");
        }

        OAuthIdentity entity = Objects.requireNonNull(mapper.toEntity(user, req), "mapper.toEntity returned null");
        OAuthIdentity saved = Objects.requireNonNull(repo.save(entity), "repo.save returned null");

        return Objects.requireNonNull(mapper.toResponse(saved), "mapper.toResponse returned null");
    }

    /**
     * Used during OAuth login: look up identity by provider + providerUserId.
     */
    @Transactional(readOnly = true)
    public OAuthIdentity getEntityByProviderAndProviderUserId(@NonNull Provider provider,
            @NonNull String providerUserId) {
        Provider p = Objects.requireNonNull(provider, "provider must not be null");
        String pid = normalizeRequired(providerUserId, "Provider user id is required.");

        return Objects.requireNonNull(
                repo.findByProviderAndProviderUserId(p, pid)
                        .orElseThrow(() -> new ResourceNotFoundException("OAuth identity not found.")),
                "oauthIdentity must not be null");
    }

    /**
     * Handy for “does this user have Google linked?”
     */
    @Transactional(readOnly = true)
    public OAuthIdentityResponse getByUserAndProvider(@NonNull UUID userId, @NonNull Provider provider) {
        UUID id = Objects.requireNonNull(userId, "userId must not be null");
        Provider p = Objects.requireNonNull(provider, "provider must not be null");

        OAuthIdentity identity = Objects.requireNonNull(
                repo.findByUser_UserIdAndProvider(id, p)
                        .orElseThrow(() -> new ResourceNotFoundException("OAuth identity not found.")),
                "oauthIdentity must not be null");

        return Objects.requireNonNull(mapper.toResponse(identity), "mapper.toResponse returned null");
    }

    /**
     * Unlink provider from user. If you want idempotent behavior, you can
     * replace the not found check with a direct delete call.
     */
    @Transactional
    public void deleteByUserAndProvider(@NonNull UUID userId, @NonNull Provider provider) {
        UUID id = Objects.requireNonNull(userId, "userId must not be null");
        Provider p = Objects.requireNonNull(provider, "provider must not be null");

        if (!repo.existsByUser_UserIdAndProvider(id, p)) {
            throw new ResourceNotFoundException("OAuth identity not found.");
        }

        repo.deleteByUser_UserIdAndProvider(id, p);
    }

    private static String normalizeRequired(String value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }
}
