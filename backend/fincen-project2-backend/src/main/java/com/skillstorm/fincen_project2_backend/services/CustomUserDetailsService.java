package com.skillstorm.fincen_project2_backend.services;

import java.util.Objects;

import com.skillstorm.fincen_project2_backend.models.Role;
import com.skillstorm.fincen_project2_backend.models.User;
import com.skillstorm.fincen_project2_backend.models.User.Status;
import com.skillstorm.fincen_project2_backend.repositories.UserRepository;

import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(@NonNull UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = normalizeRequired(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedEmail));

        String userEmail = Objects.requireNonNull(user.getEmail(), "User email must not be null");

        String passwordHash = user.getPasswordHash();
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new UsernameNotFoundException("User has no password set: " + userEmail);
        }

        Status status = Objects.requireNonNull(user.getStatus(), "User status must not be null");
        boolean enabled = status == Status.ACTIVE;
        boolean accountLocked = status == Status.SUSPENDED;

        GrantedAuthority[] authorities = toAuthoritiesArray(user);

        return Objects.requireNonNull(
                org.springframework.security.core.userdetails.User
                        .withUsername(userEmail)
                        .password(passwordHash)
                        .authorities(authorities) // <-- uses varargs overload
                        .disabled(!enabled)
                        .accountLocked(accountLocked)
                        .build(),
                "UserDetails builder returned null");
    }

    @NonNull
    private static GrantedAuthority[] toAuthoritiesArray(@NonNull User user) {
        return Objects.requireNonNull(
                user.getRoles().stream()
                        .map(Role::getName)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(name -> !name.isBlank())
                        .map(SimpleGrantedAuthority::new)
                        .distinct()
                        .toArray(GrantedAuthority[]::new),
                "authorities array must not be null");
    }

    @NonNull
    private static String normalizeRequired(String value) {
        if (value == null) {
            throw new UsernameNotFoundException("Email is required.");
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new UsernameNotFoundException("Email is required.");
        }
        return trimmed;
    }
}
