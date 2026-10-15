package com.homely.student14.service;

import com.homely.student14.dto.request.catalog.ListingFeeDTO;
import com.homely.student14.dto.request.catalog.SaveListingDTO;
import com.homely.student14.repository.CatalogRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogFeeTest {
    @Mock CatalogRepository repository;
    @Mock AuthService auth;
    @Mock Jwt jwt;
    CatalogService service;

    @BeforeEach void setup() {
        service = new CatalogService(repository, auth);
        when(auth.role(jwt, "host")).thenReturn(Map.of("id", 7L));
        when(repository.findRoomByIdForUpdate(41L)).thenReturn(new HashMap<>(Map.of(
                "id", 41L, "host_id", 7L, "availability", "AVAILABLE")));
    }

    @Test void perUnitRequiresUnitBeforeAnyWrite() {
        assertInvalid(fee("WATER", "PER_UNIT", " ", "5000"));
    }
    @Test void duplicateCodesAreRejectedBeforeAnyWrite() {
        assertInvalid(fee("WATER", "FIXED", "", "5000"), fee("WATER", "PER_UNIT", "m3", "10000"));
    }
    @Test void fractionalDongIsRejectedBeforeAnyWrite() {
        assertInvalid(fee("WATER", "FIXED", "", "0.1"));
    }
    @Test void negativeAmountIsRejectedBeforeAnyWrite() {
        assertInvalid(fee("WATER", "FIXED", "", "-1"));
    }
    @Test void amountBeyondDatabasePrecisionIsRejected() {
        assertInvalid(fee("WATER", "FIXED", "", "1000000000000000000"));
    }
    @Test void validFeesRetainCodeModeAndTrimmedUnit() {
        ListingFeeDTO fee = fee("ELECTRICITY", "PER_UNIT", " kWh ", "3500");
        SaveListingDTO request = request(fee);
        when(repository.createListing(41L, request)).thenReturn(55L);
        when(repository.findListingById(55L)).thenReturn(new HashMap<>(Map.of(
                "id", 55L, "room_id", 41L, "status", "PUBLISHED")));
        service.saveListing(41L, jwt, request);
        verify(repository).createListing(41L, request);
        assertEquals("kWh", fee.getUnitName());
        assertEquals("PER_UNIT", fee.getFeeMode());
        assertEquals(new BigDecimal("3500"), fee.getAmountVnd());
    }

    private void assertInvalid(ListingFeeDTO... fees) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.saveListing(41L, jwt, request(fees)));
        assertEquals(400, error.getStatusCode().value());
        verify(repository, never()).createListing(anyLong(), any());
        verify(repository, never()).updateListing(anyLong(), any());
    }
    private SaveListingDTO request(ListingFeeDTO... fees) {
        SaveListingDTO request = new SaveListingDTO();
        request.setTitle("Tin phụ phí");
        request.setDescription("Kiểm tra dữ liệu phụ phí");
        request.setRentVnd(new BigDecimal("3000000"));
        request.setDepositVnd(BigDecimal.ZERO);
        request.setFees(List.of(fees));
        return request;
    }
    private ListingFeeDTO fee(String code, String mode, String unit, String amount) {
        ListingFeeDTO fee = new ListingFeeDTO();
        fee.setFeeCode(code);
        fee.setFeeMode(mode);
        fee.setUnitName(unit);
        fee.setAmountVnd(new BigDecimal(amount));
        return fee;
    }
}
