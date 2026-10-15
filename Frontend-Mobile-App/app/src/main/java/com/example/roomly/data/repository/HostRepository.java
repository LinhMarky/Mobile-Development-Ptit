package com.example.roomly.data.repository;

import com.example.roomly.data.api.ApiClient;
import com.example.roomly.data.api.ApiMapper;
import com.example.roomly.data.model.HostListing;
import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.RoomCard;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Keeps server results for navigation between the host's list and edit screens. */
public final class HostRepository {
    private static final HostRepository INSTANCE = new HostRepository();
    private final List<HostRoom> rooms = new ArrayList<>();
    private final List<HostListing> listings = new ArrayList<>();
    private final Map<String, JSONObject> listingDetails = new HashMap<>();
    private static final String[] AMENITIES = {
        "Wi-Fi",
        "Điều hòa",
        "Máy giặt",
        "Tủ lạnh",
        "Nóng lạnh",
        "Chỗ để xe",
        "Camera an ninh",
        "Ban công",
        "Bếp riêng",
        "Vệ sinh riêng",
        "Cho nuôi thú cưng",
        "Có nội thất",
        "Thang máy",
        "Phòng gym",
        "Hồ bơi"
    };
    private String owner = "";

    private HostRepository() {}

    public static HostRepository getInstance() {
        return INSTANCE;
    }

    private void checkOwner() {
        String current = SessionRepository.getInstance().getCurrentSession().getUserId();
        if (current == null) {
            current = "";
        }
        if (!current.equals(owner)) {
            rooms.clear();
            listings.clear();
            listingDetails.clear();
            owner = current;
        }
    }

    public HostRoom getMyRoomById(String id) {
        checkOwner();
        for (HostRoom room : rooms) if (room.getId().equals(id)) return room;
        return null;
    }

    public HostListing getMyListingById(String id) {
        checkOwner();
        for (HostListing listing : listings) if (listing.getId().equals(id)) return listing;
        return null;
    }

    public void fetchRooms(Consumer<List<HostRoom>> success, Consumer<String> failure) {
        checkOwner();
        ApiClient.execute(
                () -> {
                    JSONArray rows =
                            ((JSONObject) ApiClient.request("GET", "rooms", null))
                                    .getJSONArray("data");
                    List<HostRoom> result = new ArrayList<>();
                    for (int index = 0; index < rows.length(); index++)
                        result.add(mapRoom(rows.getJSONObject(index)));
                    return result;
                },
                result -> {
                    rooms.clear();
                    rooms.addAll(result);
                    success.accept(result);
                },
                failure);
    }

    public void fetchListings(Consumer<List<HostListing>> success, Consumer<String> failure) {
        fetchRooms(
                ignored ->
                        ApiClient.execute(
                                () -> {
                                    JSONArray rows =
                                            ((JSONObject)
                                                            ApiClient.request(
                                                                    "GET", "listings/my", null))
                                                    .getJSONArray("data");
                                    return rows;
                                },
                                rows -> {
                                    try {
                                        listings.clear();
                                        listingDetails.clear();
                                        for (int index = 0; index < rows.length(); index++) {
                                            JSONObject row = rows.getJSONObject(index);
                                            listings.add(mapListing(row));
                                            listingDetails.put(row.getString("id"), row);
                                        }
                                        success.accept(new ArrayList<>(listings));
                                    } catch (Exception exception) {
                                        failure.accept("Tin đăng từ máy chủ không hợp lệ.");
                                    }
                                },
                                failure),
                failure);
    }

    public void saveRoom(String id, JSONObject body, Runnable success, Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                id == null ? "POST" : "PUT",
                                id == null ? "rooms" : "rooms/" + id,
                                body),
                row -> fetchRooms(ignored -> success.run(), failure),
                failure);
    }

    public void saveRoom(
            String id,
            JSONObject body,
            String image,
            Consumer<String> success,
            Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        (JSONObject)
                                ApiClient.request(
                                        id == null ? "POST" : "PUT",
                                        id == null ? "rooms" : "rooms/" + id,
                                        body),
                row -> {
                    String roomId = row.optString("id");
                    ApiClient.execute(
                            () -> {
                                try {
                                    if (image != null
                                            && !image.isEmpty()
                                            && !image.startsWith("http")
                                            && !image.startsWith("/api/")) {
                                        ApiClient.request(
                                                "POST",
                                                "rooms/" + roomId + "/image",
                                                ApiClient.imageBody(image));
                                    } else if (id != null && (image == null || image.isEmpty())) {
                                        ApiClient.request(
                                                "DELETE", "rooms/" + roomId + "/image", null);
                                    }
                                    return "Đã lưu thông tin phòng.";
                                } catch (Exception exception) {
                                    return "Đã lưu phòng. Ảnh chưa tải lên: "
                                            + exception.getMessage();
                                }
                            },
                            text ->
                                    fetchRooms(
                                            ignored -> success.accept(text),
                                            error ->
                                                    success.accept(
                                                            text + " Hãy mở lại danh sách phòng.")),
                            failure);
                },
                failure);
    }

    public void saveListing(
            String room, JSONObject body, Runnable success, Consumer<String> failure) {
        ApiClient.execute(
                () -> ApiClient.request("POST", "listings/room/" + room, body),
                // Mutation đã thành công: lỗi tải lại danh sách không được biến thành yêu cầu lưu lại.
                row -> fetchListings(ignored -> success.run(), error -> success.run()),
                failure);
    }

    public void hideListing(String id, Runnable success, Consumer<String> failure) {
        ApiClient.execute(() -> ApiClient.request("POST", "listings/" + id + "/hide", new JSONObject()),
                row -> fetchListings(ignored -> success.run(), error -> success.run()), failure);
    }

    public void submit(String id, Runnable success, Consumer<String> failure) {
        ApiClient.execute(
                () -> ApiClient.request("POST", "listings/" + id + "/submit", new JSONObject()),
                row -> fetchListings(ignored -> success.run(), failure),
                failure);
    }

    /** null là chưa tải dữ liệu; [] là tin thực sự không có phụ phí. */
    public JSONArray getListingFees(String id) {
        JSONObject row = listingDetails.get(id);
        if (row == null || row.optJSONArray("fees") == null) return null;
        try {
            return new JSONArray(row.getJSONArray("fees").toString());
        } catch (org.json.JSONException error) {
            return null;
        }
    }

    public JSONObject listingBody(String id, String title, long rent, long deposit, String description) {
        JSONArray fees = getListingFees(id);
        if (fees == null) throw new IllegalStateException("Chưa tải được phụ phí. Hãy thử lại.");
        return listingBody(title, rent, deposit, description, fees);
    }

    public JSONObject listingBody(String title, long rent, long deposit, String description, JSONArray fees) {
        if (fees == null) throw new IllegalStateException("Chưa tải được phụ phí. Hãy thử lại.");
        return ApiClient.json("title", title, "rent_vnd", rent, "description", description,
                "deposit_vnd", deposit, "fees", fees);
    }

    public static JSONObject roomBody(
            String code,
            String name,
            String address,
            String area,
            String description,
            RoomCard.RoomType type,
            int occupants,
            List<String> amenities) {
        JSONArray ids = new JSONArray();
        for (String label : amenities) {
            for (int index = 0; index < AMENITIES.length; index++) {
                if (AMENITIES[index].equals(label)) {
                    ids.put(index + 1);
                    break;
                }
            }
        }
        String roomType =
                type == RoomCard.RoomType.ROOM
                        ? "SINGLE_ROOM"
                        : type == RoomCard.RoomType.APARTMENT ? "MINI_APARTMENT" : "STUDIO";
        return ApiClient.json(
                "unit_code",
                code,
                "name",
                name,
                "address_line",
                address,
                "area_m2",
                area,
                "description",
                description,
                "room_type",
                roomType,
                "max_occupants",
                occupants,
                "amenity_ids",
                ids);
    }

    private static HostRoom mapRoom(JSONObject row) throws Exception {
        String type = row.getString("room_type");
        List<String> amenities = new ArrayList<>();
        JSONArray ids = new JSONArray(row.getString("amenity_ids"));
        for (int index = 0; index < ids.length(); index++) {
            int id = ids.getInt(index);
            if (id > 0 && id <= AMENITIES.length) amenities.add(AMENITIES[id - 1]);
        }
        return new HostRoom(
                row.getString("id"),
                row.getString("host_id"),
                row.getString("unit_code"),
                row.getString("name"),
                row.getString("address_line"),
                new BigDecimal(row.getString("area_m2")),
                row.isNull("description") ? "" : row.optString("description"),
                row.optString("image_url", ""),
                type.equals("STUDIO")
                        ? RoomCard.RoomType.STUDIO
                        : type.equals("MINI_APARTMENT")
                                ? RoomCard.RoomType.APARTMENT
                                : RoomCard.RoomType.ROOM,
                row.getInt("max_occupants"),
                amenities,
                availability(row.optString("availability")));
    }

    private static RoomCard.Availability availability(String status) {
        try {
            return RoomCard.Availability.valueOf(status);
        } catch (IllegalArgumentException exception) {
            return RoomCard.Availability.UNKNOWN;
        }
    }

    private static HostListing mapListing(JSONObject row) throws Exception {
        String status = row.getString("status");
        HostListing.Status mapped = HostListing.Status.valueOf(status);
        return new HostListing(
                row.getString("id"),
                row.getString("host_id"),
                row.getString("room_id"),
                row.getString("title"),
                ApiMapper.money(row, "rent_vnd"),
                ApiMapper.money(row, "deposit_vnd"),
                row.optString("description"),
                ApiMapper.timestamp(row.getString("created_at")),
                mapped,
                row.isNull("moderation_reason") ? "" : row.optString("moderation_reason"));
    }
}
