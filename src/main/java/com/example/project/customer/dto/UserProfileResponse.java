package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    private Integer userId;
    private String firebaseUid;
    private String name;
    private String email;
    private String phone;
    private String role;
    private boolean active;
    private Integer sellerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
