package com.example.project.customer.service.location;

import com.example.project.customer.dto.PincodeServiceabilityResponse;

public interface ServiceabilityService {

    PincodeServiceabilityResponse checkPincode(String pincode);
}
