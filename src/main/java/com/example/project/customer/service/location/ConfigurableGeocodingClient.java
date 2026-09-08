package com.example.project.customer.service.location;

import com.example.project.customer.dto.ReverseGeocodeResponse;
import com.example.project.customer.exception.GeocodingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;

@Slf4j
@Component
public class ConfigurableGeocodingClient implements GeocodingClient {

    private final String provider;
    private final String apiKey;
    private final String baseUrl;
    private final int timeoutMs;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ConfigurableGeocodingClient(
            @Value("${location.provider:google}") String provider,
            @Value("${location.api-key:}") String apiKey,
            @Value("${location.base-url:}") String baseUrl,
            @Value("${location.timeout-ms:5000}") int timeoutMs,
            ObjectMapper objectMapper) {
        this.provider = provider != null ? provider.trim().toLowerCase(Locale.ROOT) : "google";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "";
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : 5000;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(this.timeoutMs);
        factory.setReadTimeout(this.timeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public ReverseGeocodeResponse reverseGeocode(Double latitude, Double longitude) {
        if ("google".equalsIgnoreCase(provider) && !apiKey.isBlank()) {
            try {
                return reverseGeocodeWithGoogle(latitude, longitude);
            } catch (Exception e) {
                log.warn("Google Maps geocoding failed for ({}, {}): {}. Attempting fallback...",
                        latitude, longitude, e.getMessage());
                return reverseGeocodeWithNominatim(latitude, longitude);
            }
        }

        // Default or fallback to OpenStreetMap Nominatim
        return reverseGeocodeWithNominatim(latitude, longitude);
    }

    private ReverseGeocodeResponse reverseGeocodeWithGoogle(Double latitude, Double longitude) {
        String url = !baseUrl.isBlank() ? baseUrl : "https://maps.googleapis.com/maps/api/geocode/json";
        String requestUrl = String.format(Locale.ROOT, "%s?latlng=%.6f,%.6f&key=%s",
                url, latitude, longitude, apiKey);

        log.info("Calling Google Maps reverse geocoding for coordinates: ({}, {})", latitude, longitude);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(requestUrl, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GeocodingException("Google Maps returned non-200 status: " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String status = root.path("status").asText("");
            if (!"OK".equalsIgnoreCase(status)) {
                String errorMsg = root.path("error_message").asText(status);
                throw new GeocodingException("Google Maps Geocoding API returned status: " + status + " (" + errorMsg + ")");
            }

            JsonNode results = root.path("results");
            if (results.isEmpty()) {
                throw new GeocodingException("No address results found for coordinates: " + latitude + ", " + longitude);
            }

            JsonNode primaryResult = results.get(0);
            String formattedAddress = primaryResult.path("formatted_address").asText("");

            String houseNumber = "";
            String route = "";
            String area = "";
            String city = "";
            String state = "";
            String country = "India";
            String pincode = "";

            JsonNode components = primaryResult.path("address_components");
            for (JsonNode c : components) {
                JsonNode types = c.path("types");
                for (JsonNode t : types) {
                    String type = t.asText();
                    if ("street_number".equals(type) || "premise".equals(type) || "subpremise".equals(type)) {
                        if (houseNumber.isEmpty()) {
                            houseNumber = c.path("long_name").asText();
                        }
                    } else if ("route".equals(type)) {
                        route = c.path("long_name").asText();
                    } else if ("sublocality_level_1".equals(type) || "sublocality".equals(type) || "neighborhood".equals(type)) {
                        if (area.isEmpty()) {
                            area = c.path("long_name").asText();
                        }
                    } else if ("locality".equals(type)) {
                        city = c.path("long_name").asText();
                    } else if ("administrative_area_level_2".equals(type) && city.isEmpty()) {
                        city = c.path("long_name").asText();
                    } else if ("administrative_area_level_1".equals(type)) {
                        state = c.path("long_name").asText();
                    } else if ("country".equals(type)) {
                        country = c.path("long_name").asText();
                    } else if ("postal_code".equals(type)) {
                        pincode = c.path("long_name").asText();
                    }
                }
            }

            String addressLine1 = route;
            if (!houseNumber.isEmpty()) {
                addressLine1 = houseNumber + (route.isEmpty() ? "" : ", " + route);
            }
            if (addressLine1.isEmpty()) {
                addressLine1 = !area.isEmpty() ? area : (!city.isEmpty() ? city : "Main Road");
            }

            return ReverseGeocodeResponse.builder()
                    .latitude(latitude)
                    .longitude(longitude)
                    .formattedAddress(formattedAddress)
                    .addressLine1(addressLine1)
                    .addressLine2(!area.isEmpty() && !area.equalsIgnoreCase(addressLine1) ? area : null)
                    .area(!area.isEmpty() ? area : city)
                    .city(!city.isEmpty() ? city : "Hyderabad")
                    .state(!state.isEmpty() ? state : "Telangana")
                    .country(country)
                    .pincode(pincode)
                    .build();

        } catch (GeocodingException ge) {
            throw ge;
        } catch (Exception ex) {
            log.error("Error invoking Google Maps Geocoding API: {}", ex.getMessage());
            throw new GeocodingException("Failed to reverse-geocode coordinates via Google Maps: " + ex.getMessage(), ex);
        }
    }

    private ReverseGeocodeResponse reverseGeocodeWithNominatim(Double latitude, Double longitude) {
        String url = !baseUrl.isBlank() && !baseUrl.contains("googleapis")
                ? baseUrl
                : "https://nominatim.openstreetmap.org/reverse";
        String requestUrl = String.format(Locale.ROOT, "%s?format=jsonv2&lat=%.6f&lon=%.6f&addressdetails=1",
                url, latitude, longitude);

        log.info("Calling OpenStreetMap Nominatim reverse geocoding for coordinates: ({}, {})", latitude, longitude);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "HinchMart-Backend/1.0 (contact@hinchmart.com)");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GeocodingException("Geocoding service returned status: " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("error")) {
                throw new GeocodingException("Nominatim error: " + root.path("error").asText());
            }

            String formattedAddress = root.path("display_name").asText("");
            JsonNode addressNode = root.path("address");

            String houseNumber = addressNode.path("house_number").asText("");
            String road = addressNode.path("road").asText("");
            String suburb = addressNode.path("suburb").asText("");
            String neighbourhood = addressNode.path("neighbourhood").asText("");
            String area = !suburb.isEmpty() ? suburb : neighbourhood;

            String city = addressNode.path("city").asText("");
            if (city.isEmpty()) {
                city = addressNode.path("town").asText("");
            }
            if (city.isEmpty()) {
                city = addressNode.path("county").asText("");
            }
            if (city.isEmpty()) {
                city = addressNode.path("state_district").asText("");
            }

            String state = addressNode.path("state").asText("");
            String country = addressNode.path("country").asText("India");
            String pincode = addressNode.path("postcode").asText("");

            String addressLine1 = road;
            if (!houseNumber.isEmpty()) {
                addressLine1 = houseNumber + (road.isEmpty() ? "" : ", " + road);
            }
            if (addressLine1.isEmpty()) {
                addressLine1 = !area.isEmpty() ? area : (!city.isEmpty() ? city : "Main Road");
            }

            return ReverseGeocodeResponse.builder()
                    .latitude(latitude)
                    .longitude(longitude)
                    .formattedAddress(formattedAddress)
                    .addressLine1(addressLine1)
                    .addressLine2(!area.isEmpty() && !area.equalsIgnoreCase(addressLine1) ? area : null)
                    .area(!area.isEmpty() ? area : city)
                    .city(!city.isEmpty() ? city : "Hyderabad")
                    .state(!state.isEmpty() ? state : "Telangana")
                    .country(country)
                    .pincode(pincode)
                    .build();

        } catch (GeocodingException ge) {
            throw ge;
        } catch (Exception ex) {
            log.error("Error invoking Nominatim geocoding service: {}", ex.getMessage());
            throw new GeocodingException("Failed to reverse geocode coordinates. Please enter address manually.", ex);
        }
    }
}
