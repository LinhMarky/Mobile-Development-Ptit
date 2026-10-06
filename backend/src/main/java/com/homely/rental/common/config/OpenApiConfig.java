package com.homely.rental.common.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;
import java.util.Set;

@Configuration
public class OpenApiConfig {
    @org.springframework.beans.factory.annotation.Value("${apiPrefix:api/v1}")
    private String apiPrefix = "api/v1";
    @Bean OpenAPI homelyApi() {
        return new OpenAPI().info(new Info().title("Homely API").version("1.6.0")
                .description("HTTP JSON dùng {statusCode,message,data}; DTO trong data dùng snake_case, VND là chuỗi số nguyên. Thanh toán và hoàn tiền là sandbox."))
                .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
    @Bean OpenApiCustomizer protocol() {
        return api -> {
            ModelConverters.getInstance().readAll(com.homely.rental.common.dto.ProblemDTO.class).forEach(api.getComponents()::addSchemas);
            api.getComponents().addSchemas("RestResponseProblemDTO", envelope(500, new Schema<>().$ref("#/components/schemas/ProblemDTO")));
            api.getComponents().addSchemas("RestResponseVoid", envelope(200, new ObjectSchema().nullable(true)));
            String base = "/" + apiPrefix.replaceAll("^/+|/+$", "") + "/";
            var guarded=Set.of("rooms","listings","wishlist","conversations","admin","notifications","bookings","payments","viewings","viewing-slots","reviews","reports");
            api.getPaths().forEach((path,item)->item.readOperationsMap().forEach((method,operation)->{
                String relative=path.startsWith(base) ? path.substring(base.length()) : path;
                String resource=relative.split("/")[0];
                boolean publicRead=method==PathItem.HttpMethod.GET &&
                        ((resource.equals("listings")&&!relative.equals("listings/my")) || resource.equals("amenities")
                        || relative.startsWith("media/room/") || relative.matches("media/[^/]+/content")
                        || relative.startsWith("reviews/room/") || resource.equals("viewing-slots"));
                boolean publicAuth=Set.of("auth/login","auth/register","auth/refresh","auth/verify-email").contains(relative);
                if(!publicRead&&!publicAuth&&!path.startsWith("/webhooks/"))operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                if(method==PathItem.HttpMethod.POST&&guarded.contains(resource))operation.addParametersItem(new Parameter()
                        .name("Idempotency-Key").in("header").required(true).schema(new StringSchema().minLength(1).maxLength(128))
                        .description("Giữ nguyên khi retry cùng thao tác; cùng khóa nhưng payload khác trả 409."));
                operation.getResponses().forEach((code, result) -> {
                    if (!code.matches("2[0-9]{2}") || code.equals("204") || code.equals("205")) return;
                    Content original = result.getContent();
                    if (original != null && original.keySet().stream().anyMatch(type -> type.startsWith("image/")
                            || type.startsWith("video/") || type.equals("application/octet-stream"))) return;
                    Schema<?> payload = original == null || original.isEmpty() ? new ObjectSchema().nullable(true)
                            : original.values().iterator().next().getSchema();
                    if (payload == null) payload = new ObjectSchema().nullable(true);
                    if ("binary".equals(payload.getFormat()) || "byte".equals(payload.getFormat())) return;
                    boolean alreadyWrapped = payload.getProperties() != null && payload.getProperties().keySet().containsAll(java.util.List.of("statusCode", "message", "data"));
                    result.setContent(new Content().addMediaType("application/json", new MediaType().schema(
                            alreadyWrapped ? payload : envelope(Integer.parseInt(code), payload))));
                });
                operation.getResponses().addApiResponse("default",new ApiResponse().description("HTTP status giữ nguyên; data chứa ProblemDTO với code, field_errors và trace_id")
                        .content(new Content().addMediaType("application/json",new MediaType().schema(new Schema<>().$ref("#/components/schemas/RestResponseProblemDTO")))));
            }));
            api.getComponents().getSchemas().values().forEach(schema->{
                if(schema.getProperties()!=null)schema.getProperties().forEach((name,value)->{
                    if(name.toString().endsWith("_vnd")){
                        Schema<?> money=(Schema<?>)value;money.setType("string");money.setFormat(null);money.setPattern("^[0-9]+$");
                    }
                });
            });
        };
    }

    private static Schema<?> envelope(int status, Schema<?> payload) {
        return new ObjectSchema().addProperty("statusCode", new IntegerSchema().example(status))
                .addProperty("message", new StringSchema()).addProperty("data", payload)
                .required(java.util.List.of("statusCode", "message", "data"));
    }
}
