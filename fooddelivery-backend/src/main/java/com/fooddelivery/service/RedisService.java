package com.fooddelivery.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private static final String RIDER_GEO_KEY = "riders:geo";

    public void updateRiderLocation(String riderId, double lat, double lng) {
        redisTemplate.opsForGeo().add(RIDER_GEO_KEY, new Point(lng, lat), riderId);
    }

    public void removeRiderLocation(String riderId) {
        redisTemplate.opsForGeo().remove(RIDER_GEO_KEY, riderId);
    }

    public List<String> findNearbyRiders(double lat, double lng, double radiusKm, int limit) {
        Circle circle = new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS));

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .sortAscending()
                .limit(limit);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
                .radius(RIDER_GEO_KEY, circle, args);

        if (results == null)
            return List.of();

        return results.getContent().stream()
                .map(geoResult -> geoResult.getContent().getName())
                .collect(Collectors.toList());
    }

    public boolean tryLock(String key, String value, long timeoutSeconds) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value,
                java.time.Duration.ofSeconds(timeoutSeconds));
        return Boolean.TRUE.equals(success);
    }

    public boolean tryLock(String key, long timeoutSeconds) {
        return tryLock(key, "LOCKED", timeoutSeconds);
    }

    public void unlock(String key) {
        redisTemplate.delete(key);
    }

    public boolean unlock(String key, String value) {
        String currentValue = redisTemplate.opsForValue().get(key);
        if (value.equals(currentValue)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    public boolean isLocked(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public String getLockValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
