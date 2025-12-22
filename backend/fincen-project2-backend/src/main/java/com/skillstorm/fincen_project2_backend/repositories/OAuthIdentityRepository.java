package com.skillstorm.fincen_project2_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.models.OAuthIdentity;
import com.skillstorm.fincen_project2_backend.models.OAuthIdentity.Provider;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, UUID> {
    // Used during OAuth login (provider + provider user id)
    Optional<OAuthIdentity> findByProviderAndProviderUserId(
            Provider provider,
            String providerUserId);

    // Enforces "one provider per user" rule
    boolean existsByUser_UserIdAndProvider(UUID userId, Provider provider);

    // Optional: cleanup or checks
    void deleteByUser_UserIdAndProvider(UUID userId, Provider provider);

    // Handy for "get this user's Google identity" or "unlink this provider"
    Optional<OAuthIdentity> findByUser_UserIdAndProvider(UUID userId, Provider provider);
}
