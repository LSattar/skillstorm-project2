package com.skillstorm.reserveone.dto;

public record OperationalMetricsDTO(
    int totalRooms,
    int occupiedRooms,
    double occupancyRate,
    int checkInsToday,
    int checkInsPending,
    int checkOutsToday,
    int checkOutsPending
) {
}
