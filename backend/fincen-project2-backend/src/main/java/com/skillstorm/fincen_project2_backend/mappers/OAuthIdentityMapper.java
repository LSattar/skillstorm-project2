package com.skillstorm.fincen_project2_backend.mappers;

import com.skillstorm.fincen_project2_backend.dto.oauthidentities.CreateOAuthIdentityRequest;
import com.skillstorm.fincen_project2_backend.dto.oauthidentities.OAuthIdentityResponse;
import com.skillstorm.fincen_project2_backend.models.OAuthIdentity;
import com.skillstorm.fincen_project2_backend.models.User;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;

@Component
public class OAuthIdentityMapper {
    // CREATE
    @Nullable
    public OAuthIdentity toEntity(@NonNull User user, @Nullable CreateOAuthIdentityRequest req) {
        if (req == null) {
            return null;
        }

        String providerUserId = req.providerUserId().trim();
        return new OAuthIdentity(user, req.provider(), providerUserId);
    }

    // READ
    @Nullable
    public OAuthIdentityResponse toResponse(@Nullable OAuthIdentity oauthIdentity) {
        if (oauthIdentity == null) {
            return null;
        }

        return new OAuthIdentityResponse(
                oauthIdentity.getOauthIdentityId(),
                oauthIdentity.getProvider(),
                oauthIdentity.getProviderUserId(),
                oauthIdentity.getCreatedAt(),
                oauthIdentity.getUpdatedAt());
    }

}
