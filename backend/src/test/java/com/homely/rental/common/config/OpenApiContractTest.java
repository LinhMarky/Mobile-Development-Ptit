package com.homely.rental.common.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.media.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

class OpenApiContractTest {
    @Test void customPrefixHasCorrectSecurityIdempotencyAndCompleteProblemSchemas() {
        OpenApiConfig config = new OpenApiConfig(); ReflectionTestUtils.setField(config, "apiPrefix", "custom/v2");
        var login = new Operation().responses(new ApiResponses().addApiResponse("200", new ApiResponse().content(
                new Content().addMediaType("*/*", new MediaType().schema(new Schema<>().$ref("#/components/schemas/AuthResponse"))))));
        var create = new Operation().responses(new ApiResponses());
        var media = new Operation().responses(new ApiResponses().addApiResponse("200", new ApiResponse().content(
                new Content().addMediaType("image/jpeg", new MediaType().schema(new StringSchema().format("binary"))))));
        OpenAPI api = config.homelyApi().paths(new Paths()
                .addPathItem("/custom/v2/auth/login", new PathItem().post(login))
                .addPathItem("/custom/v2/bookings", new PathItem().post(create))
                .addPathItem("/custom/v2/media/7/content", new PathItem().get(media)));
        config.protocol().customise(api);
        assertThat(login.getSecurity()).isNullOrEmpty(); assertThat(media.getSecurity()).isNullOrEmpty();
        assertThat(create.getSecurity()).isNotEmpty();
        assertThat(create.getParameters()).anySatisfy(parameter -> {
            assertThat(parameter.getName()).isEqualTo("Idempotency-Key"); assertThat(parameter.getRequired()).isTrue();
        });
        assertThat(api.getComponents().getSchemas()).containsKeys("ProblemDTO", "FieldErrorDTO");
        assertThat(api.getComponents().getSchemas().get("ProblemDTO").getProperties()).containsKey("field_errors");
        assertThat(login.getResponses().get("default").getContent()).containsKey("application/json");
        assertThat(login.getResponses().get("default").getContent().get("application/json").getSchema().get$ref())
                .isEqualTo("#/components/schemas/RestResponseProblemDTO");
        var envelope = login.getResponses().get("200").getContent().get("application/json").getSchema();
        assertThat(envelope.getProperties()).containsOnlyKeys("statusCode", "message", "data");
        assertThat(((Schema<?>) envelope.getProperties().get("data")).get$ref()).isEqualTo("#/components/schemas/AuthResponse");
        assertThat(media.getResponses().get("200").getContent()).containsOnlyKeys("image/jpeg");
    }
}
