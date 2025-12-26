package com.skillstorm.reserveone.dto.users;

import com.skillstorm.reserveone.models.User.Status;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull Status status) {

}
