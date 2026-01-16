package com.skillstorm.reserveone.controllers;

import java.time.LocalDate;

import com.skillstorm.reserveone.dto.PaymentTransactionListResponseDto;
import com.skillstorm.reserveone.services.PaymentTransactionService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment-transactions")
public class PaymentTransactionController {

    private final PaymentTransactionService service;

    public PaymentTransactionController(PaymentTransactionService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PaymentTransactionListResponseDto getAll(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return service.findAll(query, status, from, to, page, size, sort);
    }
}
