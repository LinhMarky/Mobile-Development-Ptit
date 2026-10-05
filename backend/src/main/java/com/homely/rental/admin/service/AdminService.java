package com.homely.rental.admin.service;

import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.repository.RefreshTokenRepository;
import com.homely.rental.auth.repository.UserRepository;
import com.homely.rental.auth.security.AccountAccessService;
import com.homely.rental.booking.entity.BookingCase;
import com.homely.rental.booking.entity.BookingCaseDecision;
import com.homely.rental.booking.entity.CaseStatus;
import com.homely.rental.booking.repository.BookingCaseRepository;
import com.homely.rental.catalog.entity.Listing;
import com.homely.rental.catalog.entity.ListingStatus;
import com.homely.rental.catalog.repository.ListingRepository;
import com.homely.rental.common.audit.AuditService;
import com.homely.rental.common.exception.ConflictException;
import com.homely.rental.common.exception.ResourceNotFoundException;
import com.homely.rental.interaction.entity.Report;
import com.homely.rental.interaction.entity.ReportStatus;
import com.homely.rental.interaction.entity.Review;
import com.homely.rental.interaction.repository.ReportRepository;
import com.homely.rental.interaction.repository.ReviewRepository;
import com.homely.rental.notification.entity.NotificationType;
import com.homely.rental.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {
    private final ListingRepository listingRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    private final BookingCaseRepository bookingCaseRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokens;
    private final AccountAccessService accounts;
    private final AuditService audit;
    private final NotificationService notifications;
    private final com.homely.rental.booking.service.CaseSettlementService settlement;
    private final com.homely.rental.catalog.service.ListingService listingService;
    private final com.homely.rental.catalog.repository.RoomRepository rooms;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional(readOnly = true)
    public com.homely.rental.common.dto.PageResponse<com.homely.rental.catalog.dto.response.ListingDTO> pendingListings(org.springframework.data.domain.Pageable pageable) {
        accounts.requireAdmin();
        var page = listingRepository.findAll((root, query, cb) -> cb.equal(root.get("status"), ListingStatus.PENDING_REVIEW), pageable);
        return com.homely.rental.common.dto.PageResponse.of(page, page.getContent().stream().map(listingService::toDTO).toList());
    }

    public void approveListing(Long listingId) {
        User admin = accounts.requireAdmin();
        Listing listing = listing(listingId);
        requireStatus(listing, ListingStatus.PENDING_REVIEW);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setPublishedAt(now);
        listing.setExpiresAt(now.plus(30, ChronoUnit.DAYS));
        listing.setTermsVersion(listing.getRoom().getTermsVersion());
        listingRepository.save(listing);
        audit.record(admin, "LISTING_APPROVED", "listing", listingId, Map.of(
                "from", "PENDING_REVIEW", "to", "PUBLISHED", "expires_at", listing.getExpiresAt().toString()));
        notifications.createNotification(listing.getRoom().getHost(), NotificationType.LISTING_APPROVED,
                "Tin đăng đã được duyệt", "Tin đăng có hiệu lực trong 30 ngày.", "listing", listingId);
    }

    @Transactional(readOnly = true)
    public com.homely.rental.common.dto.PageResponse<com.homely.rental.booking.dto.BookingCaseDTO> openCases(org.springframework.data.domain.Pageable pageable) {
        accounts.requireAdmin();
        var page = bookingCaseRepository.findByStatusInOrderByCreatedAtAsc(java.util.List.of(CaseStatus.OPEN, CaseStatus.IN_REVIEW), pageable);
        return com.homely.rental.common.dto.PageResponse.of(page, page.getContent().stream().map(com.homely.rental.booking.dto.BookingCaseDTO::of).toList());
    }

    public void rejectListing(Long listingId, String reason) {
        User admin = accounts.requireAdmin();
        reason = reason(reason, 500);
        Listing listing = listing(listingId);
        requireStatus(listing, ListingStatus.PENDING_REVIEW);
        listing.setStatus(ListingStatus.REJECTED);
        listingRepository.save(listing);
        audit.record(admin, "LISTING_REJECTED", "listing", listingId, Map.of(
                "from", "PENDING_REVIEW", "to", "REJECTED", "reason", reason));
        notifications.createNotification(listing.getRoom().getHost(), NotificationType.LISTING_REJECTED,
                "Tin đăng chưa được duyệt", reason, "listing", listingId);
    }

    public void suspendListing(Long listingId, String reason) {
        User admin = accounts.requireAdmin();
        reason = reason(reason, 500);
        Listing listing = listing(listingId);
        ListingStatus before = listing.getStatus();
        if (before != ListingStatus.PUBLISHED && before != ListingStatus.HIDDEN) {
            throw conflict("Only PUBLISHED or HIDDEN listings can be suspended");
        }
        listing.setStatus(ListingStatus.SUSPENDED);
        listingRepository.save(listing);
        audit.record(admin, "LISTING_SUSPENDED", "listing", listingId, Map.of(
                "from", before.name(), "to", "SUSPENDED", "reason", reason));
        notifications.createNotification(listing.getRoom().getHost(), NotificationType.LISTING_SUSPENDED,
                "Tin đăng đã bị tạm ngưng", reason, "listing", listingId);
    }

    /** Records the adjudication and sandbox settlement in the same transaction. */
    public void resolveBookingCase(Long caseId, BookingCaseDecision decision, String note) {
        resolveBookingCase(caseId, decision, note, null);
    }

    public void resolveBookingCase(Long caseId, BookingCaseDecision decision, String note, java.math.BigDecimal tenantRefund) {
        User admin = accounts.requireAdmin();
        note = reason(note, 2000);
        if (decision == null) throw new IllegalArgumentException("decision is required");
        BookingCase bookingCase = bookingCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("BookingCase", caseId));
        if (bookingCase.getStatus() != CaseStatus.OPEN && bookingCase.getStatus() != CaseStatus.IN_REVIEW) {
            throw conflict("Case is already resolved or closed");
        }
        CaseStatus before = bookingCase.getStatus();
        bookingCase.setStatus(CaseStatus.RESOLVED);
        bookingCase.setDecision(decision);
        bookingCase.setResolutionNote(note);
        bookingCase.setResolvedBy(admin);
        bookingCase.setResolvedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        settlement.settle(bookingCase, decision, tenantRefund);
        bookingCaseRepository.save(bookingCase);
        audit.record(admin, "BOOKING_CASE_RESOLVED", "booking_case", caseId, Map.of(
                "from", before.name(), "to", "RESOLVED", "decision", decision.name(), "note", note));
        var booking = bookingCase.getBooking();
        notifications.createNotification(booking.getTenant(), NotificationType.BOOKING_CASE_RESOLVED,
                "Tranh chấp đã có kết luận", note, "booking", booking.getId());
        notifications.createNotification(booking.getHost(), NotificationType.BOOKING_CASE_RESOLVED,
                "Tranh chấp đã có kết luận", note, "booking", booking.getId());
    }

    public void suspendUser(Long userId, String reason) {
        User admin = accounts.requireAdmin();
        reason = reason(reason, 500);
        if (admin.getId().equals(userId)) {
            throw new ConflictException("SELF_ACTION_NOT_ALLOWED", "You cannot suspend your own account");
        }
        User user = user(userId);
        if (user.getStatus() != UserStatus.ACTIVE || user.isSuspended()) throw conflict("Account is not active");
        user.setStatus(UserStatus.SUSPENDED);
        user.setSuspended(true);
        user.setSuspendReason(reason);
        userRepository.saveAndFlush(user);
        int revoked = refreshTokens.revokeAllByUserId(userId);
        audit.record(admin, "USER_SUSPENDED", "user", userId, Map.of("reason", reason, "revoked_sessions", revoked));
        notifications.createNotification(user, NotificationType.USER_SUSPENDED,
                "Tài khoản đã bị tạm khóa", reason, "user", userId);
    }

    public void unsuspendUser(Long userId) {
        User admin = accounts.requireAdmin();
        User user = user(userId);
        // Accept the legacy suspended flag, but never reactivate deleted/deactivated accounts.
        if (user.getStatus() != UserStatus.SUSPENDED
                && !(user.getStatus() == UserStatus.ACTIVE && user.isSuspended())) {
            throw conflict("Account is not suspended");
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setSuspended(false);
        user.setSuspendReason(null);
        userRepository.save(user);
        audit.record(admin, "USER_RESTORED", "user", userId, Map.of("to", "ACTIVE"));
        notifications.createNotification(user, NotificationType.USER_RESTORED,
                "Tài khoản đã được mở khóa", "Bạn cần đăng nhập lại để sử dụng ứng dụng.", "user", userId);
    }

    public void resolveReport(Long reportId, String note) {
        User admin = accounts.requireAdmin();
        note = reason(note, 1000);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));
        if (report.getStatus() != ReportStatus.PENDING && report.getStatus() != ReportStatus.REVIEWING) {
            throw conflict("Report is already resolved or dismissed");
        }
        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedBy(admin);
        report.setResolutionNote(note);
        report.setResolvedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        reportRepository.save(report);
        audit.record(admin, "REPORT_RESOLVED", "report", reportId, Map.of("note", note));
        notifications.createNotification(report.getReporter(), NotificationType.REPORT_RESOLVED,
                "Báo cáo của bạn đã được xử lý", note, "report", reportId);
    }

    public void hideReview(Long reviewId, String reason) {
        User admin = accounts.requireAdmin();
        reason = reason(reason, 500);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        if (!review.isVisible()) throw conflict("Review is already hidden");
        review.setVisible(false);
        review.setHideReason(reason);
        reviewRepository.save(review);
        audit.record(admin, "REVIEW_HIDDEN", "review", reviewId, Map.of("reason", reason));
    }

    private Listing listing(Long id) {
        Long roomId = listingRepository.findRoomId(id).orElseThrow(() -> new ResourceNotFoundException("Listing", id));
        rooms.lockById(roomId).orElseThrow();
        Listing result = listingRepository.findById(id).orElseThrow();
        entityManager.refresh(result);
        return result;
    }

    private User user(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void requireStatus(Listing listing, ListingStatus expected) {
        if (listing.getStatus() != expected) throw conflict("Listing must be " + expected);
    }

    private ConflictException conflict(String message) {
        return new ConflictException("INVALID_STATUS_TRANSITION", message);
    }

    private String reason(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException("A nonblank reason within the length limit is required");
        }
        return value.trim();
    }
}
