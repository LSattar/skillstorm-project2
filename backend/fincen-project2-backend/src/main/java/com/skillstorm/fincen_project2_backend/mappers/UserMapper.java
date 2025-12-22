package com.skillstorm.fincen_project2_backend.mappers;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.skillstorm.fincen_project2_backend.dto.users.CreateUserRequest;
import com.skillstorm.fincen_project2_backend.dto.users.UpdateUserRequest;
import com.skillstorm.fincen_project2_backend.dto.users.UpdateUserStatusRequest;
import com.skillstorm.fincen_project2_backend.dto.users.UserResponse;
import com.skillstorm.fincen_project2_backend.models.Role;
import com.skillstorm.fincen_project2_backend.models.User;

/*
 * PATCH behavior:
 * - null values are ignored (field not included in request)
 * - blank strings are ignored to avoid unintentionally clearing persisted data
 * Clients must send a non-blank value to update a field.
 */

@Component
public class UserMapper {

    // CREATE
    public User toEntity(CreateUserRequest req) {
        if (req == null) {
            return null;
        }

        User user = new User(req.firstName(), req.lastName(), req.email());

        if (req.phone() != null && !req.phone().isBlank()) {
            user.setPhone(req.phone().trim());
        }

        if (req.address1() != null && !req.address1().isBlank()) {
            user.setAddress1(req.address1().trim());
        }

        if (req.address2() != null && !req.address2().isBlank()) {
            user.setAddress2(req.address2().trim());
        }

        if (req.city() != null && !req.city().isBlank()) {
            user.setCity(req.city().trim());
        }

        if (req.state() != null && !req.state().isBlank()) {
            user.setState(req.state().trim());
        }

        if (req.zip() != null && !req.zip().isBlank()) {
            user.setZip(req.zip().trim());
        }

        return user;
    }

    // READ
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return new UserResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress1(),
                user.getAddress2(),
                user.getCity(),
                user.getState(),
                user.getZip(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                toRoleNames(user.getRoles()));
    }

    // UPDATE STATUS
    public void applyUpdate(UpdateUserStatusRequest req, User user) {
        if (req == null || user == null) {
            return;
        }

        user.setStatus(req.status());
    }

    private Set<String> toRoleNames(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }

        return roles.stream()
                .map(Role::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    // UPDATE PROFILE
    public void applyUpdate(UpdateUserRequest req, User user) {
        if (req == null || user == null) {
            return;
        }

        if (req.firstName() != null && !req.firstName().isBlank()) {
            user.setFirstName(req.firstName().trim());
        }

        if (req.lastName() != null && !req.lastName().isBlank()) {
            user.setLastName(req.lastName().trim());
        }

        if (req.phone() != null && !req.phone().isBlank()) {
            user.setPhone(req.phone().trim());
        }

        if (req.address1() != null && !req.address1().isBlank()) {
            user.setAddress1(req.address1().trim());
        }

        if (req.address2() != null && !req.address2().isBlank()) {
            user.setAddress2(req.address2().trim());
        }

        if (req.city() != null && !req.city().isBlank()) {
            user.setCity(req.city().trim());
        }

        if (req.state() != null && !req.state().isBlank()) {
            user.setState(req.state().trim());
        }

        if (req.zip() != null && !req.zip().isBlank()) {
            user.setZip(req.zip().trim());
        }
    }
}
