package com.homely.rental.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionTest {

    private static final String SENSITIVE_VALUE = "private-token-value";
    private MockMvc mvc;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mvc = MockMvcBuilders.standaloneSetup(new InputController())
                .setControllerAdvice(new GlobalException(), new com.homely.rental.common.response.FormatRestResponse(mapper))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\":\"private-token-value\",",
            "{\"name\":\"private-token-value\",\"quantity\":\"private-token-value\"}",
            ""
    })
    void malformedMissingAndWrongTypeBodiesReturnInvalidJson(String body) throws Exception {
        JsonNode problem = problem(mvc.perform(post("/input")
                .contentType(MediaType.APPLICATION_JSON).content(body)), 400, "INVALID_JSON", "/input");

        assertThat(problem.get("field_errors").isEmpty()).isTrue();
        assertThat(problem.toString()).doesNotContain(SENSITIVE_VALUE, "HttpMessageNotReadableException",
                "com.fasterxml.jackson");
    }

    @Test
    void invalidQueryValueReturnsFieldErrorWithoutReflectingValueOrQuery() throws Exception {
        JsonNode problem = problem(mvc.perform(get("/query")
                .queryParam("quantity", SENSITIVE_VALUE).queryParam("token", SENSITIVE_VALUE)),
                400, "VALIDATION_FAILED", "/query");

        assertFieldError(problem, "quantity", "Invalid value");
        assertThat(problem.toString()).doesNotContain(SENSITIVE_VALUE, "NumberFormatException");
    }

    @Test
    void missingRequiredQueryParameterReturnsValidationFailure() throws Exception {
        JsonNode problem = problem(mvc.perform(get("/query")), 400, "VALIDATION_FAILED", "/query");

        assertFieldError(problem, "quantity", "This parameter is required");
    }

    @Test
    void invalidPathValueReturnsValidationFailureWithSafeDetail() throws Exception {
        JsonNode problem = problem(mvc.perform(get("/items/not-a-number")),
                400, "VALIDATION_FAILED", "/items/not-a-number");

        assertFieldError(problem, "id", "Invalid value");
        assertThat(problem.get("detail").asText()).doesNotContain("not-a-number", "java.lang");
    }

    @Test
    void modelBindingErrorsDoNotExposeRejectedValues() throws Exception {
        JsonNode problem = problem(mvc.perform(get("/model").queryParam("quantity", SENSITIVE_VALUE)),
                400, "VALIDATION_FAILED", "/model");

        assertFieldError(problem, "quantity", "Invalid value");
        assertThat(problem.toString()).doesNotContain(SENSITIVE_VALUE, "NumberFormatException");
    }

    @Test
    void beanValidationKeepsActionableFieldErrors() throws Exception {
        JsonNode problem = problem(mvc.perform(post("/input").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"quantity\":1}")), 400, "VALIDATION_FAILED", "/input");

        assertFieldError(problem, "name", "Name is required");
    }

    @Test
    void methodParameterConstraintReturnsValidationFailure() throws Exception {
        JsonNode problem = problem(mvc.perform(get("/validated-query").queryParam("quantity", "0")),
                400, "VALIDATION_FAILED", "/validated-query");

        assertFieldError(problem, "quantity", "Invalid value");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/bad-credentials", "/unknown-user"})
    void authenticationErrorsUseContractCodeWithoutAccountDetails(String path) throws Exception {
        JsonNode problem = problem(mvc.perform(get(path).queryParam("token", SENSITIVE_VALUE)),
                401, "INVALID_CREDENTIALS", path);

        assertThat(problem.get("detail").asText()).isEqualTo("Invalid email or password");
        assertThat(problem.toString()).doesNotContain(SENSITIVE_VALUE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/domain-not-found", "/static-not-found", "/no-such-route"})
    void allNotFoundPathsUseContractCode(String path) throws Exception {
        JsonNode problem = problem(mvc.perform(get(path).queryParam("token", SENSITIVE_VALUE)),
                404, "RESOURCE_NOT_FOUND", path);

        assertThat(problem.get("field_errors").isEmpty()).isTrue();
        assertThat(problem.toString()).doesNotContain(SENSITIVE_VALUE);
    }

    @Test
    void unexpectedErrorsDoNotExposeInternalDetails() throws Exception {
        JsonNode problem = problem(mvc.perform(get("/unexpected-error")),
                500, "INTERNAL_ERROR", "/unexpected-error");

        assertThat(problem.toString()).doesNotContain(SENSITIVE_VALUE, "IllegalStateException", "SELECT");
    }

    private JsonNode problem(ResultActions result, int status, String code, String path) throws Exception {
        String body = result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andReturn().getResponse().getContentAsString();
        JsonNode envelope = mapper.readTree(body);
        assertThat(envelope.size()).isEqualTo(3);
        assertThat(envelope.path("statusCode").asInt()).isEqualTo(status);
        JsonNode problem = envelope.get("data");
        assertThat(envelope.path("message").asText()).isEqualTo(problem.path("detail").asText());
        assertThat(problem.size()).isEqualTo(9);
        assertThat(problem.get("status").asInt()).isEqualTo(status);
        assertThat(problem.get("type").asText()).isEqualTo("urn:problem:" + code.toLowerCase().replace('_', '-'));
        assertThat(problem.get("title").asText()).isNotBlank();
        assertThat(problem.get("detail").asText()).isNotBlank();
        assertThat(problem.get("instance").asText()).isEqualTo(path);
        assertThat(problem.get("code").asText()).isEqualTo(code);
        assertThat(problem.get("field_errors").isArray()).isTrue();
        assertThat(UUID.fromString(problem.get("trace_id").asText())).isNotNull();
        assertThat(Instant.parse(problem.get("timestamp").asText())).isBeforeOrEqualTo(Instant.now());
        assertThat(problem.has("data")).isFalse();
        return problem;
    }

    private void assertFieldError(JsonNode problem, String field, String reason) {
        JsonNode fieldErrors = problem.get("field_errors");
        assertThat(fieldErrors.size()).isEqualTo(1);
        assertThat(fieldErrors.get(0).get("field").asText()).isEqualTo(field);
        assertThat(fieldErrors.get(0).get("reason").asText()).isEqualTo(reason);
    }

    @RestController
    static class InputController {
        @PostMapping("/input")
        Map<String, String> input(@Valid @RequestBody Input input) {
            return Map.of("result", "ok");
        }

        @GetMapping("/query")
        int query(@RequestParam("quantity") int quantity) {
            return quantity;
        }

        @GetMapping("/items/{id}")
        long path(@PathVariable("id") long id) {
            return id;
        }

        @GetMapping("/model")
        int model(@ModelAttribute QuantityQuery input) {
            return input.getQuantity();
        }

        @GetMapping("/validated-query")
        int validatedQuery(@RequestParam("quantity") @Min(1) int quantity) {
            return quantity;
        }

        @GetMapping("/bad-credentials")
        void badCredentials() {
            throw new BadCredentialsException(SENSITIVE_VALUE);
        }

        @GetMapping("/unknown-user")
        void unknownUser() {
            throw new UsernameNotFoundException(SENSITIVE_VALUE);
        }

        @GetMapping("/domain-not-found")
        void domainNotFound() {
            throw new ResourceNotFoundException("Room", 12L);
        }

        @GetMapping("/static-not-found")
        void staticNotFound() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, SENSITIVE_VALUE);
        }

        @GetMapping("/unexpected-error")
        void unexpectedError() {
            throw new IllegalStateException("SELECT password " + SENSITIVE_VALUE);
        }
    }

    record Input(@NotBlank(message = "Name is required") String name, Integer quantity) {
    }

    public static class QuantityQuery {
        private int quantity;

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}
