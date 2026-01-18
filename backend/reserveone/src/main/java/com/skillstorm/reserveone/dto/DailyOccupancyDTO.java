package com.skillstorm.reserveone.dto;

import java.time.LocalDate;

public record DailyOccupancyDTO(
    LocalDate date,
    int occupiedRooms,
    int totalRooms,
    double occupancyRate,
    int checkIns,
    int checkOuts
) {
}
