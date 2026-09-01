package com.example.project.customer.repository;

import com.example.project.customer.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {
    Optional<UserProfile> findByPhone(String phone);
    Optional<UserProfile> findByEmail(String email);
}
