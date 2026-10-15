package com.homely.student14.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.homely.student14.dto.request.catalog.ListingActionDTO;
import com.homely.student14.dto.request.catalog.SaveListingDTO;
import com.homely.student14.repository.CatalogRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogServicePolicyTest {
    @Mock CatalogRepository repository;
    @Mock AuthService authService;
    @Mock Jwt jwt;
    CatalogService service;
    Map<String, Object> listing;
    Map<String, Object> actor;

    @BeforeEach void setup() {
        service = new CatalogService(repository, authService);
        actor = new HashMap<>(Map.of("id", 7L, "is_admin", false));
        listing = new HashMap<>(Map.of("id", 55L, "room_id", 41L, "host_id", 7L,
                                      "status", "DRAFT", "fees", "[]", "amenity_ids", "[]"));
        when(authService.user(jwt)).thenReturn(actor);
        when(authService.role(jwt, "host")).thenReturn(actor);
        when(repository.findRoomByIdForUpdate(41L)).thenReturn(new HashMap<>(Map.of(
                "id", 41L, "host_id", 7L, "availability", "AVAILABLE")));
        when(repository.findRoomListingsForUpdate(41L)).thenReturn(List.of(listing));
        when(repository.findListingByIdForUpdate(55L)).thenReturn(listing);
        when(repository.findListingById(55L)).thenAnswer(call -> new HashMap<>(listing));
        doAnswer(call -> { listing.put("status", call.getArgument(1)); return null; })
                .when(repository).updateListingStatus(anyLong(), anyString(), anyString());
        doAnswer(call -> { listing.put("status", "DRAFT"); return null; })
                .when(repository).updateListing(anyLong(), any());
    }

    @ParameterizedTest
    @CsvSource({"DRAFT,true", "PENDING_REVIEW,false", "PUBLISHED,true", "HIDDEN,true",
                "REJECTED,true", "SUSPENDED,false", "EXPIRED,true", "ARCHIVED,false"})
    void hostEditFollowsPolicy(String status, boolean allowed) {
        listing.put("status", status);
        SaveListingDTO request = new SaveListingDTO();
        request.setTitle("Tin kiểm tra");
        request.setDescription("Mô tả kiểm tra");
        request.setRentVnd(new BigDecimal("3000000"));
        request.setDepositVnd(BigDecimal.ZERO);
        if (allowed) {
            assertEquals("DRAFT", service.saveListing(41L, jwt, request).get("status"));
            verify(repository).updateListing(55L, request);
        } else {
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> service.saveListing(41L, jwt, request));
            assertEquals(409, error.getStatusCode().value());
            verify(repository, never()).updateListing(anyLong(), any());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "DRAFT,submit,PENDING_REVIEW", "DRAFT,hide,BLOCKED",
        "PENDING_REVIEW,submit,BLOCKED", "PENDING_REVIEW,hide,BLOCKED",
        "PUBLISHED,submit,BLOCKED", "PUBLISHED,hide,HIDDEN",
        "HIDDEN,submit,PENDING_REVIEW", "HIDDEN,hide,BLOCKED",
        "REJECTED,submit,PENDING_REVIEW", "REJECTED,hide,BLOCKED",
        "SUSPENDED,submit,BLOCKED", "SUSPENDED,hide,BLOCKED",
        "EXPIRED,submit,PENDING_REVIEW", "EXPIRED,hide,BLOCKED"
    })
    void hostTransitionsFollowPolicy(String status, String action, String expected) {
        checkAction(status, action, expected);
    }

    @ParameterizedTest
    @CsvSource({
        "DRAFT,approve,BLOCKED", "DRAFT,reject,BLOCKED", "DRAFT,suspend,BLOCKED",
        "PENDING_REVIEW,approve,PUBLISHED", "PENDING_REVIEW,reject,REJECTED", "PENDING_REVIEW,suspend,BLOCKED",
        "PUBLISHED,approve,BLOCKED", "PUBLISHED,reject,BLOCKED", "PUBLISHED,suspend,SUSPENDED",
        "HIDDEN,approve,BLOCKED", "HIDDEN,reject,BLOCKED", "HIDDEN,suspend,SUSPENDED",
        "REJECTED,approve,BLOCKED", "REJECTED,reject,BLOCKED", "REJECTED,suspend,BLOCKED",
        "SUSPENDED,approve,BLOCKED", "SUSPENDED,reject,BLOCKED", "SUSPENDED,suspend,BLOCKED",
        "EXPIRED,approve,BLOCKED", "EXPIRED,reject,BLOCKED", "EXPIRED,suspend,BLOCKED"
    })
    void adminTransitionsFollowPolicy(String status, String action, String expected) {
        actor.put("is_admin", true);
        checkAction(status, action, expected);
    }

    @Test void hostCannotApproveOwnListing() {
        listing.put("status", "PENDING_REVIEW");
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.listingAction(55L, jwt, "approve", actionRequest()));
        assertEquals(403, error.getStatusCode().value());
        verify(repository, never()).updateListingStatus(anyLong(), anyString(), anyString());
    }

    private void checkAction(String status, String action, String expected) {
        listing.put("status", status);
        if ("BLOCKED".equals(expected)) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> service.listingAction(55L, jwt, action, actionRequest()));
            assertEquals(409, error.getStatusCode().value());
            verify(repository, never()).updateListingStatus(anyLong(), anyString(), anyString());
        } else {
            assertEquals(expected, service.listingAction(55L, jwt, action, actionRequest()).get("status"));
            verify(repository).updateListingStatus(55L, expected, "Lý do kiểm tra");
        }
    }

    private ListingActionDTO actionRequest() {
        ListingActionDTO request = new ListingActionDTO();
        request.setReason("Lý do kiểm tra");
        return request;
    }
}
