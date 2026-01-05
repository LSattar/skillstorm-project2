package com.skillstorm.reserveone.models;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "oauth_identities", uniqueConstraints = {
        @UniqueConstraint(name = "uq_provider_user", columnNames = { "provider", "provider_user_id" }),
        @UniqueConstraint(name = "uq_user_provider", columnNames = { "user_id", "provider" })
})
public class OAuthIdentity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @JsonIgnore
    private User user;

    @Id
    @UuidGenerator
    @Column(name = "oauth_identity_id", nullable = false, updatable = false)
    private UUID oauthIdentityId;

    @NotNull
    @Convert(converter = OAuthProviderConverter.class)
    @Column(name = "provider", nullable = false, updatable = false, length = 50)
    private OAuthProvider provider;

    @NotBlank
    @Size(max = 255)
    @Column(name = "provider_user_id", nullable = false, updatable = false, length = 255)
    private String providerUserId;

    // DB-populated (DEFAULT now())
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // DB-populated (trigger updates on UPDATE)
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected OAuthIdentity() {
        // for JPA
    }

    public OAuthIdentity(User user, OAuthProvider provider, String providerUserId) {
        this.user = Objects.requireNonNull(user, "User must not be null");
        this.provider = Objects.requireNonNull(provider, "Provider must not be null");

        String pid = providerUserId == null ? null : providerUserId.trim();
        if (pid == null || pid.isBlank()) {
            throw new IllegalArgumentException("Provider user id must not be blank");
        }
        this.providerUserId = pid;
    }

    public UUID getOauthIdentityId() {
        return oauthIdentityId;
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public User getUser() {
        return user;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUser(User user) {
        Objects.requireNonNull(user, "OAuthIdentity user cannot be null");
        if (this.user != null && this.user != user) {
            throw new IllegalArgumentException("OAuthIdentity user cannot be changed once set");
        }
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OAuthIdentity other))
            return false;
        return oauthIdentityId != null && oauthIdentityId.equals(other.oauthIdentityId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
