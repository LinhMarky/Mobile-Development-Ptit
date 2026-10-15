package com.example.roomly.data.repository;

import com.example.roomly.data.api.ApiClient;
import com.example.roomly.data.api.ApiMapper;
import com.example.roomly.data.model.RoomCard;

import org.json.JSONObject;

import java.util.List;
import java.util.function.Consumer;

public final class RoomRepository {
    public void fetchRooms(Consumer<List<RoomCard>> success, Consumer<String> failure) {
        boolean tenant =
                SessionRepository.getInstance()
                        .getCurrentSession()
                        .hasRole(com.example.roomly.data.model.UserRole.TENANT);
        ApiClient.execute(
                () -> {
                    List<RoomCard> rooms =
                            ApiMapper.rooms(
                                    (JSONObject) ApiClient.request("GET", "listings", null));
                    if (tenant) {
                        List<RoomCard> saved =
                                ApiMapper.rooms(
                                        (JSONObject) ApiClient.request("GET", "wishlist", null));
                        java.util.Set<String> ids = new java.util.HashSet<>();
                        for (RoomCard room : saved) ids.add(room.getRoomId());
                        for (RoomCard room : rooms) room.setSaved(ids.contains(room.getRoomId()));
                    }
                    return rooms;
                },
                success,
                failure);
    }

    public void save(RoomCard room, boolean saved, Runnable success, Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                saved ? "POST" : "DELETE", "wishlist/" + room.getRoomId(), null),
                response -> success.run(),
                failure);
    }

    public void fetchSavedRooms(Consumer<List<RoomCard>> success, Consumer<String> failure) {
        ApiClient.execute(
                () -> {
                    List<RoomCard> rooms =
                            ApiMapper.rooms(
                                    (JSONObject) ApiClient.request("GET", "wishlist", null));
                    for (RoomCard room : rooms) room.setSaved(true);
                    return rooms;
                },
                success,
                failure);
    }
}
