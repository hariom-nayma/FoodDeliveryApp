package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.ORSRouteResponse;
import com.fooddelivery.util.ORSRouteParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/navigation")
@RequiredArgsConstructor
public class ORSNavigationController {

    @Value("${ors.api.key}")
    private String orsApiKey;

    private final RestTemplate restTemplate;

    @GetMapping("/route")
    public ResponseEntity<ApiResponse<Object>> getRoute(
            @RequestParam double fromLat,
            @RequestParam double fromLng,
            @RequestParam double toLat,
            @RequestParam double toLng) {

        String url = String.format(
                "https://api.openrouteservice.org/v2/directions/driving-car?start=%f,%f&end=%f,%f&geometry_format=encodedpolyline",
                fromLng, fromLat, toLng, toLat); // Note: Longitude first for ORS

        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", orsApiKey);
            headers.set("Content-Type", "application/json");

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            System.out.println("DEBUG: Requesting ORS URL: " + url);
            System.out.println("DEBUG: ORS Key (Prefix): "
                    + (orsApiKey != null && orsApiKey.length() > 10 ? orsApiKey.substring(0, 10) : "NULL"));

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    String.class);

            System.out.println("DEBUG: ORS Response Code: " + response.getStatusCode());
            System.out.println("DEBUG: ORS Response Body: " + response.getBody()); 

            ORSRouteResponse resp = ORSRouteParser.parse(response.getBody());
            return ResponseEntity.ok(ApiResponse.success("Route fetched", resp));

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("ERROR: ORS API Error: " + e.getStatusCode());
            System.out.println("ERROR: ORS Body: " + e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error("ORS API Error: " + e.getResponseBodyAsString(), "ORS_ERROR"));
        } catch (Exception e) {
            System.out.println("ERROR: Internal Navigation Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("Internal Server Error", "INTERNAL_ERROR"));
        }
    }
}
