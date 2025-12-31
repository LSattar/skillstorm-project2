package com.skillstorm.fincen_project2_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.fincen_project2_backend.dtos.AmenityRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.AmenityResponseDTO;
import com.skillstorm.fincen_project2_backend.services.AmenityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/amenities")
public class AmenityController {

    private final AmenityService service;

    public AmenityController(AmenityService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AmenityResponseDTO createOne(@Valid @RequestBody AmenityRequestDTO dto) {
        return service.createOne(dto);
    }

    @GetMapping("/{id}")
    public AmenityResponseDTO readOne(@PathVariable UUID id) {
        return service.readOne(id);
    }

    @GetMapping
    public List<AmenityResponseDTO> readAll() {
        return service.readAll();
    }

    @PutMapping("/{id}")
    public AmenityResponseDTO updateOne(@PathVariable UUID id, 
                                      @Valid @RequestBody AmenityRequestDTO dto) {
        return service.updateOne(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOne(@PathVariable UUID id) {
        service.deleteOne(id);
    }
}

