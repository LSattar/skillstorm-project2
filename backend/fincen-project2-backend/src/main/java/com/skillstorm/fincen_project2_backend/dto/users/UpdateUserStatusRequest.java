package com.skillstorm.fincen_project2_backend.dto.users;

import com.skillstorm.fincen_project2_backend.models.User.Status;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull Status status) {

}
