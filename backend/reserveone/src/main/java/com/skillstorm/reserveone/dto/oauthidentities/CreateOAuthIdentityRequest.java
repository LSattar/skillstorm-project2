package com.skillstorm.reserveone.dto.oauthidentities;

import com.skillstorm.reserveone.models.OAuthProvider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOAuthIdentityRequest(
                @NotNull OAuthProvider provider,
                @NotBlank String providerUserId) {
}