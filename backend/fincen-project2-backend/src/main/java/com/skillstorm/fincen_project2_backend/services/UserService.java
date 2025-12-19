package com.skillstorm.fincen_project2_backend.services;

import com.skillstorm.fincen_project2_backend.repositories.UserRepository;

public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

}
