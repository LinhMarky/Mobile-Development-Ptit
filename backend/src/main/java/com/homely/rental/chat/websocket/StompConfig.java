package com.homely.rental.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtHandshakeInterceptor handshakeInterceptor;
    private final ChatStompInterceptor inboundInterceptor;
    private final ChatWebSocketSessions sessions;
    private final ChatStompErrorHandler errorHandler;
    private final String[] allowedOrigins;
    private final ObjectMapper objectMapper;

    public StompConfig(JwtHandshakeInterceptor handshakeInterceptor, ChatStompInterceptor inboundInterceptor,
                       ChatWebSocketSessions sessions, ChatStompErrorHandler errorHandler,
                       ObjectMapper objectMapper,
                       @Value("${homely.chat.allowed-origins:}") String allowedOrigins) {
        this.handshakeInterceptor = handshakeInterceptor;
        this.inboundInterceptor = inboundInterceptor;
        this.sessions = sessions;
        this.errorHandler = errorHandler;
        this.objectMapper = objectMapper;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim)
                .filter(origin -> !origin.isEmpty()).toArray(String[]::new);
        for (String origin : this.allowedOrigins) {
            if (origin.contains("*") || !(origin.startsWith("https://") || origin.startsWith("http://"))) {
                throw new IllegalArgumentException("Chat origins must be explicit HTTP(S) origins without wildcards");
            }
        }
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.setErrorHandler(errorHandler);
        registry.setPreserveReceiveOrder(true);
        registry.addEndpoint("/ws").addInterceptors(handshakeInterceptor).setAllowedOrigins(allowedOrigins);
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> converters) {
        MappingJackson2MessageConverter json = new MappingJackson2MessageConverter();
        json.setObjectMapper(objectMapper.copy().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        converters.add(json);
        return true;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        registry.enableSimpleBroker("/queue");
        registry.setPreservePublishOrder(true);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(inboundInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(sessions);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(ChatStompInterceptor.MAX_MESSAGE_BYTES)
                .setSendBufferSizeLimit(256 * 1024).setSendTimeLimit(10_000)
                .addDecoratorFactory(sessions::decorate);
    }
}
