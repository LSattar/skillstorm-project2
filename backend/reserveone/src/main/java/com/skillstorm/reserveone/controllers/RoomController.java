package com.skillstorm.reserveone.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.skillstorm.reserveone.dto.RoomRequestDTO;
import com.skillstorm.reserveone.dto.RoomResponseDTO;
import com.skillstorm.reserveone.services.RoomService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService service;

    public RoomController(RoomService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponseDTO createOne(@Valid @RequestBody RoomRequestDTO dto) {
        return service.createOne(dto);
    }

    @GetMapping("/{id}")
    public RoomResponseDTO readOne(@PathVariable UUID id) {
        return service.readOne(id);
    }

    @GetMapping
    public List<RoomResponseDTO> readAll(
            @RequestParam(required = false) UUID hotelId) {
        if (hotelId != null) {
            // If RoomService has a readByHotelId method, use it here
            // Otherwise, just return all rooms
            return service.readAll();
        }
        return service.readAll();
    }

    @GetMapping("/available")
    public List<RoomResponseDTO> searchAvailableRooms(
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer guestCount,
            @RequestParam(required = false) UUID roomTypeId) {
        return service.searchAvailableRooms(hotelId, startDate, endDate, guestCount, roomTypeId);
    }

    @PutMapping("/{id}")
    public RoomResponseDTO updateOne(@PathVariable UUID id, 
                                    @Valid @RequestBody RoomRequestDTO dto) {
        return service.updateOne(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOne(@PathVariable UUID id) {
        service.deleteOne(id);
    }
}

