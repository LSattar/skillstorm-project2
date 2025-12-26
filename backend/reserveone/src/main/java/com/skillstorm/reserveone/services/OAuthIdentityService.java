package com.skillstorm.reserveone.services;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.dto.oauthidentities.CreateOAuthIdentityRequest;
import com.skillstorm.reserveone.dto.oauthidentities.OAuthIdentityResponse;
import com.skillstorm.reserveone.exceptions.ResourceConflictException;
import com.skillstorm.reserveone.exceptions.ResourceNotFoundException;
import com.skillstorm.reserveone.mappers.OAuthIdentityMapper;
import com.skillstorm.reserveone.models.OAuthIdentity;
import com.skillstorm.reserveone.models.OAuthProvider;
import com.skillstorm.reserveone.models.User;
import com.skillstorm.reserveone.repositories.OAuthIdentityRepository;

@Service
public class OAuthIdentityService {

    private final OAuthIdentityRepository repo;
    private final OAuthIdentityMapper mapper;

    public OAuthIdentityService(
            @NonNull OAuthIdentityRepository repo,
            @NonNull OAuthIdentityMapper mapper) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    // CREATE (controller-driven)
    @Transactional
    public OAuthIdentityResponse create(@NonNull User user, @NonNull CreateOAuthIdentityRequest req) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(req, "req must not be null");

        OAuthIdentity entity = Objects.requireNonNull(
                mapper.toEntity(user, req),
                "mapper.toEntity returned null");

        try {
            OAuthIdentity saved = Objects.requireNonNull(repo.save(entity), "repo.save returned null");
            return Objects.requireNonNull(mapper.toResponse(saved), "mapper.toResponse returned null");
        } catch (DataIntegrityViolationException e) {
            String msg = mostSpecificMessage(e);

            if (containsConstraint(msg, "uq_user_provider")) {
                throw new ResourceConflictException("User already has an identity for this provider.");
            }
            if (containsConstraint(msg, "uq_provider_user")) {
                throw new ResourceConflictException("This provider identity is already linked to a user.");
            }

            throw e;
        }
    }

    // READ (lean fetch) by provider + user id
    @Transactional(readOnly = true)
    public OAuthIdentity getEntityByProviderAndProviderUserId(
            @NonNull OAuthProvider provider,
            @NonNull String providerUserId) {

        OAuthProvider p = Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(providerUserId, "providerUserId must not be null");

        String pid = providerUserId.trim();
        if (pid.isBlank()) {
            throw new IllegalArgumentException("providerUserId must not be blank");
        }

        return repo.findByProviderAndProviderUserId(p, pid)
                .orElseThrow(() -> new ResourceNotFoundException("OAuth identity not found."));
    }

    // READ (security-safe fetch) with user + user.roles
    @Transactional(readOnly = true)
    public OAuthIdentity getEntityWithUserAndRolesByProviderAndProviderUserId(
            @NonNull OAuthProvider provider,
            @NonNull String providerUserId) {

        OAuthProvider p = Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(providerUserId, "providerUserId must not be null");

        String pid = providerUserId.trim();
        if (pid.isBlank()) {
            throw new IllegalArgumentException("providerUserId must not be blank");
        }

        return repo.findWithUserAndRolesByProviderAndProviderUserId(p, pid)
                .orElseThrow(() -> new ResourceNotFoundException("OAuth identity not found."));
    }

    // UPSERT-LIKE helper for OAuth login flows (safe + idempotent)
    @Transactional
    public OAuthIdentity linkOrGet(
            @NonNull User user,
            @NonNull OAuthProvider provider,
            @NonNull String providerUserId) {

        Objects.requireNonNull(user, "user must not be null");
        OAuthProvider p = Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(providerUserId, "providerUserId must not be null");

        String pid = providerUserId.trim();
        if (pid.isBlank()) {
            throw new IllegalArgumentException("providerUserId must not be blank");
        }

        // Fast path
        var existing = repo.findByProviderAndProviderUserId(p, pid);
        if (existing.isPresent()) {
            return existing.get();
        }

        OAuthIdentity entity = new OAuthIdentity(user, p, pid);

        try {
            return Objects.requireNonNull(repo.save(entity), "repo.save returned null");
        } catch (DataIntegrityViolationException e) {
            // Race: created between check + save
            return repo.findByProviderAndProviderUserId(p, pid)
                    .orElseThrow(() -> e);
        }
    }

    // ----------------------------
    // READ by user + provider
    // ----------------------------
    @Transactional(readOnly = true)
    public OAuthIdentityResponse getByUserAndProvider(@NonNull UUID userId, @NonNull OAuthProvider provider) {
        UUID id = Objects.requireNonNull(userId, "userId must not be null");
        OAuthProvider p = Objects.requireNonNull(provider, "provider must not be null");

        final OAuthIdentity identity = Objects.requireNonNull(
                repo.findByUser_UserIdAndProvider(id, p)
                        .orElseThrow(() -> new ResourceNotFoundException("OAuth identity not found.")),
                "identity must not be null");

        return Objects.requireNonNull(mapper.toResponse(identity), "mapper.toResponse returned null");
    }


    // DELETE by user + provider
    @Transactional
    public void deleteByUserAndProvider(@NonNull UUID userId, @NonNull OAuthProvider provider) {
        UUID id = Objects.requireNonNull(userId, "userId must not be null");
        OAuthProvider p = Objects.requireNonNull(provider, "provider must not be null");

        if (!repo.existsByUser_UserIdAndProvider(id, p)) {
            throw new ResourceNotFoundException("OAuth identity not found.");
        }
        repo.deleteByUser_UserIdAndProvider(id, p);
    }

    // ----------------------------
    // LIST identities for user
    // ----------------------------
    @Transactional(readOnly = true)
    public List<OAuthIdentityResponse> listForUser(@NonNull UUID userId) {
        UUID id = Objects.requireNonNull(userId, "userId must not be null");

        return repo.findAllByUser_UserId(id).stream()
                .map(mapper::toResponse)
                .toList();
    }

    // Helpers
    private static String mostSpecificMessage(DataIntegrityViolationException e) {
        Throwable t = e.getMostSpecificCause();
        return t != null ? t.getMessage() : null;
    }

    private static boolean containsConstraint(String message, String constraintName) {
        return message != null && message.contains(constraintName);
    }
}
