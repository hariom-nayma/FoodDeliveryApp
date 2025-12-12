package com.fooddelivery.config;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketIOConfig {

    @Bean
    public com.corundumstudio.socketio.annotation.SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketServer) {
        return new com.corundumstudio.socketio.annotation.SpringAnnotationScanner(socketServer);
    }

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("localhost");
        config.setPort(9092);
        
        // CORS and other settings
        config.setOrigin("*"); 
        
        // Exception Handling
        config.setExceptionListener(new com.corundumstudio.socketio.listener.DefaultExceptionListener());

        return new SocketIOServer(config);
    }
}  
