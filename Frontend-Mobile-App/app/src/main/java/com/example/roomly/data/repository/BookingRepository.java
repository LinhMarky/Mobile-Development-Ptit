package com.example.roomly.data.repository;

import com.example.roomly.data.api.ApiClient;
import com.example.roomly.data.api.ApiMapper;
import com.example.roomly.data.model.Booking;

import org.json.JSONObject;

import java.util.List;
import java.util.function.Consumer;

public final class BookingRepository {
    private static final java.util.Map<String, Booking> cached = new java.util.HashMap<>();
    private static String owner = "";

    public static Booking cachedBooking(String id) {
        checkOwner();
        return cached.get(id);
    }

    private static void checkOwner() {
        String current = SessionRepository.getInstance().getCurrentSession().getUserId();
        if (current == null) {
            current = "";
        }
        if (!current.equals(owner)) {
            cached.clear();
            owner = current;
        }
    }

    public void fetchBookings(
            boolean host, Consumer<List<Booking>> success, Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        ApiMapper.bookings(
                                (JSONObject)
                                        ApiClient.request(
                                                "GET",
                                                host ? "bookings/host" : "bookings/my",
                                                null)),
                success,
                failure);
    }

    public void fetchBooking(String id, Consumer<JSONObject> success, Consumer<String> failure) {
        ApiClient.execute(
                () -> (JSONObject) ApiClient.request("GET", "bookings/" + id, null),
                row -> {
                    try {
                        checkOwner();
                        cached.put(id, ApiMapper.booking(row));
                        success.accept(row);
                    } catch (Exception exception) {
                        failure.accept("Yêu cầu thuê từ máy chủ không hợp lệ.");
                    }
                },
                failure);
    }

    public void create(
            String room,
            String moveIn,
            int occupants,
            String note,
            Consumer<Booking> success,
            Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        ApiMapper.booking(
                                (JSONObject)
                                        ApiClient.request(
                                                "POST",
                                                "bookings",
                                                ApiClient.json(
                                                        "room_id",
                                                        room,
                                                        "desired_move_in",
                                                        moveIn,
                                                        "occupant_count",
                                                        occupants,
                                                        "note",
                                                        note))),
                success,
                failure);
    }

    public void action(
            String id,
            String action,
            JSONObject body,
            Consumer<JSONObject> success,
            Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        (JSONObject)
                                ApiClient.request(
                                        "POST",
                                        "bookings/" + id + "/" + action,
                                        body == null ? new JSONObject() : body),
                success,
                failure);
    }
}
