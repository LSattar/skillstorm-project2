package com.skillstorm.fincen_project2_backend.models;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "room_type_amenities")
@IdClass(RoomTypeAmenityId.class)
public class RoomTypeAmenity {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "room_type_id", nullable = false, updatable = false)
    private UUID roomTypeId;

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "amenity_id", nullable = false, updatable = false)
    private UUID amenityId;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "room_type_id", nullable = false, insertable = false, updatable = false)
    private RoomType roomType;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "amenity_id", nullable = false, insertable = false, updatable = false)
    private Amenity amenity;

    protected RoomTypeAmenity() {
    }

    public RoomTypeAmenity(RoomType roomType, Amenity amenity) {
        this.roomType = roomType;
        this.amenity = amenity;
        this.roomTypeId = roomType.getRoomTypeId();
        this.amenityId = amenity.getAmenityId();
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

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
        if (roomType != null) {
            this.roomTypeId = roomType.getRoomTypeId();
        }
    }

    public Amenity getAmenity() {
        return amenity;
    }

    public void setAmenity(Amenity amenity) {
        this.amenity = amenity;
        if (amenity != null) {
            this.amenityId = amenity.getAmenityId();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RoomTypeAmenity other))
            return false;
        if (roomType == null || amenity == null)
            return false;
        return roomType.equals(other.roomType) && amenity.equals(other.amenity);
    }

    @Override
    public int hashCode() {
        if (roomType == null || amenity == null)
            return getClass().hashCode();
        return roomType.hashCode() * 31 + amenity.hashCode();
    }

    @Override
    public String toString() {
        return "RoomTypeAmenity{roomType=" + roomType + ", amenity=" + amenity + "}";
    }
}

