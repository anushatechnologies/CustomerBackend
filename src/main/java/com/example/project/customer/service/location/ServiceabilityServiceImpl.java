package com.example.project.customer.service.location;

import com.example.project.customer.dto.PincodeServiceabilityResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ServiceabilityServiceImpl implements ServiceabilityService {

    private static final Map<String, PincodeInfo> KNOWN_PINCODES = new HashMap<>();

    static {
        KNOWN_PINCODES.put("500081", new PincodeInfo("Hyderabad", "Telangana", "Madhapur / Hitec City", true, 2, true));
        KNOWN_PINCODES.put("500032", new PincodeInfo("Hyderabad", "Telangana", "Gachibowli", true, 2, true));
        KNOWN_PINCODES.put("500034", new PincodeInfo("Hyderabad", "Telangana", "Banjara Hills", true, 2, true));
        KNOWN_PINCODES.put("500033", new PincodeInfo("Hyderabad", "Telangana", "Jubilee Hills", true, 2, true));
        KNOWN_PINCODES.put("500072", new PincodeInfo("Hyderabad", "Telangana", "Kukatpally", true, 2, true));
        KNOWN_PINCODES.put("560001", new PincodeInfo("Bengaluru", "Karnataka", "MG Road / Central", true, 2, true));
        KNOWN_PINCODES.put("560034", new PincodeInfo("Bengaluru", "Karnataka", "Koramangala", true, 2, true));
        KNOWN_PINCODES.put("560066", new PincodeInfo("Bengaluru", "Karnataka", "Whitefield", true, 2, true));
        KNOWN_PINCODES.put("400001", new PincodeInfo("Mumbai", "Maharashtra", "Fort / South Mumbai", true, 2, true));
        KNOWN_PINCODES.put("400051", new PincodeInfo("Mumbai", "Maharashtra", "Bandra Kurla Complex", true, 2, true));
        KNOWN_PINCODES.put("411001", new PincodeInfo("Pune", "Maharashtra", "Pune Station / Camp", true, 2, true));
        KNOWN_PINCODES.put("110001", new PincodeInfo("New Delhi", "Delhi", "Connaught Place", true, 2, true));
        KNOWN_PINCODES.put("122001", new PincodeInfo("Gurugram", "Haryana", "Cyber City", true, 2, true));
        KNOWN_PINCODES.put("201301", new PincodeInfo("Noida", "Uttar Pradesh", "Sector 18", true, 2, true));
        KNOWN_PINCODES.put("600001", new PincodeInfo("Chennai", "Tamil Nadu", "George Town", true, 2, true));
        KNOWN_PINCODES.put("700001", new PincodeInfo("Kolkata", "West Bengal", "BBD Bagh", true, 3, true));
        KNOWN_PINCODES.put("380001", new PincodeInfo("Ahmedabad", "Gujarat", "Lal Darwaja", true, 2, true));
        KNOWN_PINCODES.put("530001", new PincodeInfo("Visakhapatnam", "Andhra Pradesh", "One Town", true, 3, false));
        KNOWN_PINCODES.put("520001", new PincodeInfo("Vijayawada", "Andhra Pradesh", "Governorpet", true, 3, false));
    }

    @Override
    public PincodeServiceabilityResponse checkPincode(String rawPincode) {
        if (rawPincode == null || rawPincode.trim().isEmpty()) {
            return PincodeServiceabilityResponse.builder()
                    .pincode("")
                    .serviceable(false)
                    .estimatedDays(null)
                    .isExpressAvailable(false)
                    .build();
        }

        String pin = rawPincode.trim().replaceAll("\\D", "");

        if (KNOWN_PINCODES.containsKey(pin)) {
            PincodeInfo info = KNOWN_PINCODES.get(pin);
            return PincodeServiceabilityResponse.builder()
                    .pincode(pin)
                    .city(info.city)
                    .state(info.state)
                    .area(info.area)
                    .serviceable(info.serviceable)
                    .estimatedDays(info.estimatedDays)
                    .isExpressAvailable(info.isExpressAvailable)
                    .build();
        }

        return resolveByPrefix(pin);
    }

    private PincodeServiceabilityResponse resolveByPrefix(String pin) {
        if (pin.length() < 2) {
            return PincodeServiceabilityResponse.builder()
                    .pincode(pin)
                    .serviceable(false)
                    .city("Unknown")
                    .state("India")
                    .estimatedDays(5)
                    .isExpressAvailable(false)
                    .build();
        }

        String p2 = pin.substring(0, 2);
        char p1 = pin.charAt(0);

        String city = "Metro Area";
        String state = "India";
        int days = 3;
        boolean express = false;

        switch (p2) {
            case "11": city = "New Delhi"; state = "Delhi"; days = 2; express = true; break;
            case "12": case "13": city = "Gurugram / Faridabad"; state = "Haryana"; days = 2; express = true; break;
            case "14": case "15": city = "Ludhiana / Amritsar"; state = "Punjab"; days = 3; break;
            case "16": city = "Chandigarh"; state = "Chandigarh"; days = 2; express = true; break;
            case "20": case "21": case "22": case "23": case "24": case "25": case "26": case "27": case "28":
                city = "Lucknow / Noida"; state = "Uttar Pradesh"; days = 3; break;
            case "30": case "31": case "32": case "33": case "34":
                city = "Jaipur"; state = "Rajasthan"; days = 3; break;
            case "36": case "37": case "38": case "39":
                city = "Ahmedabad / Surat"; state = "Gujarat"; days = 2; express = true; break;
            case "40": city = "Mumbai / Thane"; state = "Maharashtra"; days = 2; express = true; break;
            case "41": city = "Pune"; state = "Maharashtra"; days = 2; express = true; break;
            case "42": case "43": case "44":
                city = "Nagpur / Nashik"; state = "Maharashtra"; days = 3; break;
            case "45": case "46": case "47": case "48":
                city = "Indore / Bhopal"; state = "Madhya Pradesh"; days = 3; break;
            case "49": city = "Raipur"; state = "Chhattisgarh"; days = 3; break;
            case "50": city = "Hyderabad"; state = "Telangana"; days = 2; express = true; break;
            case "51": case "52": case "53":
                city = "Visakhapatnam / Vijayawada"; state = "Andhra Pradesh"; days = 3; break;
            case "56": city = "Bengaluru"; state = "Karnataka"; days = 2; express = true; break;
            case "57": case "58": case "59":
                city = "Mysuru / Hubballi"; state = "Karnataka"; days = 3; break;
            case "60": city = "Chennai"; state = "Tamil Nadu"; days = 2; express = true; break;
            case "61": case "62": case "63": case "64":
                city = "Coimbatore / Madurai"; state = "Tamil Nadu"; days = 3; break;
            case "67": case "68": case "69":
                city = "Kochi / Thiruvananthapuram"; state = "Kerala"; days = 3; break;
            case "70": city = "Kolkata"; state = "West Bengal"; days = 2; express = true; break;
            case "75": city = "Bhubaneswar"; state = "Odisha"; days = 3; break;
            case "78": city = "Guwahati"; state = "Assam"; days = 4; break;
            case "80": case "81": case "82":
                city = "Patna"; state = "Bihar"; days = 3; break;
            case "83": case "84": case "85":
                city = "Ranchi / Jamshedpur"; state = "Jharkhand"; days = 3; break;
            default:
                if (p1 >= '1' && p1 <= '8') {
                    city = "Regional Hub";
                    state = "India";
                    days = 4;
                }
                break;
        }

        return PincodeServiceabilityResponse.builder()
                .pincode(pin)
                .city(city)
                .state(state)
                .serviceable(true)
                .estimatedDays(days)
                .isExpressAvailable(express)
                .build();
    }

    private static class PincodeInfo {
        final String city;
        final String state;
        final String area;
        final boolean serviceable;
        final int estimatedDays;
        final boolean isExpressAvailable;

        PincodeInfo(String city, String state, String area, boolean serviceable, int estimatedDays, boolean isExpressAvailable) {
            this.city = city;
            this.state = state;
            this.area = area;
            this.serviceable = serviceable;
            this.estimatedDays = estimatedDays;
            this.isExpressAvailable = isExpressAvailable;
        }
    }
}
