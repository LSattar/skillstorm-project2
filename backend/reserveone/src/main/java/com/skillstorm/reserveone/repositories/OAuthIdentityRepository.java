package com.skillstorm.reserveone.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import com.skillstorm.reserveone.models.OAuthIdentity;
import com.skillstorm.reserveone.models.OAuthProvider;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, UUID> {

    // Provider based lookup (account linking flows)
    Optional<OAuthIdentity> findByProviderAndProviderUserId(
            @NonNull OAuthProvider provider,
            @NonNull String providerUserId);

    @EntityGraph(attributePaths = { "user", "user.roles" })
    Optional<OAuthIdentity> findWithUserAndRolesByProviderAndProviderUserId(
            @NonNull OAuthProvider provider,
            @NonNull String providerUserId);

    boolean existsByProviderAndProviderUserId(
            @NonNull OAuthProvider provider,
            @NonNull String providerUserId);

    // User + provider (one identity per provider per user)
    Optional<OAuthIdentity> findByUser_UserIdAndProvider(
            @NonNull UUID userId,
            @NonNull OAuthProvider provider);

    boolean existsByUser_UserIdAndProvider(
            @NonNull UUID userId,
            @NonNull OAuthProvider provider);

    void deleteByUser_UserIdAndProvider(
            @NonNull UUID userId,
            @NonNull OAuthProvider provider);

    void deleteAllByUser_UserId(@NonNull UUID userId);

    // User scoped helpers
    List<OAuthIdentity> findAllByUser_UserId(@NonNull UUID userId);

    @EntityGraph(attributePaths = { "user", "user.roles" })
    List<OAuthIdentity> findAllWithUserAndRolesByUser_UserId(@NonNull UUID userId);
}
