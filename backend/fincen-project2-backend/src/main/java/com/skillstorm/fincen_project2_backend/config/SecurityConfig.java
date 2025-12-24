package com.skillstorm.fincen_project2_backend.config;

import java.util.Collection;

import com.skillstorm.fincen_project2_backend.models.User;
import com.skillstorm.fincen_project2_backend.repositories.UserRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthConverter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()

                        .requestMatchers(HttpMethod.GET, "/rooms/available").permitAll()
                        .requestMatchers(HttpMethod.GET, "/rooms/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/rooms/hotel/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/hotels").permitAll()
                        .requestMatchers(HttpMethod.GET, "/hotels/*").permitAll()

                        // Users
                        .requestMatchers(HttpMethod.GET, "/users/*")
                        .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/users/*")
                        .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/users/*")
                        .hasAnyRole("ADMIN", "BUSINESS_OWNER")
                        .requestMatchers(HttpMethod.PATCH, "/users/*/status")
                        .hasRole("ADMIN")

                        // Bookings
                        .requestMatchers(HttpMethod.POST, "/bookings")
                        .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/bookings/*")
                        .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/bookings/user/*")
                        .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/bookings/*")
                        .hasAnyRole("EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/bookings/*")
                        .hasAnyRole("MANAGER", "BUSINESS_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/bookings")
                        .hasAnyRole("EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")

                        // Rooms
                        .requestMatchers(HttpMethod.POST, "/rooms")
                        .hasAnyRole("MANAGER", "BUSINESS_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/rooms/*")
                        .hasAnyRole("MANAGER", "BUSINESS_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/rooms/*")
                        .hasAnyRole("BUSINESS_OWNER", "ADMIN")

                        // Hotels
                        .requestMatchers(HttpMethod.POST, "/hotels")
                        .hasAnyRole("BUSINESS_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/hotels/*")
                        .hasAnyRole("BUSINESS_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/hotels/*")
                        .hasRole("ADMIN")

                        // OAuth Identities
                        .requestMatchers(HttpMethod.POST, "/oauthidentities")
                        .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/oauthidentities")
                        .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/oauthidentities/**")
                        .hasAnyRole("GUEST", "ADMIN")

                        // Admin-only areas
                        .requestMatchers("/roles/**").hasRole("ADMIN")

                        // Reports & analytics
                        .requestMatchers("/reports/**")
                        .hasAnyRole("MANAGER", "BUSINESS_OWNER", "ADMIN")
                        .requestMatchers("/analytics/**")
                        .hasAnyRole("BUSINESS_OWNER", "ADMIN")

                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)));

        return http.build();
    }

    /**
     * Convert Auth0 JWT -> Spring Authorities using DB roles.
     * Requires you to store Auth0 "sub" in your User table (recommended).
     */
    @Bean
    JwtAuthenticationConverter jwtAuthConverter(UserRepository userRepository) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String sub = jwt.getSubject();

            User user = userRepository.findByAuth0Sub(sub)
                    .orElseThrow(() -> new BadCredentialsException("User not registered in local DB"));

            Collection<GrantedAuthority> authorities = user.getRoles().stream()
                    .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.getName()))
                    .toList();

            return authorities;
        });
        return converter;
    }
}
