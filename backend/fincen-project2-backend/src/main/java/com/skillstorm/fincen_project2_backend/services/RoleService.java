package com.skillstorm.fincen_project2_backend.services;

import com.skillstorm.fincen_project2_backend.repositories.RoleRepository;

public class RoleService {

    private final RoleRepository repo;

    public RoleService(RoleRepository repo) {
        this.repo = repo;
    }

}
