package com.homely.rental.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.common.dto.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FormatRestResponseTest {
    private MockMvc mvc;
    @BeforeEach void setUp() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        mvc = MockMvcBuilders.standaloneSetup(new Responses())
                .setControllerAdvice(new FormatRestResponse(mapper))
                .setMessageConverters(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(), new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @Test void stringConverterWritesAJsonEnvelopeAndPreservesCreatedStatusAndLocation() throws Exception {
        mvc.perform(get("/text")).andExpect(status().isCreated()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Location", "/text/7")).andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Created")).andExpect(jsonPath("$.data").value("Hello \"world\""));
    }

    @Test void existingEnvelopeIsNotWrappedAgain() throws Exception {
        mvc.perform(get("/already")).andExpect(status().isOk()).andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(7)).andExpect(jsonPath("$.data.statusCode").doesNotExist());
    }

    @Test void null200IsWrappedBut204And304KeepAnEmptyBody() throws Exception {
        mvc.perform(get("/empty")).andExpect(status().isOk()).andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(get("/deleted")).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get("/not-modified")).andExpect(status().isNotModified()).andExpect(content().string(""));
    }

    @Test void binaryBytesAreNotSerializedAsAnEnvelope() throws Exception {
        mvc.perform(get("/binary")).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(new byte[]{0, 1, 2, 3}));
    }

    @RestController static class Responses {
        @GetMapping("/text") ResponseEntity<String> text() { return ResponseEntity.status(201).header("Location", "/text/7").body("Hello \"world\""); }
        @GetMapping("/already") RestResponse<?> already() { return RestResponse.of(200, Map.of("id", 7)); }
        @GetMapping("/empty") ResponseEntity<Void> empty() { return ResponseEntity.ok().build(); }
        @GetMapping("/deleted") ResponseEntity<Void> deleted() { return ResponseEntity.noContent().build(); }
        @GetMapping("/not-modified") ResponseEntity<Void> notModified() { return ResponseEntity.status(304).build(); }
        @GetMapping("/binary") ResponseEntity<byte[]> binary() { return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(new byte[]{0, 1, 2, 3}); }
    }
}
