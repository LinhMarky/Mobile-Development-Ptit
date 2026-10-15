package com.example.roomly.data.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Một khoản phụ phí theo contract của tin đăng, số tiền là đồng Việt Nam nguyên. */
public class ListingFee {
    public static final List<String> CODES = Arrays.asList(
            "ELECTRICITY", "WATER", "INTERNET", "PARKING", "CLEANING", "SERVICE", "OTHER");
    public static final long MAX_AMOUNT = 999999999999999999L;

    private final String code;
    private final String mode;
    private final String unit;
    private final long amount;

    public ListingFee(String code, String mode, String unit, long amount) {
        if (!CODES.contains(code)) throw new IllegalArgumentException("Loại phụ phí không hợp lệ.");
        if (!"FIXED".equals(mode) && !"PER_UNIT".equals(mode)) {
            throw new IllegalArgumentException("Cách tính phụ phí không hợp lệ.");
        }
        String normalizedUnit = unit == null ? "" : unit.trim();
        if (normalizedUnit.length() > 50) throw new IllegalArgumentException("Đơn vị tối đa 50 ký tự.");
        if ("PER_UNIT".equals(mode) && normalizedUnit.isEmpty()) {
            throw new IllegalArgumentException("Nhập đơn vị tính, ví dụ kWh, m³ hoặc người/tháng.");
        }
        if (amount < 0 || amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("Số tiền phải là số nguyên không âm, tối đa 18 chữ số.");
        }
        this.code = code;
        this.mode = mode;
        this.unit = normalizedUnit;
        this.amount = amount;
    }

    public String getCode() { return code; }
    public String getMode() { return mode; }
    public String getUnit() { return unit; }
    public long getAmount() { return amount; }

    public static long parseAmount(String text) {
        if (text == null || !text.matches("[0-9]{1,18}")) {
            throw new IllegalArgumentException("Nhập số tiền nguyên không âm, tối đa 18 chữ số.");
        }
        return Long.parseLong(text);
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("fee_code", code).put("fee_mode", mode)
                .put("unit_name", unit).put("amount_vnd", amount);
    }

    public static List<ListingFee> fromJson(JSONArray rows) throws JSONException {
        if (rows == null) throw new IllegalArgumentException("Chưa tải được phụ phí. Hãy thử lại.");
        List<ListingFee> fees = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        for (int index = 0; index < rows.length(); index++) {
            JSONObject row = rows.getJSONObject(index);
            ListingFee fee = new ListingFee(row.getString("fee_code"), row.getString("fee_mode"),
                    row.optString("unit_name", ""), new BigDecimal(row.getString("amount_vnd")).longValueExact());
            if (!codes.add(fee.code)) throw new IllegalArgumentException("Loại phụ phí bị trùng.");
            fees.add(fee);
        }
        return fees;
    }

    public static JSONArray toJson(List<ListingFee> fees) throws JSONException {
        JSONArray rows = new JSONArray();
        for (ListingFee fee : fees) rows.put(fee.toJson());
        return rows;
    }
}
