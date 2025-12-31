package com.skillstorm.reserveone.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_role_name", columnNames = { "name" })
        }
)
public class Role {

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @JsonIgnore
    private final Set<User> users = new HashSet<>();

    @Id
    @UuidGenerator
    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    @NotBlank
    @Size(max = 40)
    @Column(name = "name", nullable = false, updatable = false, length = 40)
    private String name;

    protected Role() {
        // for JPA
    }

    public Role(String name) {
        this.name = normalizeName(name);
        if (this.name == null) {
            throw new IllegalArgumentException("Role name must not be blank");
        }
    }

    public UUID getRoleId() {
        return roleId;
    }

    public String getName() {
        return name;
    }

    public Set<User> getUsers() {
        return Collections.unmodifiableSet(users);
    }

    void internalAddUser(User user) {
        if (user != null) {
            users.add(user);
        }
    }

    void internalRemoveUser(User user) {
        if (user != null) {
            users.remove(user);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role other)) return false;
        return roleId != null && roleId.equals(other.roleId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Role{roleId=" + roleId + ", name='" + name + "'}";
    }

    private static String normalizeName(String name) {
        if (name == null) return null;
        String t = name.trim();
        return t.isBlank() ? null : t.toUpperCase();
    }
}
