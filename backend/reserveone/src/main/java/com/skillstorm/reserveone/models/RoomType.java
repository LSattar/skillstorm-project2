package com.skillstorm.reserveone.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "room_types")
public class RoomType {

    public enum BedType {
        TWIN, FULL, QUEEN, KING, SOFA
    }

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Generated(event = EventType.INSERT)
    @Column(name = "room_type_id", nullable = false, updatable = false, length = 255)
    private UUID roomTypeId;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @NotBlank
    @Size(max = 120)
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Size(max = 2000)
    @Column(name = "description", length = 2000)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @NotNull
    @Min(1)
    @Column(name = "max_guests", nullable = false)
    private Integer maxGuests;

    @NotNull
    @Min(1)
    @Column(name = "bed_count", nullable = false)
    private Integer bedCount = 1;

    @NotNull
    @Convert(converter = BedTypeConverter.class)
    @Column(name = "bed_type", nullable = false, length = 60)
    private BedType bedType = BedType.QUEEN;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // DB-owned (DEFAULT NOW() + trigger set_updated_at())
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected RoomType() {
    }

    public RoomType(Hotel hotel, String name, BigDecimal basePrice, Integer maxGuests) {
        this.hotel = hotel;
        this.name = name;
        this.basePrice = basePrice;
        this.maxGuests = maxGuests;
    }

    public UUID getRoomTypeId() {
        return roomTypeId;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(Integer maxGuests) {
        this.maxGuests = maxGuests;
    }

    public Integer getBedCount() {
        return bedCount;
    }

    public void setBedCount(Integer bedCount) {
        this.bedCount = bedCount;
    }

    public BedType getBedType() {
        return bedType;
    }

    public void setBedType(BedType bedType) {
        this.bedType = bedType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RoomType other))
            return false;
        return roomTypeId != null && roomTypeId.equals(other.roomTypeId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "RoomType{roomTypeId=" + roomTypeId + ", name=" + name + "}";
    }
}
