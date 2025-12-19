package com.skillstorm.fincen_project2_backend.dto.oauthidentities;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.skillstorm.fincen_project2_backend.models.OAuthIdentity.Provider;

public record OAuthIdentityResponse(
        UUID oauthIdentityId,
        Provider provider,
        String providerUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

}