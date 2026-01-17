package com.skillstorm.reserveone.controllers;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.reserveone.dto.OccupancyReportDTO;
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

    @GetMapping("/occupancy-report")
    public OccupancyReportDTO getOccupancyReport(
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // Default to last 30 days if not specified
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        return service.getOccupancyReport(hotelId, startDate, endDate);
    }

    @GetMapping("/cancellations-past-week")
    public int getCancellationsInPastWeek(
            @RequestParam(required = false) UUID hotelId) {
        return service.getCancellationsInPastWeek(hotelId);
    }
}
