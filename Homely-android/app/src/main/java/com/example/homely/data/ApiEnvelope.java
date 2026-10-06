package com.example.homely.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Unwrap the HTTP envelope once so screens continue to receive their domain DTOs. */
final class ApiEnvelope {
    private ApiEnvelope() { }

    private static JSONObject parse(String raw, int httpStatus) throws JSONException {
        JSONObject envelope = new JSONObject(raw);
        if (!envelope.has("statusCode") || envelope.getInt("statusCode") != httpStatus
                || !envelope.has("message") || !envelope.has("data")) {
            throw new JSONException("Invalid API response envelope");
        }
        return envelope;
    }

    static JSONObject data(String raw, int httpStatus) throws JSONException {
        if (httpStatus == 204 || httpStatus == 205) return new JSONObject();
        Object payload = parse(raw, httpStatus).get("data");
        if (payload == JSONObject.NULL) return new JSONObject();
        if (payload instanceof JSONObject object) return object;
        // Arrays have a local data key for existing list screens; pages already contain their own data key.
        if (payload instanceof JSONArray array) return new JSONObject().put("data", array);
        return new JSONObject().put("data", payload);
    }

    static String errorMessage(String raw, int httpStatus, String fallback) {
        try {
            JSONObject envelope = parse(raw, httpStatus);
            JSONObject problem = envelope.optJSONObject("data");
            String detail = problem == null ? "" : problem.optString("detail", "");
            if (!detail.isBlank()) return detail;
            String message = envelope.optString("message", "");
            return message.isBlank() ? fallback : message;
        } catch (JSONException ex) {
            return fallback;
        }
    }
}
