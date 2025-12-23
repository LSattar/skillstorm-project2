package com.skillstorm.fincen_project2_backend.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.fincen_project2_backend.dtos.HotelRequestDTO;
import com.skillstorm.fincen_project2_backend.dtos.HotelResponseDTO;
import com.skillstorm.fincen_project2_backend.models.Hotel;

@Component
public class HotelMapper {

    // CREATE
    public Hotel toEntity(HotelRequestDTO req) {
        if (req == null) {
            return null;
        }

        Hotel hotel = new Hotel(
                req.name().trim(),
                req.phone().trim(),
                req.address1().trim(),
                req.city().trim(),
                req.state().trim(),
                req.zip().trim());

        if (req.address2() != null && !req.address2().isBlank()) {
            hotel.setAddress2(req.address2().trim());
        }

        if (req.timezone() != null && !req.timezone().isBlank()) {
            hotel.setTimezone(req.timezone().trim());
        }

        return hotel;
    }

    // READ
    public HotelResponseDTO toResponse(Hotel hotel) {
        if (hotel == null) {
            return null;
        }

        return new HotelResponseDTO(
                hotel.getHotelId(),
                hotel.getName(),
                hotel.getPhone(),
                hotel.getAddress1(),
                hotel.getAddress2(),
                hotel.getCity(),
                hotel.getState(),
                hotel.getZip(),
                hotel.getTimezone(),
                hotel.getCreatedAt(),
                hotel.getUpdatedAt());
    }

    // UPDATE
    public void applyUpdate(HotelRequestDTO req, Hotel hotel) {
        if (req == null || hotel == null) {
            return;
        }

        hotel.setName(req.name().trim());
        hotel.setPhone(req.phone().trim());
        hotel.setAddress1(req.address1().trim());

        if (req.address2() != null) {
            hotel.setAddress2(req.address2().trim());
        }

        hotel.setCity(req.city().trim());
        hotel.setState(req.state().trim());
        hotel.setZip(req.zip().trim());

        if (req.timezone() != null && !req.timezone().isBlank()) {
            hotel.setTimezone(req.timezone().trim());
        }
    }
}

