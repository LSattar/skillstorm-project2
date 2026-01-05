package com.skillstorm.reserveone.controllers;

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

import com.skillstorm.reserveone.dto.HotelRequestDTO;
import com.skillstorm.reserveone.dto.HotelResponseDTO;
import com.skillstorm.reserveone.services.HotelService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/hotels")
public class HotelController {

    private final HotelService service;

    public HotelController(HotelService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HotelResponseDTO createOne(@Valid @RequestBody HotelRequestDTO dto) {
        return service.createOne(dto);
    }

    @GetMapping("/{id}")
    public HotelResponseDTO readOne(@PathVariable UUID id) {
        return service.readOne(id);
    }

    @GetMapping
    public List<HotelResponseDTO> readAll() {
        return service.readAll();
    }

    @PutMapping("/{id}")
    public HotelResponseDTO updateOne(@PathVariable UUID id, 
                                     @Valid @RequestBody HotelRequestDTO dto) {
        return service.updateOne(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOne(@PathVariable UUID id) {
        service.deleteOne(id);
    }
}

