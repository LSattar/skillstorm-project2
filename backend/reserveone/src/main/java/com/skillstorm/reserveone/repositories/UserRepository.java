package com.skillstorm.reserveone.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import com.skillstorm.reserveone.models.User;

public interface UserRepository extends JpaRepository<User, UUID> {

  boolean existsByEmail(@NonNull String email);

  Optional<User> findByEmail(@NonNull String email);

  @EntityGraph(attributePaths = { "roles" })
  Optional<User> findWithRolesByEmail(@NonNull String email);

  @EntityGraph(attributePaths = { "roles" })
  Optional<User> findWithRolesByUserId(@NonNull UUID userId);

  boolean existsByUserId(@NonNull UUID userId);

  @Query("""
      select u from User u
      where
        (:q = '' or lower(u.firstName) like lower(concat('%', :q, '%'))
         or lower(u.lastName) like lower(concat('%', :q, '%'))
         or lower(u.email) like lower(concat('%', :q, '%')))
        and (:status is null or u.status = :status)
      """)
  Page<User> searchUsers(@Param("q") String q, @Param("status") com.skillstorm.reserveone.models.User.Status status,
      Pageable pageable);
}