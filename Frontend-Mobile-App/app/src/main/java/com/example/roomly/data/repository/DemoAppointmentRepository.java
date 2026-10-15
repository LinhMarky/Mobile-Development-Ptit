package com.example.roomly.data.repository;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.HostViewingAppointment;
import com.example.roomly.data.model.HostViewingSlot;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.model.ViewingAppointment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Nguồn dữ liệu chung cho lịch người thuê và yêu cầu phía chủ trọ.
 * Dữ liệu chỉ nằm trong bộ nhớ.
 */
public final class DemoAppointmentRepository {

    private static final DemoAppointmentRepository INSTANCE =
            new DemoAppointmentRepository();

    private final List<ViewingAppointment> appointments =
            new ArrayList<>();

    /** Khởi tạo danh sách trống. */
    private DemoAppointmentRepository() {
    }

    /** Trả về repository dùng chung. */
    public static DemoAppointmentRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Tạo yêu cầu từ bài đang công khai và khung giờ đang mở.
     * Lấy người gửi từ phiên hiện tại, chặn tự đặt phòng của mình.
     */
    @MainThread
    public ViewingAppointment createRequest(
            String listingId,
            String slotId,
            String note
    ) {
        SessionState session = requireVerifiedRole(UserRole.TENANT);

        RoomCard room = findPublishedRoom(listingId);

        if (room == null) {
            throw new IllegalStateException(
                    "Bài đăng không còn được hiển thị."
            );
        }

        if (session.getUserId().equals(room.getOwnerId())) {
            throw new IllegalStateException(
                    "Bạn không thể đặt lịch xem phòng của chính mình."
            );
        }

        HostViewingSlot selectedSlot = null;

        for (HostViewingSlot slot :
                DemoHostViewingRepository.getInstance()
                        .getOpenSlotsForListing(listingId)) {
            if (slot.getId().equals(slotId)) {
                selectedSlot = slot;
                break;
            }
        }

        if (selectedSlot == null) {
            throw new IllegalStateException(
                    "Khung giờ đã đóng, đã qua hoặc không còn tồn tại."
            );
        }

        String cleanNote = note == null ? "" : note.trim();

        if (cleanNote.codePointCount(0, cleanNote.length()) > 300) {
            throw new IllegalArgumentException(
                    "Ghi chú không được vượt quá 300 ký tự."
            );
        }

        for (ViewingAppointment existing : appointments) {
            boolean active =
                    existing.getStatus()
                            == ViewingAppointment.Status.PENDING
                            || existing.getStatus()
                            == ViewingAppointment.Status.CONFIRMED;

            if (active
                    && session.getUserId().equals(existing.getGuestId())
                    && slotId.equals(existing.getSlotId())) {
                throw new IllegalStateException(
                        "Bạn đã có yêu cầu cho khung giờ này."
                );
            }
        }

        ViewingAppointment appointment = new ViewingAppointment(
                UUID.randomUUID().toString(),
                room,
                session.getUserId(),
                session.getFullName(),
                selectedSlot.getId(),
                selectedSlot.getStartTimeMillis(),
                selectedSlot.getEndTimeMillis(),
                cleanNote,
                ViewingAppointment.Status.PENDING,
                ""
        );

        appointments.add(appointment);
        return appointment;
    }

    /**
     * Giữ tên hàm cũ để code giao diện hiện tại vẫn biên dịch.
     * Không nhận lịch tự chọn giờ vì chưa kiểm tra khung giờ chủ trọ.
     */
    @MainThread
    public void addAppointment(ViewingAppointment appointment) {
        throw new IllegalStateException(
                "Bạn cần chọn khung giờ đang mở của chủ trọ."
        );
    }

    /** Chỉ trả về lịch của người thuê đang đăng nhập. */
    @MainThread
    public List<ViewingAppointment> getAppointments() {
        List<ViewingAppointment> results = new ArrayList<>();

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.TENANT)) {
            return results;
        }

        for (ViewingAppointment appointment : appointments) {
            if (session.getUserId().equals(appointment.getGuestId())) {
                results.add(appointment);
            }
        }

        results.sort(
                Comparator.comparingLong(
                        ViewingAppointment::getAppointmentTimeMillis
                )
        );

        return results;
    }

    /** Tìm lịch của đúng người thuê hiện tại theo mã. */
    @Nullable
    @MainThread
    public ViewingAppointment getMyAppointmentById(String id) {
        if (id == null) {
            return null;
        }

        for (ViewingAppointment appointment : getAppointments()) {
            if (id.equals(appointment.getId())) {
                return appointment;
            }
        }

        return null;
    }

    /** Hủy lịch của người thuê và giữ bản ghi để hai phía thấy kết quả. */
    @MainThread
    public void cancelAppointment(String appointmentId) {
        requireVerifiedRole(UserRole.TENANT);

        ViewingAppointment current =
                getMyAppointmentById(appointmentId);

        if (current == null) {
            throw new IllegalStateException(
                    "Không tìm thấy lịch thuộc tài khoản của bạn."
            );
        }

        if (!current.canCancel()) {
            throw new IllegalStateException(
                    "Lịch này không còn được phép hủy."
            );
        }

        replaceAppointment(
                current.withStatus(
                        ViewingAppointment.Status.CANCELLED,
                        "Người thuê đã hủy lịch."
                )
        );
    }

    /** Giữ tên hàm cũ; chuyển thao tác xóa thành cập nhật Đã hủy. */
    @MainThread
    public void removeAppointment(ViewingAppointment appointment) {
        if (appointment == null) {
            throw new IllegalArgumentException("Thiếu lịch cần hủy.");
        }

        cancelAppointment(appointment.getId());
    }

    /** Trả về cùng bản ghi dưới dạng thông tin dành cho đúng chủ trọ. */
    @MainThread
    public List<HostViewingAppointment> getMyHostAppointments() {
        List<HostViewingAppointment> results = new ArrayList<>();

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)) {
            return results;
        }

        for (ViewingAppointment appointment : appointments) {
            if (!session.getUserId().equals(appointment.getOwnerId())) {
                continue;
            }

            HostRoom room = DemoHostRoomRepository.getInstance()
                    .getMyRoomById(appointment.getRoomId());

            if (room != null
                    && room.getOwnerId().equals(appointment.getOwnerId())) {
                results.add(toHostAppointment(appointment));
            }
        }

        results.sort(
                Comparator.comparingLong(
                        HostViewingAppointment::getStartTimeMillis
                )
        );

        return results;
    }

    /**
     * Chủ trọ xử lý yêu cầu đang chờ của phòng mình.
     * Cập nhật chính bản ghi người thuê đang sử dụng.
     */
    @MainThread
    public HostViewingAppointment processHostDecision(
            String appointmentId,
            boolean confirmed,
            String reason
    ) {
        SessionState session = requireVerifiedRole(UserRole.HOST);

        ViewingAppointment current = null;

        for (ViewingAppointment appointment : appointments) {
            if (appointment.getId().equals(appointmentId)
                    && session.getUserId().equals(
                    appointment.getOwnerId()
            )) {
                current = appointment;
                break;
            }
        }

        if (current == null
                || DemoHostRoomRepository.getInstance()
                .getMyRoomById(current.getRoomId()) == null) {
            throw new IllegalStateException(
                    "Không tìm thấy yêu cầu thuộc phòng của bạn."
            );
        }

        if (current.getStatus() != ViewingAppointment.Status.PENDING) {
            throw new IllegalStateException(
                    "Yêu cầu này đã được xử lý hoặc đã hủy."
            );
        }

        if (current.getAppointmentTimeMillis()
                <= System.currentTimeMillis()) {
            throw new IllegalStateException(
                    "Lịch đã bắt đầu hoặc đã qua."
            );
        }

        String cleanReason = reason == null ? "" : reason.trim();

        if (!confirmed && cleanReason.isEmpty()) {
            throw new IllegalArgumentException(
                    "Bạn cần nhập lý do từ chối."
            );
        }

        ViewingAppointment updated = current.withStatus(
                confirmed
                        ? ViewingAppointment.Status.CONFIRMED
                        : ViewingAppointment.Status.REJECTED,
                confirmed ? "" : cleanReason
        );

        replaceAppointment(updated);
        return toHostAppointment(updated);
    }

    /** Tìm thẻ phòng từ nguồn đang được phép công khai. */
    @Nullable
    private RoomCard findPublishedRoom(String listingId) {
        if (listingId == null) {
            return null;
        }

        for (RoomCard room : DemoAdminListingRepository.getInstance()
                .getPublishedRoomCards()) {
            if (listingId.equals(room.getListingId())) {
                return room;
            }
        }

        return null;
    }

    /** Kiểm tra đăng nhập, vai trò và email đã xác minh. */
    private SessionState requireVerifiedRole(UserRole role) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(role)
                || !session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần đăng nhập đúng vai trò và xác minh email."
            );
        }

        return session;
    }

    /** Thay bản ghi theo mã, không dựa vào tham chiếu đối tượng cũ. */
    private void replaceAppointment(ViewingAppointment updated) {
        for (int index = 0; index < appointments.size(); index++) {
            if (appointments.get(index).getId().equals(updated.getId())) {
                appointments.set(index, updated);
                return;
            }
        }

        throw new IllegalStateException("Lịch hẹn không còn tồn tại.");
    }

    /** Chuyển cùng một lịch thành dữ liệu hiển thị phía chủ trọ. */
    private HostViewingAppointment toHostAppointment(
            ViewingAppointment appointment
    ) {
        HostViewingAppointment.Status status;

        switch (appointment.getStatus()) {
            case CONFIRMED:
                status = HostViewingAppointment.Status.CONFIRMED;
                break;
            case REJECTED:
                status = HostViewingAppointment.Status.REJECTED;
                break;
            case CANCELLED:
                status = HostViewingAppointment.Status.CANCELLED;
                break;
            case PENDING:
            default:
                status = HostViewingAppointment.Status.PENDING;
                break;
        }

        return new HostViewingAppointment(
                appointment.getId(),
                appointment.getOwnerId(),
                appointment.getRoomId(),
                appointment.getGuestId(),
                appointment.getRoomTitle(),
                appointment.getUnitCode(),
                appointment.getGuestName(),
                appointment.getAppointmentTimeMillis(),
                appointment.getEndTimeMillis(),
                appointment.getNote(),
                status,
                appointment.getDecisionReason()
        );
    }
}