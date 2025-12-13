package com.fooddelivery.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.dto.response.ORSRouteResponse;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ORSRouteParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static ORSRouteResponse parse(String json) {
        try {
            // System.out.println("DEBUG: Parsing ORS JSON...");
            JsonNode obj = mapper.readTree(json);
            
            if (obj.has("error")) {
                 System.err.println("ORS API Error in Body: " + obj.get("error"));
                 return new ORSRouteResponse(); 
            }

            // Handle V2 GeoJSON Structure
            JsonNode features = obj.path("features");
            if (features.isMissingNode() || features.size() == 0) {
                 // Fallback to checking "routes" for backward compatibility
                JsonNode routes = obj.path("routes");
                if (!routes.isMissingNode() && routes.size() > 0) {
                     return parseLegacy(routes.get(0));
                }
                System.err.println("ORS No features/routes found in response");
                return new ORSRouteResponse();
            }

            JsonNode feature = features.get(0);
            JsonNode properties = feature.path("properties");
            JsonNode summary = properties.path("summary");
            
            if (summary.isMissingNode()) {
                JsonNode segments = properties.path("segments");
                if (segments.isArray() && segments.size() > 0) {
                    summary = segments.get(0); 
                }
            }

            ORSRouteResponse resp = new ORSRouteResponse();
            resp.setDistanceMeters(summary.path("distance").asDouble());
            resp.setDurationSeconds(summary.path("duration").asDouble());
            
            // Geometry Handling
            JsonNode geometry = feature.path("geometry");
            if (geometry.isTextual()) {
                resp.setPolyline(geometry.asText());
            } else if (geometry.isObject()) {
                 // Parse coordinates and encode manually
                 JsonNode coordinates = geometry.path("coordinates");
                 if (coordinates.isArray()) {
                     String encoded = encodePolyline(coordinates);
                     resp.setPolyline(encoded);
                 }
            } else {
                 System.err.println("ORS Error: Geometry field missing or invalid.");
            }

            System.out.println("DEBUG: Parsed Polyline length: " + (resp.getPolyline() != null ? resp.getPolyline().length() : "null"));
            return resp;

        } catch (Exception e) {
            System.err.println("Failed to parse ORS response: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to parse ORS response", e);
        }
    }

    private static String encodePolyline(JsonNode coordinatesNode) {
        List<Double[]> points = new ArrayList<>();
        for (JsonNode point : coordinatesNode) {
            // GeoJSON is [lng, lat] -> Encode expects [lat, lng] usually, but let's check standard
            // Standard Polyline Algorithm expects (lat, lng).
            double lng = point.get(0).asDouble();
            double lat = point.get(1).asDouble();
            points.add(new Double[]{lat, lng}); 
        }
        return encode(points);
    }

    private static String encode(List<Double[]> points) {
        StringBuilder result = new StringBuilder();
        long lastLat = 0;
        long lastLng = 0;

        for (Double[] point : points) {
            long numLat = Math.round(point[0] * 1e5);
            long numLng = Math.round(point[1] * 1e5);

            long dLat = numLat - lastLat;
            long dLng = numLng - lastLng;

            encodeValue(dLat, result);
            encodeValue(dLng, result);

            lastLat = numLat;
            lastLng = numLng;
        }
        return result.toString();
    }

    private static void encodeValue(long value, StringBuilder result) {
        value = value < 0 ? ~(value << 1) : (value << 1);
        while (value >= 0x20) {
            result.append(Character.toChars((int) ((0x20 | (value & 0x1f)) + 63)));
            value >>= 5;
        }
        result.append(Character.toChars((int) (value + 63)));
    }

    private static ORSRouteResponse parseLegacy(JsonNode route) {
         JsonNode summary = route.path("summary");
         ORSRouteResponse resp = new ORSRouteResponse();
         resp.setDistanceMeters(summary.path("distance").asDouble());
         resp.setDurationSeconds(summary.path("duration").asDouble());
         resp.setPolyline(route.path("geometry").asText());
         return resp;
    }
}
