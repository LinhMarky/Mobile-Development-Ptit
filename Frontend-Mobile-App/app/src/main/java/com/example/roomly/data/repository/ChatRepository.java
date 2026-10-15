package com.example.roomly.data.repository;

import com.example.roomly.data.api.ApiClient;
import com.example.roomly.data.api.ApiMapper;
import com.example.roomly.data.model.ChatMessage;
import com.example.roomly.data.model.Conversation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ChatRepository {
    public void fetchConversations(Consumer<List<Conversation>> success, Consumer<String> failure) {
        ApiClient.execute(
                () -> {
                    JSONArray rows =
                            ((JSONObject) ApiClient.request("GET", "conversations", null))
                                    .getJSONArray("data");
                    List<Conversation> result = new ArrayList<>();
                    for (int index = 0; index < rows.length(); index++) {
                        JSONObject row = rows.getJSONObject(index);
                        Conversation conversation = map(row);
                        if (!row.isNull("last_message_preview"))
                            conversation.addMessage(
                                    new ChatMessage(
                                            row.optString("last_message_preview"),
                                            ApiMapper.timestamp(row.getString("last_message_at")),
                                            false));
                        result.add(conversation);
                    }
                    return result;
                },
                success,
                failure);
    }

    public void fetchConversation(
            String id, Consumer<Conversation> success, Consumer<String> failure) {
        ApiClient.execute(
                () -> {
                    Conversation conversation =
                            map((JSONObject) ApiClient.request("GET", "conversations/" + id, null));
                    JSONArray rows =
                            ((JSONObject)
                                            ApiClient.request(
                                                    "GET",
                                                    "conversations/" + id + "/messages",
                                                    null))
                                    .getJSONArray("data");
                    for (int index = 0; index < rows.length(); index++) {
                        JSONObject row = rows.getJSONObject(index);
                        conversation.addMessage(
                                new ChatMessage(
                                        row.getString("content"),
                                        ApiMapper.timestamp(row.getString("created_at")),
                                        row.getString("sender_id")));
                    }
                    return conversation;
                },
                success,
                failure);
    }

    public void create(String room, Consumer<String> success, Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        ((JSONObject)
                                        ApiClient.request(
                                                "POST",
                                                "conversations",
                                                ApiClient.json("room_id", room)))
                                .getString("id"),
                success,
                failure);
    }

    public void send(
            String id,
            String content,
            String clientId,
            Runnable success,
            Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                "POST",
                                "conversations/" + id + "/messages",
                                ApiClient.json("content", content, "client_message_id", clientId)),
                row -> success.run(),
                failure);
    }

    private static Conversation map(JSONObject row) throws Exception {
        JSONObject currentUser = ApiClient.cachedUser();
        String userId = currentUser.optString("id");
        String userName = currentUser.optString("full_name");
        String hostId = row.getString("host_id");
        String tenantId = row.getString("tenant_id");
        boolean isHost = userId.equals(hostId);
        String contactName = row.optString("contact_name");

        return new Conversation(
                row.getString("id"),
                row.getString("room_id"),
                hostId,
                tenantId,
                isHost ? userName : contactName,
                isHost ? contactName : userName,
                row.optString("room_title"),
                android.R.drawable.ic_menu_gallery,
                ApiMapper.timestamp(row.getString("created_at")));
    }
}
