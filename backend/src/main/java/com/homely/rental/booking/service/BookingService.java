package com.homely.rental.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.security.UserResolver;
import com.homely.rental.booking.dto.BookingActionRequest;
import com.homely.rental.booking.dto.BookingCreateRequest;
import com.homely.rental.booking.dto.BookingDTO;
import com.homely.rental.booking.entity.*;
import com.homely.rental.booking.repository.BookingRepository;
import com.homely.rental.booking.repository.BookingStatusHistoryRepository;
import com.homely.rental.catalog.entity.Listing;
import com.homely.rental.catalog.entity.ListingStatus;
import com.homely.rental.catalog.entity.Room;
import com.homely.rental.catalog.entity.RoomAvailability;
import com.homely.rental.catalog.repository.ListingRepository;
import com.homely.rental.catalog.repository.RoomRepository;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.ConflictException;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Booking service (BOOK01–BOOK09, BR-06, BR-07).
 * Implements the full booking state machine with concurrency control.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final RoomRepository roomRepository;
    private final ListingRepository listingRepository;
    private final UserResolver userResolver;
    private final ObjectMapper objectMapper;
    private final BookingLocks locks;
    private final BookingTransitions transitions;
    private final com.homely.rental.payment.repository.PaymentRepository payments;
    private final com.homely.rental.payment.repository.RefundRepository refunds;
    private final com.homely.rental.payment.service.RefundService refundService;
    private final com.homely.rental.booking.repository.BookingCaseRepository cases;
    private final com.homely.rental.catalog.repository.ListingFeeRepository fees;

    private static final long REQUEST_EXPIRY_HOURS = 24;
    private static final long HOLD_EXPIRY_HOURS = 48;
    private static final long HANDOVER_DUE_DAYS = 7;

    // BOOK01: Create booking (Tenant+Verified)
    @Transactional
    public BookingDTO createBooking(BookingCreateRequest dto) throws IdInvalidException {
        User tenant = getVerifiedUser();

        Room room = roomRepository.lockById(dto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", dto.getRoomId()));

        // BR-01.3: Self-action check
        if (room.getHost().getId().equals(tenant.getId())) {
            throw new ConflictException("SELF_ACTION_NOT_ALLOWED", "Cannot book your own room");
        }

        // BR-06: Room must be AVAILABLE
        if (room.getAvailability() != RoomAvailability.AVAILABLE) {
            throw new ConflictException("ROOM_NOT_AVAILABLE",
                    "Room is not available (current: " + room.getAvailability() + ")");
        }

        // Find active listing for this room
        Listing listing = listingRepository.findByRoomId(room.getId())
                .orElseThrow(() -> new IdInvalidException("Room has no listing"));
        if (listing.getStatus() != ListingStatus.PUBLISHED
                || (listing.getExpiresAt() != null && !listing.getExpiresAt().isAfter(Instant.now()))) {
            throw new IdInvalidException("Listing is not published");
        }
        if (dto.getOccupantCount() > room.getMaxOccupants()) throw new IllegalArgumentException("Occupant count exceeds room capacity");
        if (bookingRepository.existsByTenantIdAndRoomIdAndStatusIn(tenant.getId(), room.getId(),
                java.util.List.of(BookingStatus.PENDING, BookingStatus.APPROVED, BookingStatus.CONFIRMED))) {
            throw new ConflictException("DUPLICATE_BOOKING", "You already have an open booking for this room");
        }

        // Build terms snapshot
        String snapshot = buildTermsSnapshot(room, listing);

        Booking booking = new Booking();
        booking.setTenant(tenant);
        booking.setHost(room.getHost());
        booking.setRoom(room);
        booking.setListing(listing);
        booking.setStatus(BookingStatus.PENDING);
        booking.setDesiredMoveIn(dto.getDesiredMoveIn());
        booking.setOccupantCount(dto.getOccupantCount());
        booking.setRentVnd(listing.getRentVnd());
        booking.setDepositVnd(listing.getDepositVnd());
        booking.setNote(dto.getNote());
        booking.setTermsSnapshot(snapshot);
        booking.setRequestExpiresAt(Instant.now().plus(REQUEST_EXPIRY_HOURS, ChronoUnit.HOURS));

        try {
            Booking saved = bookingRepository.save(booking);
            recordHistory(saved, null, BookingStatus.PENDING, tenant, ActorType.TENANT, null);
            log.info("Booking created: id={}, tenant={}, room={}", saved.getId(), tenant.getId(), room.getId());
            return toDTO(saved);
        } catch (DataIntegrityViolationException e) {
            // BR-07: Generated UNIQUE constraint violated
            if (e.getMessage() != null && e.getMessage().contains("uk_bookings")) {
                throw new ConflictException("ROOM_NOT_AVAILABLE",
                        "Room already has an active booking or you already have a pending booking for this room");
            }
            throw e;
        }
    }

    // BOOK02: List tenant's bookings
    @Transactional(readOnly = true)
    public PageResponse<BookingDTO> getTenantBookings(Pageable pageable) throws IdInvalidException {
        User tenant = userResolver.requireCurrent();
        Page<Booking> page = bookingRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId(), pageable);
        return PageResponse.of(page, page.getContent().stream().map(this::toDTO).toList());
    }

    // BOOK03: List host's bookings
    @Transactional(readOnly = true)
    public PageResponse<BookingDTO> getHostBookings(Pageable pageable) throws IdInvalidException {
        User host = getVerifiedHost();
        Page<Booking> page = bookingRepository.findByHostIdOrderByCreatedAtDesc(host.getId(), pageable);
        return PageResponse.of(page, page.getContent().stream().map(this::toDTO).toList());
    }

    // BOOK04: Approve booking (Host)
    @Transactional
    public BookingDTO approveBooking(Long bookingId) throws IdInvalidException {
        User host = getVerifiedHost();
        Booking booking = owned(bookingId, host.getId(), true);

        assertStatus(booking, BookingStatus.PENDING, "approve");
        if (!booking.getRequestExpiresAt().isAfter(Instant.now())) throw new ConflictException("BOOKING_EXPIRED", "Request has expired");
        if (booking.getRoom().getAvailability() != RoomAvailability.AVAILABLE
                || bookingRepository.existsByRoomIdAndStatusIn(booking.getRoom().getId(),
                java.util.List.of(BookingStatus.APPROVED, BookingStatus.CONFIRMED))) {
            throw new ConflictException("ROOM_NOT_AVAILABLE", "Room already held or rented");
        }

        // Transition: PENDING → APPROVED
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.APPROVED);
        booking.setHoldExpiresAt(Instant.now().plus(HOLD_EXPIRY_HOURS, ChronoUnit.HOURS));

        // Mark room as HELD
        Room room = booking.getRoom();
        room.setAvailability(RoomAvailability.HELD);
        roomRepository.save(room);

        Booking saved = bookingRepository.save(booking);
        recordHistory(saved, from, BookingStatus.APPROVED, host, ActorType.HOST, null);
        log.info("Booking approved: id={}", bookingId);
        return toDTO(saved);
    }

    // BOOK05: Reject booking (Host)
    @Transactional
    public BookingDTO rejectBooking(Long bookingId, BookingActionRequest action) throws IdInvalidException {
        User host = getVerifiedHost();
        Booking booking = owned(bookingId, host.getId(), true);

        assertStatus(booking, BookingStatus.PENDING, "reject");

        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        booking.setLastReason(action != null ? action.getReason() : "Rejected by host");

        Booking saved = bookingRepository.save(booking);
        recordHistory(saved, from, BookingStatus.CANCELLED, host, ActorType.HOST, booking.getLastReason());
        return toDTO(saved);
    }

    // BOOK06: Confirm only a successful allocated payment; webhook normally performs this transition.
    @Transactional
    public BookingDTO confirmDeposit(Long bookingId) throws IdInvalidException {
        User tenant = userResolver.requireCurrent();
        Booking booking = owned(bookingId, tenant.getId(), false);
        var payment = booking.getAllocatedPaymentId() == null ? null : payments.findById(booking.getAllocatedPaymentId()).orElse(null);
        if (payment == null || !payment.getBooking().getId().equals(bookingId)
                || payment.getStatus() != com.homely.rental.payment.entity.PaymentStatus.SUCCEEDED
                || payment.getAmountVnd().compareTo(booking.getDepositVnd()) != 0
                || refunds.findByPaymentId(payment.getId()).isPresent()) {
            throw new ConflictException("PAYMENT_REQUIRED", "A successful allocated deposit is required");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) return toDTO(booking);
        assertStatus(booking, BookingStatus.APPROVED, "confirm deposit");
        if (booking.getHoldExpiresAt() == null || !booking.getHoldExpiresAt().isAfter(Instant.now())) {
            throw new ConflictException("BOOKING_EXPIRED", "Hold has expired");
        }
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTenantTermsAcceptedAt(Instant.now());
        booking.setHandoverDueAt(Instant.now().plus(HANDOVER_DUE_DAYS, ChronoUnit.DAYS));

        Booking saved = bookingRepository.save(booking);
        recordHistory(saved, from, BookingStatus.CONFIRMED, tenant, ActorType.TENANT, null);
        return toDTO(saved);
    }

    // BOOK07: Tenant confirms handover
    @Transactional
    public BookingDTO tenantHandover(Long bookingId) throws IdInvalidException {
        User tenant = userResolver.requireCurrent();
        Booking booking = owned(bookingId, tenant.getId(), false);

        assertStatus(booking, BookingStatus.CONFIRMED, "handover");

        booking.setTenantHandoverConfirmedAt(Instant.now());
        return checkAndCompleteHandover(booking, tenant, ActorType.TENANT);
    }

    // BOOK08: Host confirms handover
    @Transactional
    public BookingDTO hostHandover(Long bookingId) throws IdInvalidException {
        User host = getVerifiedHost();
        Booking booking = owned(bookingId, host.getId(), true);

        assertStatus(booking, BookingStatus.CONFIRMED, "handover");

        booking.setHostHandoverConfirmedAt(Instant.now());
        return checkAndCompleteHandover(booking, host, ActorType.HOST);
    }

    // BOOK09: Cancel booking (Auth — tenant or host, depending on status)
    @Transactional
    public BookingDTO cancelBooking(Long bookingId, BookingActionRequest action) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        Booking booking = locks.lock(bookingId);

        boolean isTenant = booking.getTenant().getId().equals(user.getId());
        boolean isHost = booking.getHost().getId().equals(user.getId());
        if (!isTenant && !isHost) {
            throw new ResourceNotFoundException("Booking", bookingId);
        }

        // Allow cancel from PENDING (anyone) or APPROVED (anyone, refund if paid)
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.APPROVED) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Cannot cancel booking in status: " + booking.getStatus() +
                    ". For CONFIRMED bookings, use dispute resolution.");
        }

        BookingStatus from = booking.getStatus();

        // If was APPROVED, release room
        if (from == BookingStatus.APPROVED) {
            Room room = booking.getRoom();
            room.setAvailability(RoomAvailability.AVAILABLE);
            roomRepository.save(room);
            refundSuccessfulPayments(booking);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        booking.setLastReason(action != null ? action.getReason() : null);

        ActorType actorType = isTenant ? ActorType.TENANT : ActorType.HOST;
        Booking saved = bookingRepository.save(booking);
        recordHistory(saved, from, BookingStatus.CANCELLED, user, actorType, booking.getLastReason());
        return toDTO(saved);
    }

    // ===== Internal: Check both handover confirmations =====

    private BookingDTO checkAndCompleteHandover(Booking booking, User actor, ActorType actorType) {
        if (booking.getTenantHandoverConfirmedAt() != null && booking.getHostHandoverConfirmedAt() != null) {
            // Both confirmed → COMPLETED
            BookingStatus from = booking.getStatus();
            booking.setStatus(BookingStatus.COMPLETED);
            booking.setCompletedAt(Instant.now());

            // Mark room as RENTED
            Room room = booking.getRoom();
            room.setAvailability(RoomAvailability.RENTED);
            roomRepository.save(room);

            Booking saved = bookingRepository.save(booking);
            recordHistory(saved, from, BookingStatus.COMPLETED, actor, actorType, "Both parties confirmed handover");
            log.info("Booking completed: id={}", booking.getId());
            return toDTO(saved);
        }

        // Only one side confirmed so far
        Booking saved = bookingRepository.save(booking);
        recordHistory(saved, BookingStatus.CONFIRMED, BookingStatus.CONFIRMED, actor, actorType, "Handover confirmed by " + actorType);
        return toDTO(saved);
    }

    // ===== Expiration methods (called by scheduled jobs) =====

    @Transactional
    public void expirePendingBookings() {
        var expired = bookingRepository.findExpiredPendingBookings(Instant.now());
        for (Booking candidate : expired) {
            Booking b = locks.lock(candidate.getId());
            if (b.getStatus() != BookingStatus.PENDING || b.getRequestExpiresAt().isAfter(Instant.now())) continue;
            b.setStatus(BookingStatus.EXPIRED);
            b.setCancelledAt(Instant.now());
            b.setLastReason("Request expired");
            bookingRepository.save(b);
            recordHistory(b, BookingStatus.PENDING, BookingStatus.EXPIRED, null, ActorType.SYSTEM, "Expired");
        }
        if (!expired.isEmpty()) log.info("Expired {} pending bookings", expired.size());
    }

    @Transactional
    public void expireApprovedBookings() {
        var expired = bookingRepository.findExpiredApprovedBookings(Instant.now());
        for (Booking candidate : expired) {
            Booking b = locks.lock(candidate.getId());
            if (b.getStatus() != BookingStatus.APPROVED || b.getHoldExpiresAt().isAfter(Instant.now())) continue;
            b.setStatus(BookingStatus.EXPIRED);
            b.setCancelledAt(Instant.now());
            b.setLastReason("Hold expired - deposit not received in time");

            // Release room
            Room room = b.getRoom();
            room.setAvailability(RoomAvailability.AVAILABLE);
            roomRepository.save(room);

            bookingRepository.save(b);
            recordHistory(b, BookingStatus.APPROVED, BookingStatus.EXPIRED, null, ActorType.SYSTEM, "Hold expired");
            refundSuccessfulPayments(b);
        }
        if (!expired.isEmpty()) log.info("Expired {} approved bookings (hold timeout)", expired.size());
    }

    @Transactional
    public void handleOverdueHandovers() {
        var overdue = bookingRepository.findOverdueHandovers(Instant.now());
        for (Booking candidate : overdue) {
            Booking b = locks.lock(candidate.getId());
            if (b.getStatus() != BookingStatus.CONFIRMED || b.getHandoverDueAt().isAfter(Instant.now())
                    || cases.existsByBookingIdAndStatusIn(b.getId(), java.util.List.of(CaseStatus.OPEN, CaseStatus.IN_REVIEW))) continue;
            BookingCase item = new BookingCase();
            item.setBooking(b);
            item.setType(BookingCaseType.HANDOVER_OVERDUE);
            item.setDescription("Both parties have not confirmed handover by the deadline");
            cases.save(item);
        }
    }

    // ===== DTO & Helpers =====

    @Transactional(readOnly = true)
    public BookingDTO getBooking(Long id) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        Booking b = bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        if (!b.getTenant().getId().equals(user.getId()) && !b.getHost().getId().equals(user.getId()))
            throw new ResourceNotFoundException("Booking", id);
        return toDTO(b);
    }

    @Transactional
    public com.homely.rental.booking.dto.BookingCaseDTO openCase(Long id, com.homely.rental.booking.dto.CaseCreateRequest request) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        Booking b = locks.lock(id);
        if (!b.getTenant().getId().equals(user.getId()) && !b.getHost().getId().equals(user.getId())) throw new ResourceNotFoundException("Booking", id);
        assertStatus(b, BookingStatus.CONFIRMED, "open case");
        if (cases.existsByBookingIdAndStatusIn(id, java.util.List.of(CaseStatus.OPEN, CaseStatus.IN_REVIEW)))
            throw new ConflictException("CASE_EXISTS", "An open case already exists");
        BookingCase item = new BookingCase(); item.setBooking(b); item.setOpenedBy(user);
        item.setType(request.type()); item.setDescription(request.description());
        return com.homely.rental.booking.dto.BookingCaseDTO.of(cases.save(item));
    }

    @Transactional(readOnly = true)
    public java.util.List<com.homely.rental.booking.dto.BookingCaseDTO> getCases(Long id) throws IdInvalidException {
        getBooking(id);
        return cases.findByBookingIdOrderByCreatedAtDesc(id).stream().map(com.homely.rental.booking.dto.BookingCaseDTO::of).toList();
    }

    public BookingDTO toDTO(Booking b) {
        return BookingDTO.builder()
                .id(b.getId())
                .tenantId(b.getTenant().getId())
                .hostId(b.getHost().getId())
                .roomId(b.getRoom().getId())
                .listingId(b.getListing().getId())
                .status(b.getStatus().name())
                .desiredMoveIn(b.getDesiredMoveIn())
                .occupantCount(b.getOccupantCount())
                .rentVnd(b.getRentVnd())
                .depositVnd(b.getDepositVnd())
                .note(b.getNote())
                .requestExpiresAt(b.getRequestExpiresAt())
                .holdExpiresAt(b.getHoldExpiresAt())
                .handoverDueAt(b.getHandoverDueAt())
                .completedAt(b.getCompletedAt())
                .cancelledAt(b.getCancelledAt())
                .lastReason(b.getLastReason())
                .version(b.getVersion())
                .createdAt(b.getCreatedAt())
                .build();
    }

    private String buildTermsSnapshot(Room room, Listing listing) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("room_id", room.getId());
            snapshot.put("listing_id", listing.getId());
            snapshot.put("terms_version", room.getTermsVersion());
            snapshot.put("rent_vnd", listing.getRentVnd());
            snapshot.put("deposit_vnd", listing.getDepositVnd());
            snapshot.put("room_type", room.getRoomType().name());
            snapshot.put("area_m2", room.getAreaM2());
            snapshot.put("max_occupants", room.getMaxOccupants());
            snapshot.put("fees", fees.findByListingIdOrderBySortOrder(listing.getId()).stream().map(f -> {
                Map<String, Object> fee = new LinkedHashMap<>();
                fee.put("fee_code", f.getFeeCode()); fee.put("fee_mode", f.getFeeMode());
                fee.put("amount_vnd", f.getAmountVnd()); fee.put("unit_name", f.getUnitName());
                fee.put("note", f.getNote()); return fee;
            }).toList());
            snapshot.put("snapshot_at", Instant.now().toString());
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize terms snapshot", e);
        }
    }

    private void recordHistory(Booking booking, BookingStatus from, BookingStatus to,
                               User actor, ActorType actorType, String reason) {
        transitions.record(booking, from, to, actor, actorType, reason);
    }

    private Booking owned(Long id, Long userId, boolean host) {
        Booking booking = locks.lock(id);
        if (!(host ? booking.getHost() : booking.getTenant()).getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking", id);
        }
        return booking;
    }

    private void refundSuccessfulPayments(Booking booking) {
        for (var payment : payments.findByBookingIdOrderByCreatedAtDesc(booking.getId())) {
            if (payment.getStatus() == com.homely.rental.payment.entity.PaymentStatus.SUCCEEDED) {
                refundService.refund(payment, com.homely.rental.payment.entity.RefundReason.BOOKING_EXPIRED, payment.getAmountVnd());
            }
        }
    }

    private void assertStatus(Booking booking, BookingStatus expected, String action) {
        if (booking.getStatus() != expected) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Cannot " + action + " booking in status: " + booking.getStatus());
        }
    }


    private User getVerifiedUser() throws IdInvalidException {
        User user = userResolver.requireCurrent();
        if (!user.isEmailVerified()) throw new IdInvalidException("Email must be verified");
        return user;
    }

    private User getVerifiedHost() throws IdInvalidException {
        User user = getVerifiedUser();
        if (!user.isHost()) throw new IdInvalidException("User does not have HOST role");
        return user;
    }
}
