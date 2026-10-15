package com.homely.student14.service;

import com.homely.student14.dto.request.booking.BookingActionDTO;
import com.homely.student14.dto.request.booking.CreateBookingDTO;
import com.homely.student14.dto.request.booking.CreatePaymentDTO;
import com.homely.student14.dto.request.booking.CreateReviewDTO;
import com.homely.student14.dto.request.booking.SimulatePaymentDTO;
import com.homely.student14.entity.Booking;
import com.homely.student14.entity.Listing;
import com.homely.student14.entity.Payment;
import com.homely.student14.entity.Room;
import com.homely.student14.repository.BookingRepository;
import com.homely.student14.repository.DatabaseRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final AuthService authService;

    @Value("${student14.mock-payment-enabled:false}")
    private boolean mockPaymentEnabled;

    public List<Map<String, Object>> list(Jwt jwt, boolean host) {
        Map<String, Object> currentUser = authService.role(jwt, host ? "host" : "tenant");
        return bookingRepository.findByUserId(DatabaseRepository.id(currentUser.get("id")), host);
    }

    public Map<String, Object> get(long bookingId, Jwt jwt) {
        Map<String, Object> currentUser = authService.user(jwt);
        Map<String, Object> details = bookingRepository.findDetailsById(bookingId);
        Booking booking = bookingRepository.toBooking(details);
        long currentUserId = DatabaseRepository.id(currentUser.get("id"));
        boolean participant =
                currentUserId == booking.getTenantId() || currentUserId == booking.getHostId();
        boolean admin = DatabaseRepository.flag(currentUser.get("is_admin"));
        if (!participant && !admin) {
            throw DatabaseRepository.fail(403, "Bạn không được xem yêu cầu này.");
        }
        return details;
    }

    @Transactional
    public Map<String, Object> create(Jwt jwt, CreateBookingDTO request) {
        Map<String, Object> tenant = authService.role(jwt, "tenant");
        if (!DatabaseRepository.flag(tenant.get("email_verified"))) {
            throw DatabaseRepository.fail(403, "Cần xác minh email để đặt phòng.");
        }
        Room room = bookingRepository.findRoomForUpdate(request.getRoomId());
        long tenantId = DatabaseRepository.id(tenant.get("id"));
        if (room.getHostId() == tenantId) {
            throw DatabaseRepository.fail(400, "Không thể đặt phòng của chính mình.");
        }
        if (!"AVAILABLE".equals(room.getAvailability())) {
            throw DatabaseRepository.fail(409, "Phòng không còn trống.");
        }
        Listing listing = bookingRepository.findPublishedListing(room.getId());
        if (request.getOccupantCount() < 1 || request.getOccupantCount() > room.getMaxOccupants()) {
            throw DatabaseRepository.fail(400, "Số người vượt sức chứa phòng.");
        }
        LocalDate desiredMoveIn;
        try {
            desiredMoveIn = LocalDate.parse(request.getDesiredMoveIn().trim());
        } catch (DateTimeParseException exception) {
            throw DatabaseRepository.fail(400, "Ngày vào ở không hợp lệ.");
        }
        if (!desiredMoveIn.isAfter(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")))) {
            throw DatabaseRepository.fail(400, "Ngày vào ở phải từ ngày mai.");
        }

        Booking booking = new Booking();
        booking.setTenantId(tenantId);
        booking.setHostId(room.getHostId());
        booking.setRoomId(room.getId());
        booking.setListingId(listing.getId());
        booking.setOccupantCount(request.getOccupantCount());
        booking.setRentVnd(listing.getRentVnd());
        booking.setDepositVnd(listing.getDepositVnd());
        booking.setDesiredMoveIn(desiredMoveIn);
        booking.setNote(trim(request.getNote()));

        // Store the agreed prices separately so later listing edits do not change this booking.
        Map<String, Object> termsSnapshot = new LinkedHashMap<>();
        termsSnapshot.put("rent_vnd", listing.getRentVnd().toPlainString());
        termsSnapshot.put("deposit_vnd", listing.getDepositVnd().toPlainString());
        termsSnapshot.put("terms_version", listing.getTermsVersion());
        termsSnapshot.put("fees", bookingRepository.readFees(listing));
        termsSnapshot.put("max_occupants", room.getMaxOccupants());

        long bookingId = bookingRepository.create(booking, termsSnapshot);
        bookingRepository.createNotification(
                room.getHostId(),
                "BOOKING_STATUS",
                "Yêu cầu thuê mới",
                tenant.get("full_name").toString(),
                bookingId);
        return get(bookingId, jwt);
    }

    @Transactional
    public Map<String, Object> approve(long bookingId, Jwt jwt) {
        Booking booking = lockForParticipant(bookingId, jwt, true);
        if (!"PENDING".equals(booking.getStatus()) || !isFuture(booking.getRequestExpiresAt())) {
            throw DatabaseRepository.fail(409, "Yêu cầu không còn chờ duyệt hoặc đã hết hạn.");
        }
        if (!"AVAILABLE".equals(bookingRepository.findRoomAvailability(booking.getRoomId()))) {
            throw DatabaseRepository.fail(409, "Phòng đã được giữ cho yêu cầu khác.");
        }
        bookingRepository.approve(bookingId);
        bookingRepository.updateRoomAvailability(booking.getRoomId(), "HELD");
        notifyStatus(booking, true, "approve");
        return get(bookingId, jwt);
    }

    @Transactional
    public Map<String, Object> reject(long bookingId, Jwt jwt, BookingActionDTO request) {
        Booking booking = lockForParticipant(bookingId, jwt, true);
        if (!"PENDING".equals(booking.getStatus())) {
            throw DatabaseRepository.fail(409, "Chỉ từ chối yêu cầu đang chờ duyệt.");
        }
        String reason = trim(request.getReason());
        if (reason.isEmpty()) {
            throw DatabaseRepository.fail(400, "Vui lòng nhập lý do từ chối.");
        }
        bookingRepository.cancel(bookingId, reason, "HOST_REJECTED");
        notifyStatus(booking, true, "reject");
        return get(bookingId, jwt);
    }

    @Transactional
    public Map<String, Object> cancel(long bookingId, Jwt jwt, BookingActionDTO request) {
        Booking booking = lockForParticipant(bookingId, jwt, false);
        if (!"PENDING".equals(booking.getStatus()) && !"APPROVED".equals(booking.getStatus())) {
            throw DatabaseRepository.fail(409, "Không thể hủy ở trạng thái này.");
        }
        bookingRepository.cancel(bookingId, trim(request.getReason()), "TENANT_CANCELLED");
        if ("APPROVED".equals(booking.getStatus())) {
            bookingRepository.updateRoomAvailability(booking.getRoomId(), "AVAILABLE");
        }
        notifyStatus(booking, false, "cancel");
        return get(bookingId, jwt);
    }

    @Transactional
    public Map<String, Object> confirmDeposit(long bookingId, Jwt jwt, BookingActionDTO request) {
        Booking booking = lockForParticipant(bookingId, jwt, false);
        checkActiveHold(booking);
        if (!request.isAcceptedTerms()) {
            throw DatabaseRepository.fail(400, "Bạn cần đồng ý điều khoản.");
        }
        if (booking.getDepositVnd().signum() > 0) {
            if (booking.getAllocatedPaymentId() == null) {
                throw DatabaseRepository.fail(409, "Chưa thanh toán cọc thành công.");
            }
            Payment payment = bookingRepository.findPayment(booking.getAllocatedPaymentId(), false);
            if (!"SUCCEEDED".equals(payment.getStatus())
                    || !payment.getBookingId().equals(bookingId)
                    || payment.getAmountVnd().compareTo(booking.getDepositVnd()) != 0) {
                throw DatabaseRepository.fail(409, "Thanh toán chưa hợp lệ.");
            }
        }
        bookingRepository.confirmDeposit(bookingId);
        notifyStatus(booking, false, "confirm-deposit");
        return get(bookingId, jwt);
    }

    @Transactional
    public Map<String, Object> confirmHandover(long bookingId, Jwt jwt, boolean host) {
        Booking booking = lockForParticipant(bookingId, jwt, host);
        if (!"CONFIRMED".equals(booking.getStatus()) && !"COMPLETED".equals(booking.getStatus())) {
            throw DatabaseRepository.fail(409, "Chỉ bàn giao khi đã xác nhận thuê.");
        }
        if (!"COMPLETED".equals(booking.getStatus())) {
            bookingRepository.confirmHandover(bookingId, host);
            Booking updatedBooking = bookingRepository.findById(bookingId);
            if (updatedBooking.getHostHandoverConfirmedAt() != null
                    && updatedBooking.getTenantHandoverConfirmedAt() != null) {
                bookingRepository.complete(bookingId);
                bookingRepository.updateRoomAvailability(booking.getRoomId(), "RENTED");
            }
        }
        notifyStatus(booking, host, host ? "handover-host" : "handover-tenant");
        return get(bookingId, jwt);
    }

    @Transactional
    public Map<String, Object> payment(Jwt jwt, CreatePaymentDTO request) {
        Booking booking = lockForParticipant(request.getBookingId(), jwt, false);
        checkActiveHold(booking);
        if (!request.isAcceptedTerms()) {
            throw DatabaseRepository.fail(400, "Bạn cần đồng ý điều khoản trước khi thanh toán.");
        }
        if (booking.getDepositVnd().signum() <= 0) {
            throw DatabaseRepository.fail(400, "Phòng không yêu cầu cọc; dùng xác nhận thuê.");
        }
        bookingRepository.expirePendingPayments(booking.getId());
        List<Map<String, Object>> existingPayments =
                bookingRepository.findReusablePayments(booking.getId());
        if (!existingPayments.isEmpty()) {
            return existingPayments.get(0);
        }
        long paymentId = bookingRepository.createPayment(booking);
        return bookingRepository.findPaymentDetails(paymentId);
    }

    @Transactional
    public Map<String, Object> simulate(long paymentId, Jwt jwt, SimulatePaymentDTO request) {
        if (!mockPaymentEnabled) {
            throw DatabaseRepository.fail(404, "Thanh toán mô phỏng đã tắt.");
        }
        Payment initialPayment = bookingRepository.findPayment(paymentId, false);
        Booking booking = lockForParticipant(initialPayment.getBookingId(), jwt, false);
        Payment payment = bookingRepository.findPayment(paymentId, true);
        if (!payment.isTest() || !"MOCK_SANDBOX".equals(payment.getProvider())) {
            throw DatabaseRepository.fail(403, "Chỉ mô phỏng thanh toán thử nghiệm.");
        }
        if ("SUCCEEDED".equals(payment.getStatus())) {
            return bookingRepository.findPaymentDetails(paymentId);
        }
        checkActiveHold(booking);
        if (!isFuture(payment.getExpiresAt()) || !"PENDING".equals(payment.getStatus())) {
            throw DatabaseRepository.fail(409, "Phiên thanh toán không còn hiệu lực.");
        }
        if (!"SUCCEEDED".equals(request.getStatus()) && !"FAILED".equals(request.getStatus())) {
            throw DatabaseRepository.fail(400, "Trạng thái mô phỏng không hợp lệ.");
        }
        boolean succeeded = "SUCCEEDED".equals(request.getStatus());
        bookingRepository.updatePaymentResult(paymentId, succeeded);
        if (succeeded) {
            if (booking.getTenantTermsAcceptedAt() == null) {
                throw DatabaseRepository.fail(409, "Chưa chấp nhận điều khoản.");
            }
            bookingRepository.allocatePayment(booking.getId(), paymentId);
            bookingRepository.createNotification(
                    booking.getHostId(),
                    "PAYMENT_STATUS",
                    "Đã nhận cọc mô phỏng",
                    payment.getAmountVnd().toPlainString(),
                    booking.getId());
        }
        return bookingRepository.findPaymentDetails(paymentId);
    }

    public List<Map<String, Object>> payments(long bookingId, Jwt jwt) {
        get(bookingId, jwt);
        return bookingRepository.findPaymentsByBookingId(bookingId);
    }

    @Transactional
    public Map<String, Object> review(Jwt jwt, CreateReviewDTO request) {
        Booking booking = lockForParticipant(request.getBookingId(), jwt, false);
        if (!"COMPLETED".equals(booking.getStatus())) {
            throw DatabaseRepository.fail(409, "Chỉ đánh giá sau khi hoàn tất bàn giao.");
        }
        return bookingRepository.createReview(
                booking, request.getRating(), trim(request.getComment()));
    }

    private Booking lockForParticipant(long bookingId, Jwt jwt, boolean host) {
        long currentUserId = DatabaseRepository.id(authService.user(jwt).get("id"));
        Booking booking = bookingRepository.findByIdForUpdate(bookingId);
        long participantId = host ? booking.getHostId() : booking.getTenantId();
        if (currentUserId != participantId) {
            throw DatabaseRepository.fail(403, "Bạn không có quyền thực hiện thao tác này.");
        }
        return booking;
    }

    private void checkActiveHold(Booking booking) {
        if (!"APPROVED".equals(booking.getStatus()) || !isFuture(booking.getHoldExpiresAt())) {
            throw DatabaseRepository.fail(409, "Yêu cầu chưa được duyệt hoặc đã hết hạn giữ chỗ.");
        }
    }

    private void notifyStatus(Booking booking, boolean host, String action) {
        long recipientId = host ? booking.getTenantId() : booking.getHostId();
        bookingRepository.createNotification(
                recipientId, "BOOKING_STATUS", "Cập nhật yêu cầu thuê", action, booking.getId());
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isFuture(Instant deadline) {
        return deadline != null && deadline.isAfter(Instant.now());
    }

    // Kept for the expiry job, which reads candidate rows directly from MySQL.
    public static boolean before(Map<String, Object> row, String field) {
        return row.get(field) != null && isFuture(Instant.parse(row.get(field).toString()));
    }
}
