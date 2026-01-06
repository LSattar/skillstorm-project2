package com.skillstorm.reserveone.models;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User {

    public enum Status {
        ACTIVE, INACTIVE, SUSPENDED
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @JsonIgnore
    private final Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @JsonIgnore
    private final Set<OAuthIdentity> oauthIdentities = new HashSet<>();

    @Id
    @UuidGenerator
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Size(max = 120)
    @Column(name = "first_name", length = 120)
    private String firstName;

    @Size(max = 120)
    @Column(name = "last_name", length = 120)
    private String lastName;

    @Email
    @Size(max = 255)
    @Column(name = "email", columnDefinition = "citext")
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

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected User() {
        // for JPA
    }

    public User(String firstName, String lastName, String email) {
        this.firstName = normalizeNullable(firstName);
        this.lastName = normalizeNullable(lastName);
        this.email = normalizeEmail(email);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = normalizeNullable(firstName);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = normalizeNullable(lastName);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = normalizeEmail(email);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = normalizeNullable(phone);
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = normalizeNullable(address1);
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = normalizeNullable(address2);
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = normalizeNullable(city);
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = normalizeNullable(state);
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = normalizeNullable(zip);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = Objects.requireNonNull(status);
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public void addRole(Role role) {
        if (role == null)
            return;
        if (roles.add(role)) {
            role.internalAddUser(this);
        }
    }

    public void removeRole(Role role) {
        if (role == null)
            return;
        if (roles.remove(role)) {
            role.internalRemoveUser(this);
        }
    }

    public Set<OAuthIdentity> getOauthIdentities() {
        return Collections.unmodifiableSet(oauthIdentities);
    }

    public void addOauthIdentity(OAuthIdentity identity) {
        if (identity == null)
            return;
        identity.setUser(this);
        oauthIdentities.add(identity);
    }

    public void removeOauthIdentity(OAuthIdentity identity) {
        if (identity == null)
            return;
        oauthIdentities.remove(identity);
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

    private static String normalizeNullable(String value) {
        if (value == null)
            return null;
        String t = value.trim();
        return t.isBlank() ? null : t;
    }

    private static String normalizeEmail(String email) {
        if (email == null)
            return null;
        String t = email.trim();
        return t.isBlank() ? null : t.toLowerCase();
    }
}
