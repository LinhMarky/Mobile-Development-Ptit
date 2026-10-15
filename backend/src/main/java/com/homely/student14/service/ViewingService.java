package com.homely.student14.service;

import com.homely.student14.dto.request.viewing.CreateViewingDTO;
import com.homely.student14.dto.request.viewing.ViewingActionDTO;
import com.homely.student14.repository.DatabaseRepository;
import com.homely.student14.repository.ViewingRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ViewingService {
    private static final long MAX_DURATION_SECONDS = 10800;
    private static final Set<String> CANCELLABLE_STATUSES =
            Set.of("OPEN", "REQUESTED", "CONFIRMED");

    private final ViewingRepository viewingRepository;
    private final AuthService authService;
    private final CatalogService catalogService;

    public List<Map<String, Object>> findTenantAppointments(Jwt jwt) {
        Map<String, Object> tenant = authService.role(jwt, "tenant");
        long tenantId = DatabaseRepository.id(tenant.get("id"));
        return viewingRepository.findAppointmentsByTenant(tenantId);
    }

    public List<Map<String, Object>> findHostAppointments(Jwt jwt) {
        Map<String, Object> host = authService.role(jwt, "host");
        long hostId = DatabaseRepository.id(host.get("id"));
        return viewingRepository.findAppointmentsByHost(hostId);
    }

    public List<Map<String, Object>> findRoomSlots(long roomId, Jwt jwt, boolean hostView) {
        if (hostView) {
            catalogService.ownedRoom(roomId, jwt, false);
            return viewingRepository.findHostSlots(roomId);
        }
        return viewingRepository.findAvailableSlots(roomId);
    }

    @Transactional
    public Map<String, Object> createViewing(Jwt jwt, CreateViewingDTO request) {
        long roomId = request.getRoomId();
        Map<String, Object> room = catalogService.ownedRoom(roomId, jwt, true);
        Map<String, Object> currentUser = authService.user(jwt);
        requireVerifiedEmail(currentUser);

        Instant startAt = parseTime(request.getStartAt(), "start_at");
        Instant endAt = parseTime(request.getEndAt(), "end_at");
        DatabaseRepository.require(
                startAt.isAfter(Instant.now())
                        && endAt.isAfter(startAt)
                        && endAt.isBefore(startAt.plusSeconds(MAX_DURATION_SECONDS + 1)),
                400,
                "Chọn giờ tương lai, thời lượng tối đa 3 giờ.");

        boolean overlapping = viewingRepository.hasOverlappingViewing(roomId, startAt, endAt);
        DatabaseRepository.require(
                !overlapping, 409, "Khung giờ trùng với lịch đang mở hoặc đã có người đặt.");

        long hostId = DatabaseRepository.id(room.get("host_id"));
        long viewingId = viewingRepository.createViewing(roomId, hostId, startAt, endAt);
        return viewingRepository.findViewingById(viewingId);
    }

    @Transactional
    public Map<String, Object> updateViewing(
            long viewingId, Jwt jwt, String action, ViewingActionDTO request) {
        Map<String, Object> currentUser = authService.user(jwt);

        // Luôn khóa phòng trước lịch để các thao tác đồng thời dùng cùng thứ tự khóa.
        long roomId = viewingRepository.findRoomIdByViewingId(viewingId);
        Map<String, Object> room = viewingRepository.lockRoom(roomId);
        Map<String, Object> viewing = viewingRepository.lockViewing(viewingId);
        requireVerifiedEmail(currentUser);
        DatabaseRepository.require(
                Instant.parse(viewing.get("start_at").toString()).isAfter(Instant.now()),
                409,
                "Lịch đã bắt đầu hoặc đã qua.");

        long userId = DatabaseRepository.id(currentUser.get("id"));
        boolean isHost = userId == DatabaseRepository.id(viewing.get("host_id"));
        String status = viewing.get("status").toString();

        switch (action) {
            case "request" ->
                    requestViewing(viewingId, currentUser, viewing, room, isHost, request);
            case "reopen" -> reopenViewing(viewingId, viewing, status, isHost);
            case "approve" -> reviewViewing(viewingId, status, isHost, true, request.getReason());
            case "reject" -> reviewViewing(viewingId, status, isHost, false, request.getReason());
            case "cancel" ->
                    cancelViewing(viewingId, userId, viewing, status, isHost, request.getReason());
            default -> throw DatabaseRepository.fail(400, "Thao tác không hợp lệ.");
        }

        Map<String, Object> updatedViewing = viewingRepository.findViewingById(viewingId);
        Object recipient = updatedViewing.get(isHost ? "tenant_id" : "host_id");
        if (recipient != null) {
            viewingRepository.notifyStatusChange(
                    DatabaseRepository.id(recipient),
                    updatedViewing.get("status").toString(),
                    viewingId);
        }
        return updatedViewing;
    }

    private void requestViewing(
            long viewingId,
            Map<String, Object> currentUser,
            Map<String, Object> viewing,
            Map<String, Object> room,
            boolean isHost,
            ViewingActionDTO request) {
        DatabaseRepository.require(
                !isHost && DatabaseRepository.flag(currentUser.get("is_tenant")),
                403,
                "Chỉ người thuê khác chủ phòng được đặt lịch.");
        DatabaseRepository.require(
                "OPEN".equals(viewing.get("status"))
                        && "AVAILABLE".equals(room.get("availability")),
                409,
                "Khung giờ hoặc phòng không còn trống.");

        long roomId = DatabaseRepository.id(viewing.get("room_id"));
        viewingRepository.checkPublishedListing(roomId);
        long tenantId = DatabaseRepository.id(currentUser.get("id"));
        String note = validateText(request.getNote(), "note", 1000, false);
        viewingRepository.requestViewing(viewingId, tenantId, note);
    }

    private void reopenViewing(
            long viewingId, Map<String, Object> viewing, String status, boolean isHost) {
        DatabaseRepository.require(
                isHost && viewing.get("tenant_id") == null && "CANCELLED_BY_HOST".equals(status),
                409,
                "Không thể mở lại lịch đã có người đặt.");
        viewingRepository.reopenViewing(viewingId);
    }

    private void reviewViewing(
            long viewingId, String status, boolean isHost, boolean approved, String reason) {
        DatabaseRepository.require(isHost, 403, "Chỉ chủ phòng được xử lý lịch.");
        DatabaseRepository.require("REQUESTED".equals(status), 409, "Yêu cầu không còn chờ duyệt.");
        String normalizedReason = validateText(reason, "reason", 1000, !approved);
        String updatedStatus = approved ? "CONFIRMED" : "CANCELLED_BY_HOST";
        String reasonCode = approved ? null : "HOST_REJECTED";
        viewingRepository.updateStatus(viewingId, updatedStatus, normalizedReason, reasonCode);
    }

    private void cancelViewing(
            long viewingId,
            long userId,
            Map<String, Object> viewing,
            String status,
            boolean isHost,
            String reason) {
        boolean isTenant =
                viewing.get("tenant_id") != null
                        && userId == DatabaseRepository.id(viewing.get("tenant_id"));
        DatabaseRepository.require(isHost || isTenant, 403, "Bạn không được hủy lịch này.");
        DatabaseRepository.require(
                CANCELLABLE_STATUSES.contains(status), 409, "Lịch đã kết thúc hoặc đã hủy.");
        String updatedStatus = isHost ? "CANCELLED_BY_HOST" : "CANCELLED_BY_TENANT";
        String reasonCode = isHost ? "HOST_CANCELLED" : "TENANT_CANCELLED";
        String normalizedReason = validateText(reason, "reason", 1000, false);
        viewingRepository.updateStatus(viewingId, updatedStatus, normalizedReason, reasonCode);
    }

    private void requireVerifiedEmail(Map<String, Object> currentUser) {
        DatabaseRepository.require(
                DatabaseRepository.flag(currentUser.get("email_verified")),
                403,
                "Cần xác minh email.");
    }

    private Instant parseTime(String value, String fieldName) {
        String normalizedTime = validateText(value, fieldName, 40, true);
        try {
            return Instant.parse(normalizedTime);
        } catch (DateTimeParseException exception) {
            throw DatabaseRepository.fail(400, "Thời gian không hợp lệ.");
        }
    }

    private String validateText(String value, String fieldName, int maxLength, boolean required) {
        String normalizedText = Objects.toString(value, "").trim();
        DatabaseRepository.require(
                (!required || !normalizedText.isEmpty())
                        && normalizedText.codePointCount(0, normalizedText.length()) <= maxLength,
                400,
                "Trường " + fieldName + " không hợp lệ (tối đa " + maxLength + " ký tự).");
        return normalizedText;
    }
}
