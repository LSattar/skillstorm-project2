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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.fincen_project2_backend.dtos.ReservationHoldRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.ReservationHoldResponseDTO;
import com.skillstorm.fincen_project2_backend.services.ReservationHoldService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/reservation-holds")
public class ReservationHoldController {

    private final ReservationHoldService service;

    public ReservationHoldController(ReservationHoldService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationHoldResponseDTO createOne(@Valid @RequestBody ReservationHoldRequestDTO dto) {
        return service.createOne(dto);
    }

    @GetMapping("/{id}")
    public ReservationHoldResponseDTO readOne(@PathVariable UUID id) {
        return service.readOne(id);
    }

    @GetMapping
    public List<ReservationHoldResponseDTO> readAll(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID roomId) {
        if (userId != null) {
            return service.readByUserId(userId);
        }
        if (roomId != null) {
            return service.readByRoomId(roomId);
        }
        return service.readAll();
    }

    @PutMapping("/{id}")
    public ReservationHoldResponseDTO updateOne(@PathVariable UUID id,
                                               @Valid @RequestBody ReservationHoldRequestDTO dto) {
        return service.updateOne(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOne(@PathVariable UUID id) {
        service.deleteOne(id);
    }

    @PostMapping("/{id}/cancel")
    public ReservationHoldResponseDTO cancelHold(@PathVariable UUID id) {
        return service.cancelHold(id);
    }

    @PostMapping("/expire")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void expireHolds() {
        service.expireHolds();
    }
}

