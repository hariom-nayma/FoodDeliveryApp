package com.fooddelivery.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {

    private final ObjectMapper objectMapper;

    @GetMapping("/reverse")
    public ResponseEntity<Map<String, Object>> reverseGeocode(@RequestParam Double lat, @RequestParam Double lon) {
        // Use Nominatim (OpenStreetMap)
        // Note: Nominatim requires User-Agent.
        String url = String.format("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%s&lon=%s", lat, lon);

        RestTemplate restTemplate = new RestTemplate();
        // Set User-Agent headers to avoid blocking
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("User-Agent", "FoodDeliveryProject/1.0 (hariom.ojha@spamotte.com)");
        headers.set("Referer", "http://localhost:4200");
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            // Adapt response to mimic Mapbox features format expected by frontend
            // Frontend expects: { features: [ { place_name: "...", context: [ {id: "municipal...", text: "City"}, {id:"postal_code", text: "123"} ] } ] }
            
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> features = new ArrayList<>();
            Map<String, Object> feature = new HashMap<>();
            
            // Construct Place Name
            String displayName = root.path("display_name").asText();
            feature.put("place_name", displayName);

            // Construct Context
            List<Map<String, Object>> context = new ArrayList<>();
            JsonNode addr = root.path("address");
            
            if (addr.has("city") || addr.has("town") || addr.has("village")) {
                String city = addr.has("city") ? addr.get("city").asText() 
                            : addr.has("town") ? addr.get("town").asText() 
                            : addr.get("village").asText();
                context.add(Map.of("id", "municipal_district", "text", city));
            }
            if (addr.has("state")) {
                context.add(Map.of("id", "region", "text", addr.get("state").asText()));
            }
            if (addr.has("postcode")) {
                context.add(Map.of("id", "postal_code", "text", addr.get("postcode").asText()));
            }

            feature.put("context", context);
            features.add(feature);
            result.put("features", features);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
             e.printStackTrace();
             return ResponseEntity.internalServerError().build();
        }
    }
}
