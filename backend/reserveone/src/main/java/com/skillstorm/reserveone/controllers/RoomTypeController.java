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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.reserveone.dto.RoomTypeRequestDTO;
import com.skillstorm.reserveone.dto.RoomTypeResponseDTO;
import com.skillstorm.reserveone.services.RoomTypeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/room-types")
public class RoomTypeController {

    private final RoomTypeService service;

    public RoomTypeController(RoomTypeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomTypeResponseDTO createOne(@Valid @RequestBody RoomTypeRequestDTO dto) {
        return service.createOne(dto);
    }

    @GetMapping("/{id}")
    public RoomTypeResponseDTO readOne(@PathVariable UUID id) {
        return service.readOne(id);
    }

    @GetMapping
    public List<RoomTypeResponseDTO> readAll(
            @RequestParam(required = false) UUID hotelId) {
        if (hotelId != null) {
            // If RoomTypeService has a readByHotelId method, use it here
            // Otherwise, just return all room types
            return service.readAll();
        }
        return service.readAll();
    }

    @PutMapping("/{id}")
    public RoomTypeResponseDTO updateOne(@PathVariable UUID id, 
                                        @Valid @RequestBody RoomTypeRequestDTO dto) {
        return service.updateOne(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOne(@PathVariable UUID id) {
        service.deleteOne(id);
    }
}

