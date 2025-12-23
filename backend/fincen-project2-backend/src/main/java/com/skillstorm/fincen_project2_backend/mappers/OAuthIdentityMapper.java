package com.skillstorm.fincen_project2_backend.mappers;

import com.skillstorm.fincen_project2_backend.dto.oauthidentities.CreateOAuthIdentityRequest;
import com.skillstorm.fincen_project2_backend.dto.oauthidentities.OAuthIdentityResponse;
import com.skillstorm.fincen_project2_backend.models.OAuthIdentity;
import com.skillstorm.fincen_project2_backend.models.User;

import org.springframework.stereotype.Component;

@Component
public class OAuthIdentityMapper {
    // CREATE
    public OAuthIdentity toEntity(User user, CreateOAuthIdentityRequest req) {
        if (req == null) {
            return null;
        }

        if (user == null) {
            throw new IllegalArgumentException("User must not be null when creating OAuthIdentity");
        }

        if (req.provider() == null) {
            throw new IllegalArgumentException("Provider must not be null");
        }

        String providerUserId = req.providerUserId() == null ? null : req.providerUserId().trim();
        return new OAuthIdentity(user, req.provider(), providerUserId);
    }

    // READ
    public OAuthIdentityResponse toResponse(OAuthIdentity oauthIdentity) {
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
