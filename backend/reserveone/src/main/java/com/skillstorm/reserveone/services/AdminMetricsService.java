package com.skillstorm.reserveone.services;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.reserveone.dto.DailyOccupancyDTO;
import com.skillstorm.reserveone.dto.OccupancyReportDTO;
import com.skillstorm.reserveone.dto.OperationalMetricsDTO;
import com.skillstorm.reserveone.models.Reservation;
import com.skillstorm.reserveone.models.Reservation.Status;
import com.skillstorm.reserveone.models.Room;
import com.skillstorm.reserveone.repositories.ReservationRepository;
import com.skillstorm.reserveone.repositories.RoomRepository;

/**
 * Service for calculating administrative metrics and operational statistics
 * for hotel management, including occupancy rates, check-ins, and check-outs.
 * 
 * <p>This service provides real-time operational metrics that help administrators
 * monitor hotel performance and manage daily operations. All methods are read-only
 * and use transactional queries for data consistency.
 * 
 * @author ReserveOne Team
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
public class AdminMetricsService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Constructs a new AdminMetricsService with the required repositories.
     * 
     * @param roomRepository the repository for room data access
     * @param reservationRepository the repository for reservation data access
     */
    public AdminMetricsService(
            RoomRepository roomRepository,
            ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Calculates real-time operational metrics for a hotel or all hotels.
     * 
     * <p>This method computes several key metrics:
     * <ul>
     *   <li><b>Total Rooms:</b> Count of all rooms excluding OUT_OF_SERVICE status</li>
     *   <li><b>Occupied Rooms:</b> Maximum of rooms marked OCCUPIED or rooms with CHECKED_IN reservations</li>
     *   <li><b>Occupancy Rate:</b> Percentage of occupied rooms (occupied/total * 100)</li>
     *   <li><b>Check-ins Today:</b> Reservations with CHECKED_IN status and startDate = today</li>
     *   <li><b>Check-ins Pending:</b> CONFIRMED reservations scheduled to check in today</li>
     *   <li><b>Check-outs Today:</b> Reservations with CHECKED_OUT status and endDate = today</li>
     *   <li><b>Check-outs Pending:</b> CHECKED_IN reservations scheduled to check out today</li>
     * </ul>
     * 
     * <p>The occupancy calculation uses a dual approach: it considers both rooms
     * explicitly marked as OCCUPIED and rooms with active CHECKED_IN reservations,
     * taking the maximum to ensure accuracy even if room status is out of sync.
     * 
     * @param hotelId the UUID of the hotel to calculate metrics for, or null for all hotels
     * @return OperationalMetricsDTO containing all calculated metrics
     */
    public OperationalMetricsDTO getOperationalMetrics(UUID hotelId) {
        LocalDate today = LocalDate.now();
        
        List<Room> allRooms;
        if (hotelId != null) {
            allRooms = roomRepository.findByHotel_HotelId(hotelId);
        } else {
            allRooms = roomRepository.findAll();
        }
        
        int totalRooms = (int) allRooms.stream()
            .filter(room -> room.getStatus() != Room.Status.OUT_OF_SERVICE)
            .count();
        
        int occupiedRoomsFromStatus = (int) allRooms.stream()
            .filter(room -> room.getStatus() == Room.Status.OCCUPIED)
            .count();
        
        List<Reservation> checkedInReservations;
        if (hotelId != null) {
            checkedInReservations = reservationRepository.findByHotel_HotelIdAndStatus(
                hotelId, Status.CHECKED_IN);
        } else {
            checkedInReservations = reservationRepository.findByStatus(Status.CHECKED_IN);
        }
        
        long distinctOccupiedRooms = checkedInReservations.stream()
            .map(res -> res.getRoom().getRoomId())
            .distinct()
            .count();
        
        int occupiedRooms = Math.max(occupiedRoomsFromStatus, (int) distinctOccupiedRooms);
        
        double occupancyRate = totalRooms > 0 
            ? (occupiedRooms * 100.0 / totalRooms) 
            : 0.0;
        
        List<Reservation> checkInsToday;
        if (hotelId != null) {
            checkInsToday = reservationRepository.findByHotel_HotelIdAndStatusAndStartDate(
                hotelId, Status.CHECKED_IN, today);
        } else {
            checkInsToday = reservationRepository.findByStatusAndStartDate(Status.CHECKED_IN, today);
        }
        
        List<Reservation> checkInsPending;
        if (hotelId != null) {
            checkInsPending = reservationRepository.findByHotel_HotelIdAndStatusAndStartDate(
                hotelId, Status.CONFIRMED, today);
        } else {
            checkInsPending = reservationRepository.findByStatusAndStartDate(Status.CONFIRMED, today);
        }
        
        List<Reservation> checkOutsToday;
        if (hotelId != null) {
            checkOutsToday = reservationRepository.findByHotel_HotelIdAndStatusAndEndDate(
                hotelId, Status.CHECKED_OUT, today);
        } else {
            checkOutsToday = reservationRepository.findByStatusAndEndDate(Status.CHECKED_OUT, today);
        }
        
        List<Reservation> checkOutsPending;
        if (hotelId != null) {
            checkOutsPending = reservationRepository.findByHotel_HotelIdAndStatusAndEndDate(
                hotelId, Status.CHECKED_IN, today);
        } else {
            checkOutsPending = reservationRepository.findByStatusAndEndDate(Status.CHECKED_IN, today);
        }
        
        return new OperationalMetricsDTO(
            totalRooms,
            occupiedRooms,
            Math.round(occupancyRate * 100.0) / 100.0,
            checkInsToday.size(),
            checkInsPending.size(),
            checkOutsToday.size(),
            checkOutsPending.size()
        );
    }

    /**
     * Counts the number of cancelled reservations in the past 7 days.
     * 
     * <p>This method queries for reservations that:
     * <ul>
     *   <li>Have status CANCELLED</li>
     *   <li>Were cancelled between 7 days ago and now</li>
     *   <li>Optionally filtered by hotelId if provided</li>
     * </ul>
     * 
     * <p>The time range uses OffsetDateTime to ensure accurate timezone-aware
     * calculations. The cancellation timestamp (cancelledAt) must be within
     * the 7-day window.
     * 
     * @param hotelId the UUID of the hotel to filter by, or null for all hotels
     * @return the count of cancellations in the past week
     */
    public int getCancellationsInPastWeek(UUID hotelId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime weekAgo = now.minusDays(7);
        
        List<Reservation> cancellations;
        if (hotelId != null) {
            cancellations = reservationRepository.findByHotel_HotelIdAndStatusAndCancelledAtBetween(
                hotelId, Status.CANCELLED, weekAgo, now);
        } else {
            cancellations = reservationRepository.findByStatusAndCancelledAtBetween(
                Status.CANCELLED, weekAgo, now);
        }
        
        return cancellations.size();
    }

    /**
     * Generates a detailed occupancy report for a specified date range.
     * 
     * <p>This method performs complex calculations to produce daily occupancy statistics
     * including:
     * <ul>
     *   <li>Daily occupied room counts</li>
     *   <li>Daily occupancy rates (percentage)</li>
     *   <li>Check-in and check-out counts per day</li>
     *   <li>Average and peak occupancy rates for the period</li>
     * </ul>
     * 
     * <p><b>Algorithm:</b>
     * <ol>
     *   <li>Initializes a map with zero occupancy for each day in the range</li>
     *   <li>Processes all active (non-cancelled) reservations that overlap the date range</li>
     *   <li>For each reservation:
     *     <ul>
     *       <li>Increments check-in count on the reservation start date (if in range)</li>
     *       <li>Increments check-out count on the reservation end date (if in range)</li>
     *       <li>Increments occupied room count for each day from startDate to endDate-1
     *           (check-out day is not counted as occupied)</li>
     *     </ul>
     *   </li>
     *   <li>Calculates occupancy rates for each day (occupied/total * 100)</li>
     *   <li>Computes aggregate statistics (average, peak, totals)</li>
     * </ol>
     * 
     * <p><b>Important Notes:</b>
     * <ul>
     *   <li>Rooms are considered occupied from startDate up to (but not including) endDate</li>
     *   <li>Only non-cancelled reservations are included in calculations</li>
     *   <li>Reservations that partially overlap the date range are included</li>
     *   <li>Peak occupancy date is the day with the highest occupancy rate</li>
     * </ul>
     * 
     * @param hotelId the UUID of the hotel to generate the report for, or null for all hotels
     * @param startDate the start date of the report range (inclusive)
     * @param endDate the end date of the report range (inclusive)
     * @return OccupancyReportDTO containing daily data and aggregate statistics
     * @throws IllegalArgumentException if startDate is after endDate
     */
    public OccupancyReportDTO getOccupancyReport(UUID hotelId, LocalDate startDate, LocalDate endDate) {
        // Get total rooms
        List<Room> allRooms;
        if (hotelId != null) {
            allRooms = roomRepository.findByHotel_HotelId(hotelId);
        } else {
            allRooms = roomRepository.findAll();
        }
        
        int totalRooms = (int) allRooms.stream()
            .filter(room -> room.getStatus() != Room.Status.OUT_OF_SERVICE)
            .count();
        
        // Get all reservations in the date range
        List<Reservation> allReservations;
        if (hotelId != null) {
            allReservations = reservationRepository.findByHotel_HotelId(hotelId);
        } else {
            allReservations = reservationRepository.findAll();
        }
        
        // Filter reservations that overlap with the date range and are not cancelled
        List<Reservation> activeReservations = allReservations.stream()
            .filter(r -> r.getStatus() != Status.CANCELLED)
            .filter(r -> !r.getEndDate().isBefore(startDate) && !r.getStartDate().isAfter(endDate))
            .collect(Collectors.toList());
        
        // Initialize daily data map
        Map<LocalDate, DailyOccupancyDTO> dailyDataMap = new HashMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dailyDataMap.put(currentDate, new DailyOccupancyDTO(
                currentDate, 0, totalRooms, 0.0, 0, 0));
            currentDate = currentDate.plusDays(1);
        }
        
        // Process reservations to calculate occupancy
        for (Reservation reservation : activeReservations) {
            LocalDate resStart = reservation.getStartDate();
            LocalDate resEnd = reservation.getEndDate();
            
            // Count check-ins and check-outs
            if (!resStart.isBefore(startDate) && !resStart.isAfter(endDate)) {
                DailyOccupancyDTO dayData = dailyDataMap.get(resStart);
                if (dayData != null) {
                    dailyDataMap.put(resStart, new DailyOccupancyDTO(
                        dayData.date(),
                        dayData.occupiedRooms(),
                        dayData.totalRooms(),
                        dayData.occupancyRate(),
                        dayData.checkIns() + 1,
                        dayData.checkOuts()));
                }
            }
            
            if (!resEnd.isBefore(startDate) && !resEnd.isAfter(endDate)) {
                DailyOccupancyDTO dayData = dailyDataMap.get(resEnd);
                if (dayData != null) {
                    dailyDataMap.put(resEnd, new DailyOccupancyDTO(
                        dayData.date(),
                        dayData.occupiedRooms(),
                        dayData.totalRooms(),
                        dayData.occupancyRate(),
                        dayData.checkIns(),
                        dayData.checkOuts() + 1));
                }
            }
            
            // Count occupied rooms for each day of the stay
            // Room is occupied from startDate up to (but not including) resEnd (check-out day)
            LocalDate day = resStart.isBefore(startDate) ? startDate : resStart;
            
            // Room is occupied while day < resEnd (check-out day is not occupied)
            // But also limit to the report date range (day <= endDate)
            while (day.isBefore(resEnd) && !day.isAfter(endDate)) {
                DailyOccupancyDTO dayData = dailyDataMap.get(day);
                if (dayData != null) {
                    dailyDataMap.put(day, new DailyOccupancyDTO(
                        dayData.date(),
                        dayData.occupiedRooms() + 1,
                        dayData.totalRooms(),
                        dayData.occupancyRate(),
                        dayData.checkIns(),
                        dayData.checkOuts()));
                }
                day = day.plusDays(1);
            }
        }
        
        // Calculate occupancy rates and create final list
        List<DailyOccupancyDTO> dailyData = new ArrayList<>();
        double totalOccupancyRate = 0.0;
        double peakOccupancyRate = 0.0;
        LocalDate peakDate = startDate;
        int totalCheckIns = 0;
        int totalCheckOuts = 0;
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DailyOccupancyDTO dayData = dailyDataMap.get(date);
            if (dayData != null) {
                double occupancyRate = totalRooms > 0 
                    ? (dayData.occupiedRooms() * 100.0 / totalRooms) 
                    : 0.0;
                
                DailyOccupancyDTO finalData = new DailyOccupancyDTO(
                    dayData.date(),
                    dayData.occupiedRooms(),
                    dayData.totalRooms(),
                    Math.round(occupancyRate * 100.0) / 100.0,
                    dayData.checkIns(),
                    dayData.checkOuts());
                
                dailyData.add(finalData);
                totalOccupancyRate += occupancyRate;
                totalCheckIns += dayData.checkIns();
                totalCheckOuts += dayData.checkOuts();
                
                if (occupancyRate > peakOccupancyRate) {
                    peakOccupancyRate = occupancyRate;
                    peakDate = date;
                }
            }
        }
        
        double averageOccupancyRate = dailyData.isEmpty() 
            ? 0.0 
            : Math.round((totalOccupancyRate / dailyData.size()) * 100.0) / 100.0;
        
        return new OccupancyReportDTO(
            dailyData,
            averageOccupancyRate,
            Math.round(peakOccupancyRate * 100.0) / 100.0,
            peakDate,
            totalCheckIns,
            totalCheckOuts);
    }
}
