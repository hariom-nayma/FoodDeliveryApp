package com.fooddelivery.config;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.fooddelivery.service.DeliveryPartnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SocketEventHandler {

    private final SocketIOServer server;
    private final DeliveryPartnerService deliveryPartnerService;

    // We need to register listeners explicitly if not using annotation scanner or
    // if manual start interferes.
    // However, netty-socketio usually supports @OnEvent beans if the scanner is
    // enabled.
    // For safety with our manual Runner, we can register in a @PostConstruct or
    // within the runner.
    // Let's rely on Spring annotation scanning for now.

    @OnEvent("join_room")
    public void onJoinRoom(SocketIOClient client, String room) {
        client.joinRoom(room);
        log.info("Client {} joined room {}", client.getSessionId(), room);
    }

    @OnEvent("update_location")
    public void onUpdateLocation(SocketIOClient client, Map<String, Object> data) {
        // Expected: { userId: "...", lat: 12.34, lng: 56.78 }
        try {
            String userId = (String) data.get("userId");
            Double lat = Double.valueOf(data.get("lat").toString());
            Double lng = Double.valueOf(data.get("lng").toString());
            
            if (userId != null) {
               deliveryPartnerService.updateLocation(userId, lat, lng);
               // log.debug("Location updated for {}", userId);
            }
        } catch (Exception e) {
            log.error("Error updating location via socket: {}", e.getMessage());
        }
    }
}
