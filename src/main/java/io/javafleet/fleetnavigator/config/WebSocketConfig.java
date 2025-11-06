package io.javafleet.fleetnavigator.config;

import io.javafleet.fleetnavigator.websocket.FleetOfficerWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket configuration for Fleet Officer communication
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final FleetOfficerWebSocketHandler fleetOfficerWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(fleetOfficerWebSocketHandler, "/api/fleet-officer/ws/{officerId}")
                .setAllowedOrigins("*"); // In production: specify allowed origins
    }

    /**
     * Configure WebSocket buffer sizes to handle large log files
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // Set max message size to 10MB (for large log files)
        container.setMaxTextMessageBufferSize(10 * 1024 * 1024);
        container.setMaxBinaryMessageBufferSize(10 * 1024 * 1024);
        // Set max session idle timeout to 10 minutes
        container.setMaxSessionIdleTimeout(600000L);
        return container;
    }
}
