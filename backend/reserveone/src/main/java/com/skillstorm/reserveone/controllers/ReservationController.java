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

import com.skillstorm.reserveone.dto.ReservationRequestDTO;
import com.skillstorm.reserveone.dto.ReservationResponseDTO;
import com.skillstorm.reserveone.services.ReservationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponseDTO createOne(@Valid @RequestBody ReservationRequestDTO dto) {
        return service.createOne(dto);
    }

    @GetMapping("/{id}")
    public ReservationResponseDTO readOne(@PathVariable UUID id) {
        return service.readOne(id);
    }

    @GetMapping
    public List<ReservationResponseDTO> readAll(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(required = false) UUID roomId) {
        if (userId != null) {
            return service.readByUserId(userId);
        }
        if (hotelId != null) {
            return service.readByHotelId(hotelId);
        }
        if (roomId != null) {
            return service.readByRoomId(roomId);
        }
        return service.readAll();
    }

    @PutMapping("/{id}")
    public ReservationResponseDTO updateOne(@PathVariable UUID id, 
                                           @Valid @RequestBody ReservationRequestDTO dto) {
        return service.updateOne(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOne(@PathVariable UUID id) {
        service.deleteOne(id);
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponseDTO cancelReservation(@PathVariable UUID id,
                                                    @RequestParam(required = false) String reason) {
        return service.cancelReservation(id, reason);
    }
}

