package com.example.roomly.data.repository;

import com.example.roomly.data.api.ApiClient;
import com.example.roomly.data.api.ApiMapper;
import com.example.roomly.data.model.AdminReport;
import com.example.roomly.data.model.BookingCase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ReportRepository {
    private static final ReportRepository INSTANCE = new ReportRepository();
    private final List<AdminReport> reports = new ArrayList<>();
    private final List<BookingCase> cases = new ArrayList<>();

    private ReportRepository() {}

    public static ReportRepository getInstance() {
        return INSTANCE;
    }

    public AdminReport getReportById(String id) {
        for (AdminReport report : reports) if (report.getId().equals(id)) return report;
        return null;
    }

    public BookingCase getAdminCaseById(String id) {
        for (BookingCase item : cases) if (item.getId().equals(id)) return item;
        return null;
    }

    public BookingCase getCaseByBookingId(String id) {
        for (BookingCase item : cases) if (item.getBookingId().equals(id)) return item;
        return null;
    }

    public void fetchReports(
            boolean admin,
            Boolean closed,
            Consumer<List<AdminReport>> success,
            Consumer<String> failure) {
        ApiClient.execute(
                () -> {
                    JSONArray rows =
                            ((JSONObject)
                                            ApiClient.request(
                                                    "GET",
                                                    admin ? "admin/reports" : "reports/my",
                                                    null))
                                    .getJSONArray("data");
                    List<AdminReport> result = new ArrayList<>();
                    for (int index = 0; index < rows.length(); index++) {
                        JSONObject row = rows.getJSONObject(index);
                        String type = row.getString("target_type");
                        if (!type.equals("listing")
                                && !type.equals("review")
                                && !type.equals("message")) continue;
                        String status = row.getString("status");
                        result.add(
                                new AdminReport(
                                        row.getString("id"),
                                        AdminReport.ContentType.valueOf(
                                                type.toUpperCase(java.util.Locale.ROOT)),
                                        row.getString("target_id"),
                                        row.isNull("target_title")
                                                ? "Nội dung #" + row.getString("target_id")
                                                : row.optString("target_title"),
                                        nullable(row, "content_preview"),
                                        nullable(row, "reporter_name"),
                                        nullable(row, "reason_code")
                                                + "\n"
                                                + nullable(row, "description"),
                                        ApiMapper.timestamp(row.getString("created_at")),
                                        status.equals("RESOLVED")
                                                ? AdminReport.Status.RESOLVED
                                                : status.equals("CLOSED")
                                                        ? AdminReport.Status.DISMISSED
                                                        : AdminReport.Status.OPEN,
                                        nullable(row, "resolution_note")));
                    }
                    return result;
                },
                result -> {
                    reports.clear();
                    reports.addAll(result);
                    List<AdminReport> filtered = new ArrayList<>();
                    for (AdminReport item : result)
                        if (closed == null
                                || closed == (item.getStatus() != AdminReport.Status.OPEN))
                            filtered.add(item);
                    success.accept(filtered);
                },
                failure);
    }

    public void fetchCases(
            String booking,
            Boolean closed,
            Consumer<List<BookingCase>> success,
            Consumer<String> failure) {
        ApiClient.execute(
                () -> {
                    JSONArray rows =
                            ((JSONObject)
                                            ApiClient.request(
                                                    "GET",
                                                    booking == null
                                                            ? "admin/reports?cases=true"
                                                            : "bookings/" + booking + "/cases",
                                                    null))
                                    .getJSONArray("data");
                    List<BookingCase> result = new ArrayList<>();
                    for (int index = 0; index < rows.length(); index++) {
                        JSONObject row = rows.getJSONObject(index);
                        result.add(
                                new BookingCase(
                                        row.getString("id"),
                                        row.getString("booking_id"),
                                        row.isNull("reporter_id")
                                                ? "system"
                                                : row.optString("reporter_id"),
                                        BookingCase.Type.valueOf(row.getString("reason_code")),
                                        row.getString("description"),
                                        BookingCase.Status.valueOf(row.getString("status")),
                                        row.isNull("decision")
                                                ? null
                                                : BookingCase.Decision.valueOf(
                                                        row.getString("decision")),
                                        nullable(row, "resolution_note"),
                                        nullable(row, "resolved_by"),
                                        row.isNull("resolved_at")
                                                ? null
                                                : ApiMapper.timestamp(row.getString("resolved_at")),
                                        ApiMapper.timestamp(row.getString("created_at"))));
                    }
                    return result;
                },
                result -> {
                    cases.clear();
                    cases.addAll(result);
                    List<BookingCase> filtered = new ArrayList<>();
                    for (BookingCase item : result)
                        if (closed == null || closed != item.isOpen()) filtered.add(item);
                    success.accept(filtered);
                },
                failure);
    }

    public void submit(
            String type,
            String id,
            String reason,
            String description,
            Runnable success,
            Consumer<String> failure) {
        ApiClient.execute(
                () ->
                        ApiClient.request(
                                "POST",
                                "reports",
                                ApiClient.json(
                                        "target_type",
                                        type,
                                        "target_id",
                                        id,
                                        "reason_code",
                                        reason,
                                        "description",
                                        description)),
                row -> success.run(),
                failure);
    }

    public void resolve(String id, JSONObject body, Runnable success, Consumer<String> failure) {
        ApiClient.execute(
                () -> ApiClient.request("POST", "admin/reports/" + id + "/resolve", body),
                row -> success.run(),
                failure);
    }

    private static String nullable(JSONObject row, String key) {
        return row.isNull(key) ? "" : row.optString(key);
    }
}
