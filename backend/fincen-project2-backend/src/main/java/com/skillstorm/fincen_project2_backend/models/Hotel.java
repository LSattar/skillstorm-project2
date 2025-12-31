package com.skillstorm.fincen_project2_backend.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Generated(event = EventType.INSERT)
    @Column(name = "hotel_id", nullable = false, updatable = false, length = 255)
    private UUID hotelId;

    @NotBlank
    @Size(max = 200)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @NotBlank
    @Size(max = 30)
    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @NotBlank
    @Size(max = 200)
    @Column(name = "address1", nullable = false, length = 200)
    private String address1;

    @Size(max = 200)
    @Column(name = "address2", length = 200)
    private String address2;

    @NotBlank
    @Size(max = 60)
    @Column(name = "city", nullable = false, length = 60)
    private String city;

    @NotBlank
    @Size(min = 2, max = 2)
    @Column(name = "state", nullable = false, length = 2)
    private String state;

    @NotBlank
    @Size(min = 5, max = 10)
    @Column(name = "zip", nullable = false, length = 10)
    private String zip;

    @NotBlank
    @Size(max = 80)
    @Column(name = "timezone", nullable = false, length = 80)
    private String timezone = "America/Chicago";

    // DB-owned (DEFAULT NOW() + trigger set_updated_at())
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Hotel() {
    }

    public Hotel(String name, String phone, String address1, String city, String state, String zip) {
        this.name = name;
        this.phone = phone;
        this.address1 = address1;
        this.city = city;
        this.state = state;
        this.zip = zip;
    }

    public UUID getHotelId() {
        return hotelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
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
        if (!(o instanceof Hotel other))
            return false;
        return hotelId != null && hotelId.equals(other.hotelId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Hotel{hotelId=" + hotelId + ", name=" + name + "}";
    }
}

