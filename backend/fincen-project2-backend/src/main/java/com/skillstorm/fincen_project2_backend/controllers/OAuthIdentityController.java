package com.skillstorm.fincen_project2_backend.controllers;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.fincen_project2_backend.dto.oauthidentities.CreateOAuthIdentityRequest;
import com.skillstorm.fincen_project2_backend.dto.oauthidentities.OAuthIdentityResponse;
import com.skillstorm.fincen_project2_backend.models.User;
import com.skillstorm.fincen_project2_backend.services.OAuthIdentityService;
import com.skillstorm.fincen_project2_backend.services.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/oauthidentities")
@Validated
public class OAuthIdentityController {

    private final OAuthIdentityService oauthIdentityService;
    private final UserService userService;

    public OAuthIdentityController(@NonNull OAuthIdentityService oauthIdentityService,
            @NonNull UserService userService) {

        this.oauthIdentityService = Objects.requireNonNull(oauthIdentityService, "oauthIdentityService cannot be null");
        this.userService = Objects.requireNonNull(userService, "userService cannot be null");
    }

    @PostMapping
    public ResponseEntity<OAuthIdentityResponse> create(@RequestParam @NonNull UUID userId,
            @Valid @RequestBody @NonNull CreateOAuthIdentityRequest req) {
        User user = userService.getEntityById(userId);
        OAuthIdentityResponse created = oauthIdentityService.create(user, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
