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

import com.skillstorm.reserveone.dto.RoomTypeAmenityRequestDTO;
import com.skillstorm.reserveone.dto.RoomTypeAmenityResponseDTO;
import com.skillstorm.reserveone.services.RoomTypeAmenityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/room-type-amenities")
public class RoomTypeAmenityController {

    private final RoomTypeAmenityService service;

    public RoomTypeAmenityController(RoomTypeAmenityService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomTypeAmenityResponseDTO createOne(@Valid @RequestBody RoomTypeAmenityRequestDTO dto) {
        return service.createOne(dto);
    }

    @GetMapping("/{roomTypeId}/{amenityId}")
    public RoomTypeAmenityResponseDTO readOne(
            @PathVariable UUID roomTypeId,
            @PathVariable UUID amenityId) {
        return service.readOne(roomTypeId, amenityId);
    }

    @GetMapping
    public List<RoomTypeAmenityResponseDTO> readAll(
            @RequestParam(required = false) UUID roomTypeId,
            @RequestParam(required = false) UUID amenityId) {
        // Filter by roomTypeId if provided
        if (roomTypeId != null) {
            return service.readByRoomTypeId(roomTypeId);
        }
        // Otherwise return all (or filter by amenityId if needed in the future)
        return service.readAll();
    }

    @PutMapping("/{roomTypeId}/{amenityId}")
    public RoomTypeAmenityResponseDTO updateOne(
            @PathVariable UUID roomTypeId,
            @PathVariable UUID amenityId,
            @Valid @RequestBody RoomTypeAmenityRequestDTO dto) {
        return service.updateOne(roomTypeId, amenityId, dto);
    }

    @DeleteMapping("/{roomTypeId}/{amenityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOne(
            @PathVariable UUID roomTypeId,
            @PathVariable UUID amenityId) {
        service.deleteOne(roomTypeId, amenityId);
    }
}

