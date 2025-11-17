package io.javafleet.fleetnavigator.config;

import io.javafleet.fleetnavigator.websocket.FleetMateWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket configuration for Fleet Mate communication
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final FleetMateWebSocketHandler fleetMateWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(fleetMateWebSocketHandler, "/api/fleet-mate/ws/{mateId}")
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
