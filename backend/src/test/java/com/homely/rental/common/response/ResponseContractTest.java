package com.homely.rental.common.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.security.*;
import com.homely.rental.catalog.controller.AmenityController;
import com.homely.rental.catalog.entity.*;
import com.homely.rental.catalog.repository.*;
import com.homely.rental.chat.service.ChatService;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.config.ApiWebConfig;
import com.homely.rental.common.exception.GlobalException;
import com.homely.rental.common.idempotency.IdempotencyKeyRepository;
import com.homely.rental.media.controller.MediaController;
import com.homely.rental.media.entity.*;
import com.homely.rental.media.repository.MediaRepository;
import com.homely.rental.media.service.*;
import com.homely.rental.media.validation.FileValidator;
import com.homely.rental.notification.controller.NotificationController;
import com.homely.rental.notification.dto.NotificationDTO;
import com.homely.rental.notification.service.NotificationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.*;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercise real HTTP serialization and filters with a nondefault API prefix. */
@WebMvcTest(controllers={MediaController.class, AmenityController.class, NotificationController.class}, properties="apiPrefix=custom/v2")
@Import({SecurityConfig.class, GlobalException.class, MediaService.class, ApiWebConfig.class})
class ResponseContractTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockBean MediaRepository media;
    @MockBean StorageService storage;
    @MockBean FileValidator validator;
    @MockBean UserResolver users;
    @MockBean RoomRepository rooms;
    @MockBean ListingRepository listings;
    @MockBean AmenityRepository amenities;
    @MockBean NotificationService inbox;
    @MockBean AccountAccessService accounts;
    @MockBean ChatService chat;
    @MockBean IdempotencyKeyRepository keys;
    @MockBean JwtDecoder decoder;
    User owner;
    Media photo;
    Room room;
    Listing listing;
    static final byte[] IMAGE = {(byte)0xff, (byte)0xd8, (byte)0xff, (byte)0xd9};

    @BeforeEach void fixture() {
        owner = new User(); owner.setId(1L); owner.setEmail("host@example.test");
        photo = new Media(); photo.setId(7L); photo.setUploader(owner); photo.setPurpose(MediaPurpose.ROOM_PHOTO);
        photo.setContentType("image/jpeg"); photo.setFileSizeBytes(IMAGE.length); photo.setStorageKey("private/photo.jpg");
        photo.setStatus(MediaStatus.ATTACHED); photo.setResourceType("room"); photo.setResourceId(10L);
        room = new Room(); room.setId(10L); room.setHost(owner);
        listing = new Listing(); listing.setRoom(room); listing.setStatus(ListingStatus.PUBLISHED);
        listing.setExpiresAt(Instant.now().plusSeconds(3600));
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg", "HS256")
                .subject(owner.getEmail()).claim("token_type", "access").claim("roles", List.of("ROLE_HOST"))
                .claim("user_id", owner.getId())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build());
        when(accounts.requireActive(owner.getEmail())).thenReturn(owner);
        when(users.requireCurrent()).thenReturn(owner);
        when(media.findById(photo.getId())).thenReturn(Optional.of(photo));
        when(rooms.findById(room.getId())).thenReturn(Optional.of(room));
        when(listings.findByRoomId(room.getId())).thenReturn(Optional.of(listing));
        when(media.findByResourceTypeAndResourceIdAndStatusOrderByDisplayOrder("room", 10L, MediaStatus.ATTACHED)).thenReturn(List.of(photo));
        when(storage.open(photo.getStorageKey())).thenAnswer(call -> new ByteArrayInputStream(IMAGE));
    }

    @Test void collectionAndObjectResponsesAreWrappedOnce() throws Exception {
        Amenity amenity = new Amenity(); amenity.setId(1L); amenity.setName("Wi-Fi");
        when(amenities.findAll()).thenReturn(List.of(amenity)); when(amenities.findById(1L)).thenReturn(Optional.of(amenity));
        mvc.perform(get("/custom/v2/amenities")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].name").value("Wi-Fi"));
        mvc.perform(get("/custom/v2/amenities/1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.data").doesNotExist()).andExpect(jsonPath("$.data.statusCode").doesNotExist());
    }

    @Test void paginationHasExactlyOnePageEnvelope() throws Exception {
        when(inbox.getMyNotifications(any())).thenReturn(PageResponse.<NotificationDTO>builder().data(List.of())
                .totalElements(0).totalPages(0).currentPage(0).pageSize(20).build());
        mvc.perform(get("/custom/v2/notifications").header("Authorization", "Bearer access"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.data").isArray()).andExpect(jsonPath("$.data.page_size").value(20))
                .andExpect(jsonPath("$.statusCode").value(200)).andExpect(jsonPath("$.data.message").doesNotExist());
    }

    @Test void deletedMediaReturnsAnEmpty204() throws Exception {
        photo.setStatus(MediaStatus.READY);
        mvc.perform(delete("/custom/v2/media/7").header("Authorization", "Bearer access"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(photo.getStatus()).isEqualTo(MediaStatus.DELETED); verify(storage).delete(photo.getStorageKey());
    }

    @Test void mediaUrlUsesConfiguredPrefixAndCanBeReadWithoutWrappingBytes() throws Exception {
        var response = mvc.perform(get("/custom/v2/media/room/10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].url").value("/custom/v2/media/7/content")).andReturn();
        String url = mapper.readTree(response.getResponse().getContentAsString()).get("data").get(0).get("url").asText();
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(IMAGE)).andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test void multipartUploadReturnsCreatedDtoWithConfiguredUrl() throws Exception {
        when(media.save(any(Media.class))).thenAnswer(call -> { Media saved = call.getArgument(0); saved.setId(8L); return saved; });
        mvc.perform(multipart("/custom/v2/media/upload").file(new MockMultipartFile("file", "photo.jpg", "image/jpeg", IMAGE))
                        .param("purpose", "ROOM_PHOTO").header("Authorization", "Bearer access"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.url").value("/custom/v2/media/8/content"))
                .andExpect(jsonPath("$.data.status").value("READY")).andExpect(jsonPath("$.data.data").doesNotExist());
    }

    @Test void mediaPermissionChecksStillProtectHiddenAndUnattachedContent() throws Exception {
        listing.setStatus(ListingStatus.HIDDEN);
        problem(mvc.perform(get("/custom/v2/media/7/content")), 404, "RESOURCE_NOT_FOUND");
        photo.setStatus(MediaStatus.READY);
        problem(mvc.perform(get("/custom/v2/media/7/content")), 404, "RESOURCE_NOT_FOUND");
        verify(storage, never()).open(anyString());
    }

    @Test void securityAccountIdempotencyAndMvcUseIdenticalProblemShape() throws Exception {
        problem(mvc.perform(get("/custom/v2/notifications").queryParam("token", "sensitive")), 401, "AUTHENTICATION_REQUIRED");
        problem(mvc.perform(get("/custom/v2/amenities/999").queryParam("token", "sensitive")), 404, "RESOURCE_NOT_FOUND");
        problem(mvc.perform(post("/custom/v2/notifications/read-all").header("Authorization", "Bearer access")), 409, "IDEMPOTENCY_KEY_REQUIRED");
        when(accounts.requireActive(owner.getEmail())).thenThrow(new AccountAccessException(403, "ACCOUNT_INACTIVE", "Account is inactive"));
        problem(mvc.perform(get("/custom/v2/notifications").header("Authorization", "Bearer access")), 403, "ACCOUNT_INACTIVE");
    }

    @Test void wrongMethodReturns405WithAllowHeader() throws Exception {
        var result = mvc.perform(delete("/custom/v2/amenities").header("Authorization", "Bearer access"));
        problem(result, 405, "METHOD_NOT_ALLOWED"); result.andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("GET")));
    }

    @Test void unsupportedRequestMediaTypeReturns415() throws Exception {
        problem(mvc.perform(post("/custom/v2/media/attach/room/10").header("Authorization", "Bearer access")
                .contentType(MediaType.TEXT_PLAIN).content("invalid")), 415, "UNSUPPORTED_MEDIA_TYPE");
    }

    @Test void unavailableResponseMediaTypeReturns406() throws Exception {
        problem(mvc.perform(get("/custom/v2/amenities").accept(MediaType.APPLICATION_XML)), 406, "NOT_ACCEPTABLE");
    }

    @Test void validationKeepsFieldErrorsWithoutRejectedValues() throws Exception {
        var body = problem(mvc.perform(get("/custom/v2/notifications").header("Authorization", "Bearer access")
                .param("size", "0").param("token", "sensitive")), 400, "VALIDATION_FAILED");
        assertThat(body.get("field_errors").get(0).has("field")).isTrue();
        assertThat(body.get("field_errors").get(0).has("reason")).isTrue();
    }

    private JsonNode problem(ResultActions result, int status, String code) throws Exception {
        var response = result.andExpect(status().is(status)).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();
        JsonNode envelope = mapper.readTree(response.getContentAsString());
        assertThat(envelope.size()).isEqualTo(3);
        assertThat(envelope.path("statusCode").asInt()).isEqualTo(status);
        assertThat(envelope.path("message").asText()).isNotBlank();
        JsonNode body = envelope.get("data");
        assertThat(body.size()).isEqualTo(9); assertThat(body.get("status").asInt()).isEqualTo(status);
        assertThat(body.get("code").asText()).isEqualTo(code); assertThat(body.get("field_errors").isArray()).isTrue();
        assertThat(body.get("type").asText()).isEqualTo("urn:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-'));
        assertThat(UUID.fromString(body.get("trace_id").asText())).isNotNull();
        assertThat(Instant.parse(body.get("timestamp").asText())).isBeforeOrEqualTo(Instant.now());
        assertThat(body.toString()).doesNotContain("sensitive", "statusCode", "stackTrace");
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        return body;
    }
}
