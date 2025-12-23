package com.skillstorm.fincen_project2_backend.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

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
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(name = "uq_roles_name", columnNames = "name"))
public class Role {

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @JsonIgnore
    private final Set<User> users = new HashSet<>();

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Generated(event = EventType.INSERT)
    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    @NotBlank
    @Size(max = 40)
    @Column(name = "name", nullable = false, length = 40, updatable = false)
    private String name;

    protected Role() {
    }

    public Role(String name) {
        String normalized = normalizeName(name);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("Role name must not be blank");
        }
        this.name = normalized;
    }

    public Set<User> getUsers() {
        return Collections.unmodifiableSet(users);
    }

    public UUID getRoleId() {
        return roleId;
    }

    public String getName() {
        return name;
    }

    private static String normalizeName(String name) {
        return (name == null) ? null : name.trim().toUpperCase();
    }

    /**
     * Internal helper used to keep the bidirectional User–Role relationship
     * consistent inside the persistence context.
     *
     * This method should NOT be called directly by application code.
     * The owning side of the relationship is User.roles, and all role
     * assignments should be initiated from the User entity (via
     * addRole/removeRole).
     *
     * Keeping this method package-private prevents external misuse while
     * still allowing User to synchronize both sides of the association.
     */

    void internalAddUser(User user) {
        if (user != null) {
            users.add(user);
        }
    }

    /**
     * Internal helper used to remove a User from this Role while maintaining
     * bidirectional association consistency.
     *
     * This method exists solely to support User.removeRole(...). External
     * callers should never modify Role.users directly, as Role is the
     * inverse (non-owning) side of the relationship.
     */

    void internalRemoveUser(User user) {
        if (user != null) {
            users.remove(user);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Role other)) {
            return false;
        }
        return roleId != null && roleId.equals(other.roleId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Role [roleId=" + roleId + ", name=" + name + "]";
    }
}
