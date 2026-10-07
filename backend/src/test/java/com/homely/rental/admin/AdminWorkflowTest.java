package com.homely.rental.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.admin.service.AdminService;
import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.auth.entity.*;
import com.homely.rental.auth.entity.Role;
import com.homely.rental.booking.entity.*;
import com.homely.rental.booking.repository.*;
import com.homely.rental.interaction.entity.*;
import com.homely.rental.interaction.repository.*;
import com.homely.rental.auth.repository.*;
import com.homely.rental.auth.security.*;
import com.homely.rental.catalog.entity.*;
import com.homely.rental.catalog.repository.*;
import com.homely.rental.common.audit.*;
import com.homely.rental.common.exception.ConflictException;
import com.homely.rental.common.exception.ResourceNotFoundException;
import com.homely.rental.notification.entity.*;
import com.homely.rental.notification.repository.NotificationRepository;
import com.homely.rental.notification.service.NotificationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "debug=false",
        "logging.level.org.hibernate.SQL=OFF"}, showSql = false)
@ContextConfiguration(classes = AdminWorkflowTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdminWorkflowTest {
    @org.springframework.boot.test.mock.mockito.MockBean com.homely.rental.booking.service.CaseSettlementService settlement;
    @org.springframework.boot.test.mock.mockito.MockBean com.homely.rental.catalog.service.ListingService listingService;
    @Configuration
    @EntityScan("com.homely.rental")
    @EnableJpaRepositories({"com.homely.rental.auth.repository", "com.homely.rental.catalog.repository",
            "com.homely.rental.booking.repository", "com.homely.rental.interaction.repository",
            "com.homely.rental.notification.repository", "com.homely.rental.common.audit"})
    @Import({AdminService.class, AuditService.class, NotificationService.class, AccountAccessService.class,
            com.homely.rental.auth.security.UserResolver.class, com.homely.rental.auth.service.AccountLifecycleGuard.class})
    static class Config { @Bean ObjectMapper mapper() { return new ObjectMapper(); } }

    @Autowired AdminService admin;
    @Autowired AccountAccessService accounts;
    @Autowired NotificationService inbox;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired RoomRepository rooms;
    @Autowired ListingRepository listings;
    @Autowired RefreshTokenRepository sessions;
    @Autowired AuditLogRepository audits;
    @Autowired NotificationRepository notifications;
    @Autowired PlatformTransactionManager manager;
    @Autowired BookingRepository bookings;
    @Autowired BookingCaseRepository cases;
    @Autowired ReportRepository reports;
    @Autowired ReviewRepository reviews;
    User moderator;
    User host;
    Long listingId;

    @BeforeEach void seed() {
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            Role role = roles.findByName(RoleName.ROLE_ADMIN).orElseGet(() -> {
                Role created = new Role(); created.setName(RoleName.ROLE_ADMIN); return roles.save(created);
            });
            moderator = user(); moderator.getRoles().add(role); moderator = users.save(moderator);
            host = users.save(user());
            Room room = new Room(); room.setHost(host); room.setUnitCode("ADMIN-TEST");
            room.setRoomType(RoomType.SINGLE_ROOM); room.setAreaM2(BigDecimal.valueOf(20)); room.setTermsVersion(3);
            rooms.save(room);
            Listing listing = new Listing(); listing.setRoom(room); listing.setTitle("Test listing");
            listing.setStatus(ListingStatus.PENDING_REVIEW); listingId = listings.save(listing).getId();
        });
        login(moderator);
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void approvalPublishesForThirtyDaysAndCommitsAuditAndOwnerNotification() {
        long before = audits.count();
        admin.approveListing(listingId);
        Listing approved = listings.findById(listingId).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(ListingStatus.PUBLISHED);
        assertThat(approved.getPublishedAt()).isNotNull();
        assertThat(approved.getExpiresAt()).isEqualTo(approved.getPublishedAt().plus(30, ChronoUnit.DAYS));
        assertThat(approved.getTermsVersion()).isEqualTo(3);
        assertThat(audits.count()).isEqualTo(before + 1);
        assertThat(notifications.countByUserIdAndIsReadFalse(host.getId())).isEqualTo(1);
        assertThatThrownBy(() -> admin.approveListing(listingId)).isInstanceOf(ConflictException.class);
        assertThat(audits.count()).isEqualTo(before + 1);
    }

    @Test void rejectionAndSuspensionEnforceLifecycleAndReason() {
        assertThatThrownBy(() -> admin.rejectListing(listingId, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> admin.suspendListing(listingId, "policy")).isInstanceOf(ConflictException.class);
        admin.approveListing(listingId);
        assertThatThrownBy(() -> admin.rejectListing(listingId, "policy")).isInstanceOf(ConflictException.class);
        admin.suspendListing(listingId, "Policy violation");
        assertThat(listings.findById(listingId).orElseThrow().getStatus()).isEqualTo(ListingStatus.SUSPENDED);
        assertThatThrownBy(() -> admin.approveListing(listingId)).isInstanceOf(ConflictException.class);
        assertThat(notifications.countByUserIdAndIsReadFalse(host.getId())).isEqualTo(2);
    }

    @Test void rollbackRemovesListingChangeAuditAndNotificationTogether() {
        long before = audits.count();
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            admin.approveListing(listingId);
            tx.setRollbackOnly();
        });
        assertThat(listings.findById(listingId).orElseThrow().getStatus()).isEqualTo(ListingStatus.PENDING_REVIEW);
        assertThat(audits.count()).isEqualTo(before);
        assertThat(notifications.countByUserIdAndIsReadFalse(host.getId())).isZero();
    }

    @Test void concurrentApprovalsCommitOneAuditAndNotification() throws Exception {
        long before = audits.count();
        var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> approve = () -> {
            login(moderator);
            try {
                start.await(); admin.approveListing(listingId); return true;
            } catch (ConflictException | org.springframework.orm.ObjectOptimisticLockingFailureException expected) {
                return false;
            } finally { SecurityContextHolder.clearContext(); }
        };
        try {
            var first = workers.submit(approve); var second = workers.submit(approve); start.countDown();
            assertThat(List.of(first.get(15, java.util.concurrent.TimeUnit.SECONDS), second.get(15, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally { workers.shutdownNow(); }
        assertThat(audits.count()).isEqualTo(before + 1);
        assertThat(notifications.countByUserIdAndIsReadFalse(host.getId())).isEqualTo(1);
    }

    @Test void caseResolutionRecordsDecisionAndNotifiesBothPartiesOnce() {
        Long caseId = new TransactionTemplate(manager).execute(tx -> {
            Listing listing = listings.findById(listingId).orElseThrow();
            Booking booking = new Booking(); booking.setTenant(moderator); booking.setHost(host);
            booking.setRoom(listing.getRoom()); booking.setListing(listing); booking.setRentVnd(BigDecimal.valueOf(1000000));
            booking.setTermsSnapshot("{}"); booking.setRequestExpiresAt(Instant.now().plusSeconds(600));
            bookings.save(booking);
            BookingCase dispute = new BookingCase(); dispute.setBooking(booking); dispute.setDescription("Test dispute");
            dispute.setType(BookingCaseType.HANDOVER_OVERDUE); return cases.save(dispute).getId();
        });
        admin.resolveBookingCase(caseId, BookingCaseDecision.NO_ACTION, "Evidence reviewed");
        BookingCase resolved = cases.findById(caseId).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(CaseStatus.RESOLVED);
        assertThat(resolved.getDecision()).isEqualTo(BookingCaseDecision.NO_ACTION);
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(notifications.countByUserIdAndIsReadFalse(host.getId())).isEqualTo(1);
        assertThat(notifications.countByUserIdAndIsReadFalse(moderator.getId())).isEqualTo(1);
        assertThatThrownBy(() -> admin.resolveBookingCase(caseId, BookingCaseDecision.REFUND_TENANT, "Change"))
                .isInstanceOf(ConflictException.class);
    }

    @Test void resolvingReportsAndHidingReviewsRequireReasonAndCannotRepeat() {
        Long reportId = new TransactionTemplate(manager).execute(tx -> {
            Report report = new Report(); report.setReporter(host); report.setTargetType("listing");
            report.setTargetId(listingId); report.setReasonCode("SPAM"); return reports.save(report).getId();
        });
        assertThatThrownBy(() -> admin.resolveReport(reportId, "x".repeat(1001))).isInstanceOf(IllegalArgumentException.class);
        admin.resolveReport(reportId, "Report investigated");
        assertThat(reports.findById(reportId).orElseThrow().getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThatThrownBy(() -> admin.resolveReport(reportId, "Again")).isInstanceOf(ConflictException.class);
        Long reviewId = new TransactionTemplate(manager).execute(tx -> {
            Review review = new Review(); review.setReviewer(host); review.setBookingId(listingId);
            review.setRoom(listings.findById(listingId).orElseThrow().getRoom()); review.setRating(3);
            return reviews.save(review).getId();
        });
        admin.hideReview(reviewId, "Inappropriate content");
        assertThat(reviews.findById(reviewId).orElseThrow().isVisible()).isFalse();
        assertThatThrownBy(() -> admin.hideReview(reviewId, "Again")).isInstanceOf(ConflictException.class);
    }

    @Test void suspensionRevokesSessionsBlocksAccessAndRestorationDoesNotReviveSessions() {
        RefreshToken token = new RefreshToken(); token.setUser(host); token.setTokenHash(UUID.randomUUID().toString());
        token.setInstallationId(UUID.randomUUID().toString()); token.setDeviceName("test");
        token.setExpiresAt(Instant.now().plusSeconds(3600)); token = sessions.save(token);
        admin.suspendUser(host.getId(), "Abuse report confirmed");
        assertThat(sessions.findById(token.getId()).orElseThrow().isRevoked()).isTrue();
        assertThat(users.findById(host.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThatThrownBy(() -> accounts.requireActive(host.getEmail())).isInstanceOf(AccountAccessException.class);
        admin.unsuspendUser(host.getId());
        assertThat(accounts.requireActive(host.getEmail()).isSuspended()).isFalse();
        assertThat(sessions.findById(token.getId()).orElseThrow().isRevoked()).isTrue();
    }

    @Test void cannotSuspendSelfOrRestoreDeletedUserAndInactiveAdminCannotMutate() {
        assertThatThrownBy(() -> admin.suspendUser(moderator.getId(), "self")).isInstanceOf(ConflictException.class);
        new TransactionTemplate(manager).executeWithoutResult(tx -> users.findById(host.getId()).orElseThrow().setStatus(UserStatus.DELETED));
        assertThatThrownBy(() -> admin.unsuspendUser(host.getId())).isInstanceOf(ConflictException.class);
        new TransactionTemplate(manager).executeWithoutResult(tx -> users.findById(moderator.getId()).orElseThrow().setSuspended(true));
        assertThatThrownBy(() -> admin.approveListing(listingId)).isInstanceOf(AccountAccessException.class);
    }

    @Test void databaseRoleIsRequiredEvenWhenCallerHasAnAdminAuthority() {
        login(host);
        assertThatThrownBy(() -> admin.approveListing(listingId)).isInstanceOfSatisfying(AccountAccessException.class,
                error -> assertThat(error.getCode()).isEqualTo("FORBIDDEN"));
    }

    @Test void inboxIsPrivateOrderedAndMarkAllReadOnlyUpdatesCurrentUser() throws Exception {
        admin.approveListing(listingId);
        Notification own = inbox.createNotification(host, NotificationType.SYSTEM_ANNOUNCEMENT, "Second", "body", "user", host.getId());
        Notification other = inbox.createNotification(moderator, NotificationType.SYSTEM_ANNOUNCEMENT, "Private", "body", "user", moderator.getId());
        login(host);
        var page = inbox.getMyNotifications(PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getData()).extracting(n -> n.getId()).containsExactly(own.getId());
        assertThatThrownBy(() -> inbox.markAsRead(other.getId())).isInstanceOf(ResourceNotFoundException.class);
        inbox.markAsRead(own.getId()); inbox.markAsRead(own.getId());
        assertThat(inbox.getUnreadCount()).isEqualTo(1);
        assertThat(inbox.markAllAsRead()).isEqualTo(1);
        assertThat(inbox.markAllAsRead()).isZero();
        assertThat(notifications.findById(other.getId()).orElseThrow().isRead()).isFalse();
        assertThatThrownBy(() -> inbox.getMyNotifications(PageRequest.of(0, 51))).isInstanceOf(IllegalArgumentException.class);
    }

    private User user() {
        User user = new User(); user.setEmail(UUID.randomUUID() + "@example.test");
        user.setPassword("unused-test-hash"); user.setFullName("Test User"); user.setEmailVerified(true); return user;
    }

    private void login(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getEmail(), "unused", List.of(() -> "ROLE_ADMIN")));
    }
}
