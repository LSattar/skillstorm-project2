package com.skillstorm.fincen_project2_backend.models;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RoomTypeAmenityId implements java.io.Serializable {

    @Column(name = "room_type_id")
    private UUID roomTypeId;

    @Column(name = "amenity_id")
    private UUID amenityId;

    protected RoomTypeAmenityId() {
    }

    public RoomTypeAmenityId(UUID roomTypeId, UUID amenityId) {
        this.roomTypeId = roomTypeId;
        this.amenityId = amenityId;
    }

    public UUID getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(UUID roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public UUID getAmenityId() {
        return amenityId;
    }

    public void setAmenityId(UUID amenityId) {
        this.amenityId = amenityId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RoomTypeAmenityId other))
            return false;
        return Objects.equals(roomTypeId, other.roomTypeId) && Objects.equals(amenityId, other.amenityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomTypeId, amenityId);
    }
}

