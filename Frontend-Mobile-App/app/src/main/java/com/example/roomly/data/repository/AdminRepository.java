package com.example.roomly.data.repository;

import com.example.roomly.data.api.ApiClient;
import com.example.roomly.data.api.ApiMapper;
import com.example.roomly.data.model.AdminAccount;
import com.example.roomly.data.model.AdminListing;
import com.example.roomly.data.model.UserRole;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class AdminRepository {
    private static final AdminRepository INSTANCE = new AdminRepository();
    private final List<AdminListing> listings = new ArrayList<>();
    private final List<AdminAccount> accounts = new ArrayList<>();

    private AdminRepository() {}

    public static AdminRepository getInstance() {
        return INSTANCE;
    }

    public AdminListing getListingById(String id) {
        for (AdminListing listing : listings) if (listing.getId().equals(id)) return listing;
        return null;
    }

    public AdminAccount getAccountById(String id) {
        for (AdminAccount account : accounts) if (account.getId().equals(id)) return account;
        return null;
    }

    public void fetchListings(
            AdminListing.Status filter,
            Consumer<List<AdminListing>> success,
            Consumer<String> failure) {
        ApiClient.execute(
                () -> {
                    JSONArray rows =
                            ((JSONObject) ApiClient.request("GET", "admin/listings", null))
                                    .getJSONArray("data");
                    List<AdminListing> result = new ArrayList<>();
                    for (int index = 0; index < rows.length(); index++) {
                        JSONObject row = rows.getJSONObject(index);
                        String status = row.getString("status");
                        if (status.equals("DRAFT")) continue;
                        AdminListing.Status mapped = AdminListing.Status.valueOf(status);
                        result.add(
                                new AdminListing(
                                        row.getString("id"),
                                        row.getString("host_id"),
                                        row.getString("room_id"),
                                        row.getString("title"),
                                        ApiMapper.money(row, "rent_vnd"),
                                        ApiMapper.money(row, "deposit_vnd"),
                                        row.optString("description"),
                                        row.optString("host_name"),
                                        row.optString("unit_code"),
                                        row.optString("address_line"),
                                        photo(row),
                                        mapped,
                                        row.isNull("moderation_reason")
                                                ? ""
                                                : row.optString("moderation_reason")));
                    }
                    return result;
                },
                result -> {
                    listings.clear();
                    listings.addAll(result);
                    List<AdminListing> filtered = new ArrayList<>();
                    for (AdminListing item : result)
                        if (filter == null || item.getStatus() == filter) filtered.add(item);
                    success.accept(filtered);
                },
                failure);
    }

    private static String photo(JSONObject row) {
        JSONArray images = row.optJSONArray("images");
        return images == null || images.length() == 0
                ? null
                : images.optJSONObject(0).optString("url");
    }

    public void moderate(
            String id, String action, String reason, Runnable success, Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                "POST",
                                "admin/listings/" + id + "/" + action,
                                ApiClient.json("reason", reason)),
                row -> fetchListings(null, ignored -> success.run(), failure),
                failure);
    }

    public void fetchAccounts(
            AdminAccount.Status filter,
            String query,
            Consumer<List<AdminAccount>> success,
            Consumer<String> failure) {
        ApiClient.execute(
                () -> {
                    JSONArray rows =
                            ((JSONObject) ApiClient.request("GET", "admin/users", null))
                                    .getJSONArray("data");
                    List<AdminAccount> result = new ArrayList<>();
                    for (int index = 0; index < rows.length(); index++) {
                        JSONObject row = rows.getJSONObject(index);
                        EnumSet<UserRole> roles = EnumSet.noneOf(UserRole.class);
                        JSONArray rawRoles = row.getJSONArray("roles");
                        for (int role = 0; role < rawRoles.length(); role++)
                            roles.add(
                                    UserRole.valueOf(
                                            rawRoles.getString(role).replace("ROLE_", "")));
                        result.add(
                                new AdminAccount(
                                        row.getString("id"),
                                        row.optString("full_name"),
                                        row.getString("email"),
                                        roles,
                                        row.optBoolean("email_verified"),
                                        row.getString("status").equals("ACTIVE")
                                                ? AdminAccount.Status.ACTIVE
                                                : AdminAccount.Status.SUSPENDED,
                                        row.isNull("suspend_reason")
                                                ? ""
                                                : row.optString("suspend_reason")));
                    }
                    return result;
                },
                result -> {
                    accounts.clear();
                    accounts.addAll(result);
                    List<AdminAccount> filtered = new ArrayList<>();
                    String keyword = query.toLowerCase(Locale.ROOT).trim();
                    for (AdminAccount item : result) {
                        if ((filter == null || item.getStatus() == filter)
                                && (item.getFullName() + " " + item.getEmail())
                                        .toLowerCase(Locale.ROOT)
                                        .contains(keyword)) filtered.add(item);
                    }
                    success.accept(filtered);
                },
                failure);
    }

    public void suspendAccount(
            String id, String reason, Runnable success, Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                "POST",
                                "admin/users/" + id + "/suspend",
                                ApiClient.json("reason", reason)),
                row -> success.run(),
                failure);
    }
}
