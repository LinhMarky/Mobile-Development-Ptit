package com.homely.rental.common.response;

import jakarta.servlet.http.HttpServletResponse;
import com.homely.rental.common.dto.RestResponse;
import com.homely.rental.common.annotation.ApiMessage;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class FormatRestResponse implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // Contract v1.4: all controllers return DTOs directly, including empty 204 bodies.
        return false;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        int status = servletResponse.getStatus();

        // 1. Không bọc String và Resource (file download)
        if (body instanceof String || body instanceof Resource) {
            return body;
        }

        // 2. Không bọc nếu body đã là RestResponse (chống double-wrap)
        if (body instanceof RestResponse) {
            return body;
        }

        // 3. Không bọc Swagger/OpenAPI endpoints
        String path = request.getURI().getPath();
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return body;
        }

        // 4. Không bọc Webhook endpoints (payment gateways expect raw JSON)
        if (path.startsWith("/webhooks")) {
            return body;
        }

        // 5. Không bọc error responses (status >= 400)
        //    → GlobalException đã tự tạo RestResponse rồi, để nó pass qua
        if (status >= 400) {
            return body;
        }

        // 6. Chỉ bọc success responses
        RestResponse<Object> res = new RestResponse<Object>();
        res.setStatusCode(status);
        res.setData(body);
        ApiMessage message = returnType.getMethodAnnotation(ApiMessage.class);
        res.setMessage(message != null ? message.value() : "Call api success");
        return res;
    }
}
