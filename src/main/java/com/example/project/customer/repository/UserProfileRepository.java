package com.example.project.customer.repository;

import com.example.project.customer.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {
    Optional<UserProfile> findByPhone(String phone);
    Optional<UserProfile> findByEmail(String email);
}
