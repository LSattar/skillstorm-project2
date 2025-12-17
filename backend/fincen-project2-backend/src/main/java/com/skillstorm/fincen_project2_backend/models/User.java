package com.skillstorm.fincen_project2_backend.models;

import java.time.OffsetDateTime;
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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User {

    public enum Status {
        ACTIVE, INACTIVE, SUSPENDED
    }

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Generated(event = EventType.INSERT)
    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private UUID userId;

    @NotBlank
    @Size(max = 120)
    @Column(name = "first_name", nullable = false, length = 120)
    private String firstName;

    @NotBlank
    @Size(max = 120)
    @Column(name = "last_name", nullable = false, length = 120)
    private String lastName;

    // DB: CITEXT + immutable trigger. JPA: never updates it.
    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, updatable = false, columnDefinition = "citext")
    private String email;

    @Size(max = 30)
    @Column(name = "phone", length = 30)
    private String phone;

    @Size(max = 200)
    @Column(name = "address1", length = 200)
    private String address1;

    @Size(max = 200)
    @Column(name = "address2", length = 200)
    private String address2;

    @Size(max = 60)
    @Column(name = "city", length = 60)
    private String city;

    @Size(max = 2)
    @Column(name = "state", length = 2)
    private String state;

    @Size(max = 10)
    @Column(name = "zip", length = 10)
    private String zip;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status = Status.ACTIVE;

    // DB-owned (DEFAULT NOW() + trigger set_updated_at())
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected User() {
    }

    public User(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email; // set only at creation
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    } // no setter

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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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
        if (!(o instanceof User other))
            return false;
        return userId != null && userId.equals(other.userId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "User{userId=" + userId + ", status=" + status + "}";
    }
}
