package com.skillstorm.reserveone.dto;

import java.time.LocalDate;
import java.util.List;

public record OccupancyReportDTO(
    List<DailyOccupancyDTO> dailyData,
    double averageOccupancyRate,
    double peakOccupancyRate,
    LocalDate peakOccupancyDate,
    int totalCheckIns,
    int totalCheckOuts
) {
}
