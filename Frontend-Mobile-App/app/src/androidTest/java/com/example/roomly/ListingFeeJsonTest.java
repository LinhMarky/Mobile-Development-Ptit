package com.example.roomly;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.roomly.data.model.ListingFee;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import static org.junit.Assert.*;

/** Dùng org.json thật trên Android, không dùng stub của local JVM. */
@RunWith(AndroidJUnit4.class)
public class ListingFeeJsonTest {
    @Test public void nullIsNotAnEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> ListingFee.fromJson(null));
    }
    @Test public void explicitEmptyListIsValid() throws Exception {
        assertTrue(ListingFee.fromJson(new JSONArray()).isEmpty());
    }
    @Test public void feesRoundTripWithoutLosingContractFields() throws Exception {
        ListingFee original = new ListingFee("ELECTRICITY", "PER_UNIT", "kWh", 3500);
        JSONArray json = ListingFee.toJson(java.util.Collections.singletonList(original));
        ListingFee result = ListingFee.fromJson(json).get(0);
        assertEquals("ELECTRICITY", result.getCode());
        assertEquals("PER_UNIT", result.getMode());
        assertEquals("kWh", result.getUnit());
        assertEquals(3500, result.getAmount());
    }
    @Test public void duplicateCodesDoNotOverwriteData() throws Exception {
        JSONObject fee = new ListingFee("WATER", "FIXED", "tháng", 50000).toJson();
        assertThrows(IllegalArgumentException.class, () -> ListingFee.fromJson(new JSONArray().put(fee).put(fee)));
    }
    @Test public void decimalFromServerIsNotSilentlyRounded() throws Exception {
        JSONObject fee = new ListingFee("WATER", "FIXED", "tháng", 50000).toJson().put("amount_vnd", "1.5");
        assertThrows(ArithmeticException.class, () -> ListingFee.fromJson(new JSONArray().put(fee)));
    }
}
