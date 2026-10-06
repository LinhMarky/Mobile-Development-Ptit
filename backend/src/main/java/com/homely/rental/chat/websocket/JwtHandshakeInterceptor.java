package com.homely.rental.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.chat.service.ChatException;
import com.homely.rental.common.dto.ProblemDTO;
import com.homely.rental.common.dto.RestResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    static final String AUTHENTICATION_ATTRIBUTE = JwtHandshakeInterceptor.class.getName() + ".authentication";
    private final JwtAccessAuthenticator authenticator;
    private final ObjectMapper mapper;

    public JwtHandshakeInterceptor(JwtAccessAuthenticator authenticator, ObjectMapper mapper) {
        this.authenticator = authenticator;
        this.mapper = mapper;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) throws Exception {
        try {
            attributes.put(AUTHENTICATION_ATTRIBUTE,
                    authenticator.authenticate(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)));
            return true;
        } catch (ChatProtocolException | ChatException ex) {
            ProblemDTO problem = ChatProblems.from(ex);
            problem.setInstance(request.getURI().getPath());
            response.setStatusCode(HttpStatusCode.valueOf(problem.getStatus()));
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().setCacheControl("no-store");
            mapper.writeValue(response.getBody(), RestResponse.of(problem.getStatus(), problem));
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
    }
}
