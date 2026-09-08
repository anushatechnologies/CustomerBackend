package com.example.project.customer.service;

import com.example.project.customer.dto.UserProfileResponse;
import com.example.project.customer.dto.UserProfileUpdateRequest;
import com.example.project.customer.entity.Customer;

public interface UserService {

    Customer syncUserWithFirebase(String firebaseUid, String email, String name, String phone);

    Customer getCustomerByFirebaseUid(String firebaseUid);

    Customer getCustomerById(Integer id);

    UserProfileResponse getUserProfile(Integer userId);

    UserProfileResponse updateUserProfile(Integer userId, UserProfileUpdateRequest request);

    Integer resolveSellerIdForUser(Customer customer);
}
