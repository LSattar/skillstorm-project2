package com.skillstorm.reserveone.mappers;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.skillstorm.reserveone.dto.users.CreateUserRequest;
import com.skillstorm.reserveone.dto.users.UpdateUserRequest;
import com.skillstorm.reserveone.dto.users.UpdateUserStatusRequest;
import com.skillstorm.reserveone.dto.users.UserResponse;
import com.skillstorm.reserveone.models.Role;
import com.skillstorm.reserveone.models.User;

@Component
public class UserMapper {

    @NonNull
    public User toEntity(@NonNull CreateUserRequest req) {
        Objects.requireNonNull(req, "req must not be null");

        User user = new User(req.firstName(), req.lastName(), req.email());

        user.setPhone(req.phone());
        user.setAddress1(req.address1());
        user.setAddress2(req.address2());
        user.setCity(req.city());
        user.setState(req.state());
        user.setZip(req.zip());

        return user;
    }

    @NonNull
    public UserResponse toResponse(@NonNull User user) {
        Objects.requireNonNull(user, "user must not be null");

        return new UserResponse(
                Objects.requireNonNull(user.getUserId(), "userId must not be null"),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress1(),
                user.getAddress2(),
                user.getCity(),
                user.getState(),
                user.getZip(),
                Objects.requireNonNull(user.getStatus(), "status must not be null"),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                toRoleNames(user.getRoles()));
    }

    public void applyUpdate(@NonNull UpdateUserStatusRequest req, @NonNull User user) {
        Objects.requireNonNull(req, "req must not be null");
        Objects.requireNonNull(user, "user must not be null");
        user.setStatus(req.status());
    }

    public void applyUpdate(@Nullable UpdateUserRequest req, @NonNull User user) {
        Objects.requireNonNull(user, "user must not be null");
        if (req == null) {
            return;
        }

        if (req.firstName() != null)
            user.setFirstName(req.firstName());
        if (req.lastName() != null)
            user.setLastName(req.lastName());
        if (req.phone() != null)
            user.setPhone(req.phone());
        if (req.address1() != null)
            user.setAddress1(req.address1());
        if (req.address2() != null)
            user.setAddress2(req.address2());
        if (req.city() != null)
            user.setCity(req.city());
        if (req.state() != null)
            user.setState(req.state());
        if (req.zip() != null)
            user.setZip(req.zip());
    }

    private static Set<String> toRoleNames(@Nullable Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
