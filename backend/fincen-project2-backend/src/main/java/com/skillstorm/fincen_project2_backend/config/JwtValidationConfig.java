package com.skillstorm.fincen_project2_backend.config;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtValidationConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${app.auth0.audience}") String audience) {

        final String expectedAudience = Objects.requireNonNull(audience, "Audience must not be null").trim();
        final String expectedIssuer = Objects.requireNonNull(issuer, "Issuer must not be null").trim();

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withIssuerLocation(expectedIssuer)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(expectedIssuer);

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> tokenAudiences = jwt.getAudience();
            return tokenAudiences != null && tokenAudiences.contains(expectedAudience)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error(
                                    "invalid_token",
                                    "Token audience does not match configured audience",
                                    null));
        };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));

        return decoder;
    }
}
