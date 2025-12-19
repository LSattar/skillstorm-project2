package com.skillstorm.fincen_project2_backend.services;

import com.skillstorm.fincen_project2_backend.repositories.OAuthIdentityRepository;

public class OAuthIdentityService {

    private final OAuthIdentityRepository repo;

    public OAuthIdentityService(OAuthIdentityRepository repo) {
        this.repo = repo;
    }

}
