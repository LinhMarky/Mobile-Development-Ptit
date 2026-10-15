package com.example.roomly;

import com.example.roomly.data.model.ListingFee;
import org.junit.Test;
import static org.junit.Assert.*;

public class ListingFeeTest {
    @Test public void allContractCodesAreAccepted() {
        for (String code : ListingFee.CODES) assertEquals(code, new ListingFee(code, "FIXED", "", 0).getCode());
    }
    @Test public void perUnitTrimsAndKeepsUnit() {
        assertEquals("kWh", new ListingFee("ELECTRICITY", "PER_UNIT", " kWh ", 3500).getUnit());
    }
    @Test public void decimalAmountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ListingFee.parseAmount("1.5"));
    }
    @Test public void negativeAmountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ListingFee("WATER", "FIXED", "", -1));
    }
    @Test public void amountOverDatabaseLimitIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ListingFee("WATER", "FIXED", "", ListingFee.MAX_AMOUNT + 1));
    }
    @Test public void missingAmountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ListingFee.parseAmount(""));
    }
    @Test public void perUnitRequiresUnit() {
        assertThrows(IllegalArgumentException.class, () -> new ListingFee("WATER", "PER_UNIT", " ", 10000));
    }
    @Test public void longUnitIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ListingFee("WATER", "FIXED", new String(new char[51]).replace('\0', 'x'), 10000));
    }
    @Test public void invalidCodeAndModeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ListingFee("FAKE", "FIXED", "", 0));
        assertThrows(IllegalArgumentException.class, () -> new ListingFee("WATER", "FAKE", "", 0));
    }
    @Test public void maximumIntegerIsAccepted() {
        assertEquals(ListingFee.MAX_AMOUNT, ListingFee.parseAmount("999999999999999999"));
    }
}
