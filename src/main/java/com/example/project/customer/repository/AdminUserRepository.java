package com.example.project.customer.repository;

import com.example.project.customer.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Integer> {

    Optional<AdminUser> findByEmailIgnoreCase(String email);

    Optional<AdminUser> findByFirebaseUid(String firebaseUid);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByFirebaseUid(String firebaseUid);
}
