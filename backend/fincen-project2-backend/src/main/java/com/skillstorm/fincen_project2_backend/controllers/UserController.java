package com.skillstorm.fincen_project2_backend.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")

public class UserController {
    @GetMapping
    public String testUserEndPoint() {
        return "Hello World";
    }

}
