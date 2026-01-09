package com.skillstorm.reserveone.controllers;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.reserveone.dto.OperationalMetricsDTO;
import com.skillstorm.reserveone.services.AdminMetricsService;

@RestController
@RequestMapping("/admin/metrics")
public class AdminMetricsController {

    private final AdminMetricsService service;

    public AdminMetricsController(AdminMetricsService service) {
        this.service = service;
    }

    @GetMapping
    public OperationalMetricsDTO getOperationalMetrics(
            @RequestParam(required = false) UUID hotelId) {
        return service.getOperationalMetrics(hotelId);
    }
}
