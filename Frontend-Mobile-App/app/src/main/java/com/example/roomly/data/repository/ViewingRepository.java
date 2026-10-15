package com.example.roomly.data.repository;

import com.example.roomly.data.api.ApiClient;
import com.example.roomly.data.api.ApiMapper;
import com.example.roomly.data.model.HostViewingAppointment;
import com.example.roomly.data.model.HostViewingSlot;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.ViewingAppointment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

public final class ViewingRepository {
    private static final ViewingRepository INSTANCE = new ViewingRepository();
    private final List<ViewingAppointment> tenantAppointments = new ArrayList<>();
    private final List<HostViewingAppointment> hostAppointments = new ArrayList<>();

    private ViewingRepository() {}

    public static ViewingRepository getInstance() {
        return INSTANCE;
    }

    public ViewingAppointment getTenantAppointment(String id) {
        for (ViewingAppointment item : tenantAppointments) if (item.getId().equals(id)) return item;
        return null;
    }

    public HostViewingAppointment getHostAppointment(String id) {
        for (HostViewingAppointment item : hostAppointments)
            if (item.getId().equals(id)) return item;
        return null;
    }

    public void fetchTenantAppointments(
            Consumer<List<ViewingAppointment>> success, Consumer<String> failure) {
        ApiClient.execute(
                () -> {
                    JSONArray rows =
                            ((JSONObject) ApiClient.request("GET", "viewings/my", null))
                                    .getJSONArray("data");
                    List<ViewingAppointment> result = new ArrayList<>();
                    for (int index = 0; index < rows.length(); index++) {
                        JSONObject row = rows.getJSONObject(index);
                        RoomCard room =
                                new RoomCard(
                                        row.optString("title"),
                                        ApiMapper.vnd(ApiMapper.money(row, "rent_vnd")),
                                        row.optString("address_line"),
                                        "",
                                        android.R.drawable.ic_menu_gallery,
                                        false,
                                        RoomCard.RoomType.ROOM,
                                        row.getString("room_id"),
                                        row.getString("host_id"),
                                        row.getString("unit_code"));
                        room.setListingId(row.optString("listing_id"));
                        String status = status(row);
                        result.add(
                                new ViewingAppointment(
                                        row.getString("id"),
                                        room,
                                        row.getString("tenant_id"),
                                        row.optString("tenant_name"),
                                        row.getString("id"),
                                        ApiMapper.timestamp(row.getString("start_at")),
                                        ApiMapper.timestamp(row.getString("end_at")),
                                        nullable(row, "note"),
                                        ViewingAppointment.Status.valueOf(status),
                                        nullable(row, "cancelled_reason")));
                    }
                    return result;
                },
                result -> {
                    tenantAppointments.clear();
                    tenantAppointments.addAll(result);
                    success.accept(result);
                },
                failure);
    }

    public void fetchHostAppointments(
            Consumer<List<HostViewingAppointment>> success, Consumer<String> failure) {
        ApiClient.execute(
                () -> {
                    JSONArray rows =
                            ((JSONObject) ApiClient.request("GET", "viewings/host", null))
                                    .getJSONArray("data");
                    List<HostViewingAppointment> result = new ArrayList<>();
                    for (int index = 0; index < rows.length(); index++) {
                        JSONObject row = rows.getJSONObject(index);
                        result.add(
                                new HostViewingAppointment(
                                        row.getString("id"),
                                        row.getString("host_id"),
                                        row.getString("room_id"),
                                        row.getString("tenant_id"),
                                        row.optString("room_name"),
                                        row.optString("unit_code"),
                                        row.optString("tenant_name"),
                                        ApiMapper.timestamp(row.getString("start_at")),
                                        ApiMapper.timestamp(row.getString("end_at")),
                                        nullable(row, "note"),
                                        HostViewingAppointment.Status.valueOf(status(row)),
                                        nullable(row, "cancelled_reason")));
                    }
                    return result;
                },
                result -> {
                    hostAppointments.clear();
                    hostAppointments.addAll(result);
                    success.accept(result);
                },
                failure);
    }

    public void fetchSlots(
            String room,
            boolean host,
            Consumer<List<HostViewingSlot>> success,
            Consumer<String> failure) {
        ApiClient.execute(
                () -> {
                    JSONArray rows =
                            ((JSONObject)
                                            ApiClient.request(
                                                    "GET",
                                                    "viewings/room/" + room + "?host=" + host,
                                                    null))
                                    .getJSONArray("data");
                    List<HostViewingSlot> result = new ArrayList<>();
                    for (int index = 0; index < rows.length(); index++) {
                        JSONObject row = rows.getJSONObject(index);
                        result.add(
                                new HostViewingSlot(
                                        row.getString("id"),
                                        row.getString("host_id"),
                                        row.getString("room_id"),
                                        ApiMapper.timestamp(row.getString("start_at")),
                                        ApiMapper.timestamp(row.getString("end_at")),
                                        row.getString("status").equals("OPEN")));
                    }
                    return result;
                },
                success,
                failure);
    }

    public void createSlot(
            String room, long start, long end, Runnable success, Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                "POST",
                                "viewings",
                                ApiClient.json(
                                        "room_id",
                                        room,
                                        "start_at",
                                        iso(start),
                                        "end_at",
                                        iso(end))),
                row -> success.run(),
                failure);
    }

    public void action(
            String id,
            String action,
            String note,
            String reason,
            Runnable success,
            Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                "POST",
                                "viewings/" + id + "/" + action,
                                ApiClient.json("note", note, "reason", reason)),
                row -> success.run(),
                failure);
    }

    private static String status(JSONObject row) {
        String status = row.optString("status");
        if (status.equals("REQUESTED")) return "PENDING";
        if (status.equals("CANCELLED_BY_HOST")
                && row.optString("cancel_reason_code").equals("HOST_REJECTED")) return "REJECTED";
        if (status.startsWith("CANCELLED")) return "CANCELLED";
        return status;
    }

    private static String nullable(JSONObject row, String key) {
        return row.isNull(key) ? "" : row.optString(key);
    }

    private static String iso(long value) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(value));
    }
}
