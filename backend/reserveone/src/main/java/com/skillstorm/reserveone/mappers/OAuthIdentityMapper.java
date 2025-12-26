package com.skillstorm.reserveone.mappers;

import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.skillstorm.reserveone.dto.oauthidentities.CreateOAuthIdentityRequest;
import com.skillstorm.reserveone.dto.oauthidentities.OAuthIdentityResponse;
import com.skillstorm.reserveone.models.OAuthIdentity;
import com.skillstorm.reserveone.models.User;

@Component
public class OAuthIdentityMapper {

    @NonNull
    public OAuthIdentity toEntity(@NonNull User user, @NonNull CreateOAuthIdentityRequest req) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(req, "req must not be null");

        // Entity constructor enforces required fields + normalization
        return new OAuthIdentity(
                user,
                req.provider(),
                req.providerUserId());
    }

    @NonNull
    public OAuthIdentityResponse toResponse(@NonNull OAuthIdentity entity) {
        Objects.requireNonNull(entity, "entity must not be null");

        return new OAuthIdentityResponse(
                entity.getOauthIdentityId(),
                entity.getProvider(),
                entity.getProviderUserId());
    }
}
