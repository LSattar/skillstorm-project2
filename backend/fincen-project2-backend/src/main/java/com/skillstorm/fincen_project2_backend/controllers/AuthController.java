package com.skillstorm.fincen_project2_backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.fincen_project2_backend.dto.users.UserDto;
import com.skillstorm.fincen_project2_backend.models.User;
import com.skillstorm.fincen_project2_backend.repositories.UserRepository;

//this class contains a single method for registering a new user

@RestController
@RequestMapping("/auth")
public class AuthController {

    private UserRepository repo;
    private PasswordEncoder encoder;

    public AuthController(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    // this method takes in a user's username and password via a body object
    // then encrypts the password and stores the user in the database
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserDto dto) {
        User user = new User(dto.username(), encoder.encode(dto.password()), dto.role());
        this.repo.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered");
    }

}
