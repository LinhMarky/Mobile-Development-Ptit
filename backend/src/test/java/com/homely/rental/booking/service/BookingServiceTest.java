package com.homely.rental.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.auth.entity.*;
import com.homely.rental.auth.security.UserResolver;
import com.homely.rental.booking.dto.*;
import com.homely.rental.booking.entity.*;
import com.homely.rental.booking.repository.*;
import com.homely.rental.catalog.entity.*;
import com.homely.rental.catalog.repository.*;
import com.homely.rental.common.exception.*;
import com.homely.rental.payment.entity.*;
import com.homely.rental.payment.repository.*;
import com.homely.rental.payment.service.RefundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Domain state-machine checks, independent of HTTP and database availability. */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock BookingRepository bookingRepository;
    @Mock BookingStatusHistoryRepository historyRepository;
    @Mock RoomRepository roomRepository;
    @Mock ListingRepository listingRepository;
    @Mock UserResolver userResolver;
    @Spy ObjectMapper objectMapper = new ObjectMapper();
    @Mock BookingLocks locks;
    @Mock BookingTransitions transitions;
    @Mock PaymentRepository payments;
    @Mock RefundRepository refunds;
    @Mock RefundService refundService;
    @Mock BookingCaseRepository cases;
    @Mock ListingFeeRepository fees;
    @InjectMocks BookingService service;

    User tenant, host, outsider;
    Room room;
    Listing listing;
    Booking booking;

    @BeforeEach void fixture() {
        tenant = user(1L); host = user(2L); outsider = user(3L);
        Role role = new Role(); role.setName(RoleName.ROLE_HOST); host.getRoles().add(role);
        room = new Room(); room.setId(10L); room.setHost(host); room.setAvailability(RoomAvailability.AVAILABLE);
        room.setRoomType(RoomType.SINGLE_ROOM); room.setAreaM2(new BigDecimal("25")); room.setMaxOccupants(2);
        listing = new Listing(); listing.setId(20L); listing.setRoom(room); listing.setStatus(ListingStatus.PUBLISHED);
        listing.setRentVnd(new BigDecimal("3000000")); listing.setDepositVnd(new BigDecimal("1000000"));
        listing.setExpiresAt(Instant.now().plusSeconds(86400));
        booking = new Booking(); booking.setId(30L); booking.setTenant(tenant); booking.setHost(host);
        booking.setRoom(room); booking.setListing(listing); booking.setRentVnd(listing.getRentVnd());
        booking.setDepositVnd(listing.getDepositVnd()); booking.setRequestExpiresAt(Instant.now().plusSeconds(3600));
    }

    private User user(long id) {
        User user = new User(); user.setId(id); user.setEmail("user" + id + "@example.test");
        user.setEmailVerified(true); return user;
    }
    private void actor(User user) { when(userResolver.requireCurrent()).thenReturn(user); }
    private void locked() { when(locks.lock(booking.getId())).thenReturn(booking); }
    private void saves() { when(bookingRepository.save(any(Booking.class))).thenAnswer(call -> call.getArgument(0)); }
    private BookingCreateRequest request() { var request = new BookingCreateRequest(); request.setRoomId(room.getId()); return request; }

    @Test void createSnapshotsPriceAndGivesRequest24HoursWithoutHoldingRoom() throws Exception {
        actor(tenant); when(roomRepository.lockById(room.getId())).thenReturn(Optional.of(room));
        when(listingRepository.findByRoomId(room.getId())).thenReturn(Optional.of(listing)); saves();
        Instant before = Instant.now(); service.createBooking(request()); Instant after = Instant.now();
        var capture = ArgumentCaptor.forClass(Booking.class); verify(bookingRepository).save(capture.capture());
        Booking saved = capture.getValue();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(saved.getRequestExpiresAt()).isBetween(before.plusSeconds(86400), after.plusSeconds(86400));
        assertThat(saved.getDepositVnd()).isEqualByComparingTo("1000000");
        assertThat(objectMapper.readTree(saved.getTermsSnapshot()).get("rent_vnd").decimalValue()).isEqualByComparingTo("3000000");
        assertThat(room.getAvailability()).isEqualTo(RoomAvailability.AVAILABLE);
        verify(transitions).record(saved, null, BookingStatus.PENDING, tenant, ActorType.TENANT, null);
    }

    @Test void hostCannotBookOwnRoom() {
        actor(host); when(roomRepository.lockById(room.getId())).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> service.createBooking(request())).isInstanceOfSatisfying(ConflictException.class,
                error -> assertThat(error.getCode()).isEqualTo("SELF_ACTION_NOT_ALLOWED"));
        verifyNoInteractions(bookingRepository, transitions);
    }

    @Test void unverifiedAccountCannotCreateBooking() {
        tenant.setEmailVerified(false); actor(tenant);
        assertThatThrownBy(() -> service.createBooking(request())).isInstanceOf(IdInvalidException.class);
        verifyNoInteractions(roomRepository, bookingRepository, transitions);
    }

    @Test void approveHoldsRoomFor48HoursAndRecordsHostTransition() throws Exception {
        actor(host); locked(); saves(); Instant before = Instant.now(); service.approveBooking(booking.getId());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(booking.getHoldExpiresAt()).isBetween(before.plusSeconds(172800), Instant.now().plusSeconds(172800));
        assertThat(room.getAvailability()).isEqualTo(RoomAvailability.HELD);
        verify(transitions).record(booking, BookingStatus.PENDING, BookingStatus.APPROVED, host, ActorType.HOST, null);
    }

    @ParameterizedTest @EnumSource(value=BookingStatus.class, names="PENDING", mode=EnumSource.Mode.EXCLUDE)
    void approveRejectsEveryNonPendingState(BookingStatus status) {
        actor(host); locked(); booking.setStatus(status);
        assertThatThrownBy(() -> service.approveBooking(booking.getId())).isInstanceOf(ConflictException.class);
        assertThat(booking.getStatus()).isEqualTo(status);
        verifyNoInteractions(transitions); verify(bookingRepository, never()).save(any());
    }

    @Test void approvedBookingCannotStealAnotherActiveHold() {
        actor(host); locked();
        when(bookingRepository.existsByRoomIdAndStatusIn(eq(room.getId()), anyList())).thenReturn(true);
        assertThatThrownBy(() -> service.approveBooking(booking.getId())).isInstanceOfSatisfying(ConflictException.class,
                error -> assertThat(error.getCode()).isEqualTo("ROOM_NOT_AVAILABLE"));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING); verifyNoInteractions(transitions);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void bothHandoverOrdersCompleteOnlyAfterSecondParty(boolean tenantFirst) throws Exception {
        booking.setStatus(BookingStatus.CONFIRMED); room.setAvailability(RoomAvailability.HELD); locked(); saves();
        actor(tenantFirst ? tenant : host);
        if (tenantFirst) service.tenantHandover(booking.getId()); else service.hostHandover(booking.getId());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(room.getAvailability()).isEqualTo(RoomAvailability.HELD);
        assertThat(booking.getCompletedAt()).isNull();
        actor(tenantFirst ? host : tenant);
        if (tenantFirst) service.hostHandover(booking.getId()); else service.tenantHandover(booking.getId());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(room.getAvailability()).isEqualTo(RoomAvailability.RENTED);
        assertThat(booking.getCompletedAt()).isNotNull();
        verify(transitions).record(eq(booking), eq(BookingStatus.CONFIRMED), eq(BookingStatus.COMPLETED),
                eq(tenantFirst ? host : tenant), eq(tenantFirst ? ActorType.HOST : ActorType.TENANT), anyString());
    }

    @Test void tenantCannotPerformHostHandover() {
        actor(tenant);
        assertThatThrownBy(() -> service.hostHandover(booking.getId())).isInstanceOf(IdInvalidException.class);
        verifyNoInteractions(locks, transitions);
    }

    @Test void outsiderCannotCancelOrConfirmHandover() {
        actor(outsider); locked(); booking.setStatus(BookingStatus.CONFIRMED);
        assertThatThrownBy(() -> service.cancelBooking(booking.getId(), null)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.tenantHandover(booking.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(booking.getTenantHandoverConfirmedAt()).isNull(); verifyNoInteractions(transitions, refundService);
    }

    @ParameterizedTest @EnumSource(value=BookingStatus.class, names={"CONFIRMED","COMPLETED","CANCELLED","EXPIRED"})
    void cannotCancelConfirmedOrTerminalBooking(BookingStatus status) {
        actor(tenant); locked(); booking.setStatus(status);
        assertThatThrownBy(() -> service.cancelBooking(booking.getId(), null)).isInstanceOf(ConflictException.class);
        assertThat(booking.getStatus()).isEqualTo(status); verifyNoInteractions(transitions, refundService);
    }

    @Test void cancelPendingRequestDoesNotReleaseAnotherTenantsHeldRoom() throws Exception {
        actor(tenant); locked(); saves(); room.setAvailability(RoomAvailability.HELD);
        service.cancelBooking(booking.getId(), null);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(room.getAvailability()).isEqualTo(RoomAvailability.HELD);
        verifyNoInteractions(roomRepository, payments, refundService);
    }

    @Test void cancelApprovedBookingReleasesRoomAndRefundsOnlySuccessfulPayments() throws Exception {
        actor(host); locked(); saves(); booking.setStatus(BookingStatus.APPROVED); room.setAvailability(RoomAvailability.HELD);
        Payment paid = new Payment(); paid.setStatus(PaymentStatus.SUCCEEDED); paid.setAmountVnd(new BigDecimal("1000000"));
        Payment pending = new Payment(); Payment failed = new Payment(); failed.setStatus(PaymentStatus.FAILED);
        when(payments.findByBookingIdOrderByCreatedAtDesc(booking.getId())).thenReturn(List.of(paid, pending, failed));
        service.cancelBooking(booking.getId(), null);
        assertThat(room.getAvailability()).isEqualTo(RoomAvailability.AVAILABLE);
        assertThat(booking.getCancelledAt()).isNotNull();
        verify(refundService).refund(paid, RefundReason.BOOKING_EXPIRED, paid.getAmountVnd());
        verifyNoMoreInteractions(refundService);
    }

    @Test void expiryRechecksLockedStatusInsteadOfExpiringNewlyConfirmedBooking() {
        booking.setStatus(BookingStatus.CONFIRMED); room.setAvailability(RoomAvailability.HELD); locked();
        Booking stale = new Booking(); stale.setId(booking.getId()); stale.setStatus(BookingStatus.APPROVED);
        when(bookingRepository.findExpiredApprovedBookings(any())).thenReturn(List.of(stale));
        service.expireApprovedBookings();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(room.getAvailability()).isEqualTo(RoomAvailability.HELD);
        verifyNoInteractions(transitions, refundService); verify(bookingRepository, never()).save(any());
    }

    @Test void expiryOfPendingRequestLeavesExistingHoldUntouched() {
        booking.setRequestExpiresAt(Instant.now().minusSeconds(1)); room.setAvailability(RoomAvailability.HELD); locked();
        when(bookingRepository.findExpiredPendingBookings(any())).thenReturn(List.of(booking));
        service.expirePendingBookings();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(room.getAvailability()).isEqualTo(RoomAvailability.HELD);
        verify(transitions).record(booking, BookingStatus.PENDING, BookingStatus.EXPIRED, null, ActorType.SYSTEM, "Expired");
        verifyNoInteractions(roomRepository, refundService);
    }
}
