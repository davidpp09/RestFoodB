package restaurante.api.infra.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 📮 Buzón para que el servidor envíe mensajes al cliente (ej: /topic/cocina)
        config.enableSimpleBroker("/topic");

        // 📥 Prefijo para mensajes que el cliente envía al servidor
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 🔌 El punto de conexión físico para el Frontend
        registry.addEndpoint("/ws-restfood")
                .setAllowedOrigins("*") // ⚠️ En red local permitimos todo, luego podemos ajustar
                .withSockJS(); // 🛠️ Plan B por si el navegador no soporta WebSockets puros
    }
    
}
