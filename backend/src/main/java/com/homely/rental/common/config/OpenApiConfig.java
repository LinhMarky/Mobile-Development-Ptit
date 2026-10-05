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
    @Bean OpenAPI homelyApi() {
        return new OpenAPI().info(new Info().title("Homely API").version("1.4.0")
                .description("DTO trực tiếp, snake_case; tiền VND là chuỗi số nguyên. Thanh toán và hoàn tiền là sandbox."))
                .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
    @Bean OpenApiCustomizer protocol() {
        return api -> {
            ModelConverters.getInstance().read(com.homely.rental.common.dto.ProblemDTO.class).forEach(api.getComponents()::addSchemas);
            var guarded=Set.of("rooms","listings","wishlist","conversations","admin","notifications","bookings","payments","viewings","viewing-slots","reviews","reports");
            api.getPaths().forEach((path,item)->item.readOperationsMap().forEach((method,operation)->{
                String relative=path.replaceFirst("^/api/v1/","");
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
                operation.getResponses().addApiResponse("default",new ApiResponse().description("Lỗi theo RFC 9457; status và code xác định nguyên nhân")
                        .content(new Content().addMediaType("application/problem+json",new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDTO")))));
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
}
