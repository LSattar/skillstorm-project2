package com.skillstorm.fincen_project2_backend.dto.oauthidentities;

import com.skillstorm.fincen_project2_backend.models.OAuthIdentity.Provider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOAuthIdentityRequest(
        @NotNull Provider provider,
        @NotBlank @Size(max = 255) String providerUserId) {
}
