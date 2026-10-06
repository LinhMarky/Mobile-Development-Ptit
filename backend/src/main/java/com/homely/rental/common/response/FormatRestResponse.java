package com.homely.rental.common.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.common.dto.RestResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Wrap application HTTP JSON once; leave file/stream responses and framework endpoints alone. */
@RestControllerAdvice(basePackages = "com.homely.rental")
public class FormatRestResponse implements ResponseBodyAdvice<Object> {
    private final ObjectMapper mapper;

    public FormatRestResponse(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType)
                || StringHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        int status = response instanceof ServletServerHttpResponse servlet ? servlet.getServletResponse().getStatus() : 200;
        if (status < 200 || status == 204 || status == 205 || status == 304) return body;
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Object envelope = body instanceof RestResponse<?> ? body : RestResponse.of(status, body);
        if (StringHttpMessageConverter.class.isAssignableFrom(converterType)) {
            try {
                return mapper.writeValueAsString(envelope);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Unable to serialize the API response", ex);
            }
        }
        return envelope;
    }
}
