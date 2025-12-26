// enum for OAuth provider (compile-time safety)
package com.skillstorm.reserveone.models;

import java.util.Locale;
import java.util.Objects;

import org.springframework.lang.NonNull;

public enum OAuthProvider {
    GOOGLE("google"),
    FACEBOOK("facebook"),
    APPLE("apple"),
    GITHUB("github"),
    MICROSOFT("microsoft"),
    OKTA("okta");

    private final String dbValue;

    OAuthProvider(@NonNull String dbValue) {
        this.dbValue = Objects.requireNonNull(dbValue);
    }

    public String dbValue() {
        return dbValue;
    }

    public static OAuthProvider fromDbValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("provider must not be null");
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        for (OAuthProvider p : values()) {
            if (p.dbValue.equals(v)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unsupported provider: " + value);
    }
}
