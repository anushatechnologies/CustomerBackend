package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthUserResponse {

    private Integer userId;
    private String firebaseUid;
    private String email;
    private String name;
    private String phone;
    private String role;
    private Integer sellerId;
    private Map<String, Object> claims;
}
