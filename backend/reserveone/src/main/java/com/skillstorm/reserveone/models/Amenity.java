package com.skillstorm.reserveone.models;

import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "amenities")
public class Amenity {

    public enum Category {
        ROOM, PROPERTY
    }

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Generated(event = EventType.INSERT)
    @Column(name = "amenity_id", nullable = false, updatable = false, length = 255)
    private UUID amenityId;

    @NotBlank
    @Size(max = 120)
    @Column(name = "name", nullable = false, unique = true, length = 120)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private Category category = Category.ROOM;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    protected Amenity() {
    }

    public Amenity(String name) {
        this.name = name;
    }

    public Amenity(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    public UUID getAmenityId() {
        return amenityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Amenity other))
            return false;
        return amenityId != null && amenityId.equals(other.amenityId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Amenity{amenityId=" + amenityId + ", name=" + name + "}";
    }
}

