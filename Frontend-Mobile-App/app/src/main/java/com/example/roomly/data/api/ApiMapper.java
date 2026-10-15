package com.example.roomly.data.api;

import com.example.roomly.data.model.Booking;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.RoomImage;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class ApiMapper {
    private ApiMapper() {}

    public static SessionState session(JSONObject user) throws JSONException {
        EnumSet<UserRole> roles = EnumSet.noneOf(UserRole.class);
        JSONArray values = user.getJSONArray("roles");
        for (int index = 0; index < values.length(); index++) {
            roles.add(UserRole.valueOf(values.getString(index).replace("ROLE_", "")));
        }
        return SessionState.authenticated(
                user.getString("id"),
                user.optString("full_name"),
                user.getString("email"),
                user.optBoolean("email_verified"),
                roles);
    }

    public static List<RoomCard> rooms(JSONObject page) throws JSONException {
        List<RoomCard> rooms = new ArrayList<>();
        JSONArray rows = page.getJSONArray("data");
        for (int index = 0; index < rows.length(); index++)
            rooms.add(room(rows.getJSONObject(index)));
        return rooms;
    }

    public static RoomCard room(JSONObject row) throws JSONException {
        String type = row.getString("room_type");
        RoomCard.RoomType roomType =
                type.equals("SINGLE_ROOM")
                        ? RoomCard.RoomType.ROOM
                        : type.equals("MINI_APARTMENT")
                                ? RoomCard.RoomType.APARTMENT
                                : RoomCard.RoomType.STUDIO;
        long rent = money(row, "rent_vnd");
        RoomCard room =
                new RoomCard(
                        row.getString("title"),
                        vnd(rent) + "/tháng",
                        row.optString("address_line", ""),
                        row.getString("area_m2") + " m²",
                        android.R.drawable.ic_menu_gallery,
                        false,
                        roomType,
                        row.getString("room_id"),
                        row.getString("host_id"),
                        row.getString("unit_code"));
        room.setListingId(row.getString("id"));
        room.setDescription(row.optString("description"));
        room.setHostName(row.optString("host_name"));
        room.setMonthlyRent(rent);
        room.setMaxOccupants(row.optInt("max_occupants", 10));
        room.setDepositVnd(money(row, "deposit_vnd"));
        room.setArea(new BigDecimal(row.getString("area_m2")));
        room.setAvailability(RoomCard.Availability.valueOf(row.getString("availability")));
        List<RoomCard.Fee> fees = new ArrayList<>();
        JSONArray rows = row.optJSONArray("fees");
        for (int index = 0; rows != null && index < rows.length(); index++) {
            JSONObject fee = rows.getJSONObject(index);
            fees.add(
                    new RoomCard.Fee(
                            feeName(fee.optString("fee_code")),
                            money(fee, "amount_vnd"),
                            fee.optString("unit_name")));
        }
        room.setFees(fees);
        List<RoomImage> images = new ArrayList<>();
        JSONArray photos = row.optJSONArray("images");
        for (int index = 0; photos != null && index < photos.length(); index++) {
            String path = photos.getJSONObject(index).optString("url");
            if (!path.isEmpty()) images.add(RoomImage.fromUri(path, "Ảnh " + room.getTitle()));
        }
        room.setImages(images);
        if (!images.isEmpty()) room.setImageUri(images.get(0).getUri());
        return room;
    }

    public static String feeName(String code) {
        switch (code) {
            case "ELECTRICITY":
                return "Điện";
            case "WATER":
                return "Nước";
            case "INTERNET":
                return "Internet";
            case "PARKING":
                return "Giữ xe";
            case "CLEANING":
                return "Vệ sinh";
            case "SERVICE":
                return "Dịch vụ";
            default:
                return "Phí khác";
        }
    }

    public static String feeSummary(JSONObject booking) {
        JSONObject terms = booking.optJSONObject("terms_snapshot");
        JSONArray fees = terms == null ? null : terms.optJSONArray("fees");
        if (fees == null || fees.length() == 0) return "Không có phụ phí bổ sung.";
        StringBuilder result = new StringBuilder("Phụ phí đã chốt:");
        for (int index = 0; index < fees.length(); index++) {
            JSONObject fee = fees.optJSONObject(index);
            if (fee == null) continue;
            long amount = new BigDecimal(fee.optString("amount_vnd", "0")).longValueExact();
            result.append("\n")
                    .append(feeName(fee.optString("fee_code")))
                    .append(": ")
                    .append(vnd(amount));
            String unit = fee.optString("unit_name", "");
            if (!unit.isEmpty()) result.append("/").append(unit);
        }
        return result.toString();
    }

    public static List<Booking> bookings(JSONObject page) throws JSONException {
        List<Booking> bookings = new ArrayList<>();
        JSONArray rows = page.getJSONArray("data");
        for (int index = 0; index < rows.length(); index++)
            bookings.add(booking(rows.getJSONObject(index)));
        return bookings;
    }

    public static Booking booking(JSONObject row) throws JSONException {
        String status = row.getString("status");
        if (status.equals("CANCELLED")
                && "HOST_REJECTED".equals(row.optString("cancel_reason_code"))) status = "REJECTED";
        return new Booking(
                row.getString("id"),
                row.getString("room_id"),
                row.getString("room_title"),
                row.optString("room_address"),
                money(row, "rent_vnd"),
                money(row, "deposit_vnd"),
                date(row.getString("desired_move_in")),
                row.getInt("occupant_count"),
                row.isNull("note") ? "" : row.optString("note"),
                Booking.Status.valueOf(status),
                timestamp(row.getString("created_at")),
                false);
    }

    public static long money(JSONObject row, String field) throws JSONException {
        return new BigDecimal(row.getString(field)).longValueExact();
    }

    public static String vnd(long amount) {
        return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(amount) + " ₫";
    }

    public static long date(String value) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value).getTime();
        } catch (ParseException exception) {
            throw new IllegalArgumentException("Ngày từ máy chủ không hợp lệ.", exception);
        }
    }

    public static long timestamp(String value) {
        try {
            String normalized = value.replaceFirst("\\.([0-9]{1,9})Z$", "Z");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(normalized).getTime();
        } catch (ParseException exception) {
            throw new IllegalArgumentException("Thời gian từ máy chủ không hợp lệ.", exception);
        }
    }
}
