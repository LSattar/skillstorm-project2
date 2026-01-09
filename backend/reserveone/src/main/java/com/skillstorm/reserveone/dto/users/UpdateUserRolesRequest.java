package com.skillstorm.reserveone.dto.users;

import java.util.Set;

import org.springframework.lang.Nullable;

public record UpdateUserRolesRequest(
        @Nullable Set<String> add,
        @Nullable Set<String> remove) {
}
