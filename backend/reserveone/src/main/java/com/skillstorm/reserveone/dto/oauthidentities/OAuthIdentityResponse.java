package com.skillstorm.reserveone.dto.oauthidentities;

import java.util.UUID;

import com.skillstorm.reserveone.models.OAuthProvider;

public record OAuthIdentityResponse(
                UUID oauthIdentityId,
                OAuthProvider provider,
                String providerUserId) {
}
