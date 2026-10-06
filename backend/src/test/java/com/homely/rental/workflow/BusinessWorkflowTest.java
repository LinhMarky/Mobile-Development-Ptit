package com.homely.rental.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.entity.Role;
import com.homely.rental.auth.entity.RoleName;
import com.homely.rental.auth.repository.*;
import com.homely.rental.auth.security.AccountAccessService;
import com.homely.rental.auth.security.UserResolver;
import com.homely.rental.booking.entity.*;
import com.homely.rental.booking.dto.*;
import com.homely.rental.booking.repository.*;
import com.homely.rental.booking.service.*;
import com.homely.rental.catalog.entity.*;
import com.homely.rental.catalog.repository.*;
import com.homely.rental.catalog.service.*;
import com.homely.rental.catalog.dto.request.*;
import com.homely.rental.common.exception.*;
import com.homely.rental.media.entity.*;
import com.homely.rental.media.repository.MediaRepository;
import com.homely.rental.media.service.*;
import com.homely.rental.media.validation.FileValidator;
import com.homely.rental.notification.service.NotificationService;
import com.homely.rental.notification.push.*;
import com.homely.rental.payment.dto.*;
import com.homely.rental.payment.entity.*;
import com.homely.rental.payment.repository.*;
import com.homely.rental.payment.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties={"spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "homely.fcm.enabled=true"},showSql=false)
@ContextConfiguration(classes=BusinessWorkflowTest.Config.class)
class BusinessWorkflowTest {
    @Configuration @EntityScan("com.homely.rental") @EnableJpaRepositories("com.homely.rental")
    @Import({BookingService.class,BookingLocks.class,BookingTransitions.class,PaymentService.class,RefundService.class,
            CaseSettlementService.class,NotificationService.class,AccountAccessService.class,UserResolver.class,
            RoomService.class,ListingService.class,MediaService.class,PushOutbox.class,PushWorker.class,
            com.homely.rental.interaction.service.ViewingService.class, com.homely.rental.auth.service.AccountDeletionService.class,
            com.homely.rental.auth.service.AccountDeletionEligibility.class, com.homely.rental.auth.service.AccountDeletionProcessor.class,
            com.homely.rental.auth.service.AccountLifecycleGuard.class})
    static class Config { @Bean ObjectMapper mapper(){return new ObjectMapper();} }
    @MockBean StorageService storage;
    @MockBean FileValidator validator;
    @MockBean PushGateway gateway;
    @MockBean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired PushWorker worker;
    @Autowired PushDeliveryRepository deliveries;
    @Autowired DeviceTokenRepository devices;
    @Autowired NotificationPreferenceRepository preferences;
    @Autowired NotificationService notifications;
    @Autowired com.homely.rental.notification.repository.NotificationRepository inbox;
    @Autowired com.homely.rental.interaction.service.ViewingService viewings;
    @Autowired com.homely.rental.auth.service.AccountDeletionService deletion;
    @Autowired com.homely.rental.auth.service.AccountDeletionProcessor deletionProcessor;
    @Autowired AccountDeletionRequestRepository deletionRequests;
    @Autowired RefreshTokenRepository sessions;
    @Autowired WishlistRepository wishlist;
    @Autowired BookingService service;
    @Autowired PaymentService paymentService;
    @Autowired MediaService mediaService;
    @Autowired ListingService listingService;
    @Autowired CaseSettlementService settlements;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired RoomRepository rooms;
    @Autowired ListingRepository listings;
    @Autowired BookingRepository bookings;
    @Autowired PaymentRepository payments;
    @Autowired RefundRepository refunds;
    @Autowired WebhookReceiptRepository receipts;
    @Autowired BookingCaseRepository cases;
    @Autowired MediaRepository media;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    User host,tenant,other;
    Long roomId,listingId;

    @BeforeEach void seed() {
        org.mockito.Mockito.when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString())).thenReturn("deleted-password-hash");
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            Role role=roles.findByName(RoleName.ROLE_HOST).orElseGet(()->{Role r=new Role();r.setName(RoleName.ROLE_HOST);return roles.save(r);});
            host=user();host.getRoles().add(role);users.save(host); tenant=user();other=user();
            Room room=new Room();room.setHost(host);room.setUnitCode(UUID.randomUUID().toString().substring(0,20));
            room.setRoomType(RoomType.SINGLE_ROOM);room.setAreaM2(new BigDecimal("25"));room.setMaxOccupants(2);
            Address address=new Address();address.setLine("Test campus");address.setProvinceCode("HN");room.setAddress(address);
            room.setLocation(new GeoPoint(21.0285,105.8542));rooms.save(room);roomId=room.getId();
            Listing l=new Listing();l.setRoom(room);l.setTitle("Test room");l.setDescription("Near university");
            l.setRentVnd(new BigDecimal("3000000"));l.setDepositVnd(new BigDecimal("1000000"));l.setStatus(ListingStatus.PUBLISHED);
            l.setPublishedAt(Instant.now());l.setExpiresAt(Instant.now().plusSeconds(86400));listingId=listings.save(l).getId();
            em.flush();
        });
        login(tenant);
    }
    @AfterEach void logout(){SecurityContextHolder.clearContext();}
    User user(){User u=new User();u.setEmail(UUID.randomUUID()+"@test.local");u.setPassword("hash");u.setFullName("Test");u.setEmailVerified(true);return users.save(u);}
    void login(User u){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u.getEmail(),"n/a",List.of()));}
    BookingDTO create() throws Exception {BookingCreateRequest r=new BookingCreateRequest();r.setRoomId(roomId);return service.createBooking(r);}
    Long approved() throws Exception {login(tenant);Long id=create().getId();login(host);service.approveBooking(id);login(tenant);return id;}
    PaymentDTO payment(Long id){PaymentCreateRequest r=new PaymentCreateRequest();r.setBookingId(id);return paymentService.createPayment(r);}
    WebhookPayload event(PaymentDTO p){WebhookPayload w=new WebhookPayload();w.setProviderPaymentId(p.getProviderPaymentId());w.setEventId(UUID.randomUUID().toString());w.setStatus("SUCCEEDED");return w;}

    @Test void depositCannotBeConfirmedWithoutPayment() throws Exception {
        Long id=approved();
        assertThatThrownBy(()->service.confirmDeposit(id)).isInstanceOf(ConflictException.class).hasMessageContaining("deposit");
        assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.APPROVED);
    }
    @Test void successfulWebhookAllocatesOnceAndRejectsEventMutation() throws Exception {
        Long id=approved();var p=payment(id);var w=event(p);
        paymentService.processWebhook(w);paymentService.processWebhook(w);
        assertThat(service.confirmDeposit(id).getStatus()).isEqualTo("CONFIRMED");
        assertThat(receipts.findByProviderAndEventId("MOCK_SANDBOX",w.getEventId())).isPresent();
        w.setStatus("FAILED");
        assertThatThrownBy(()->paymentService.processWebhook(w)).isInstanceOf(ConflictException.class);
    }
    @Test void latePaymentRefundsWithoutRevivingExpiredBooking() throws Exception {
        Long id=approved();var p=payment(id);
        var b=bookings.findById(id).orElseThrow();b.setHoldExpiresAt(Instant.now().minusSeconds(5));em.flush();
        service.expireApprovedBookings();
        paymentService.processWebhook(event(p));
        assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(refunds.findByPaymentId(p.getId()).orElseThrow().getAmountVnd()).isEqualByComparingTo("1000000");
        assertThat(rooms.findById(roomId).orElseThrow().getAvailability()).isEqualTo(RoomAvailability.AVAILABLE);
    }
    @Test void expiredPaymentAttemptCanRetryAndOldSuccessIsRefunded() throws Exception {
        Long id=approved();var old=payment(id);
        payments.findById(old.getId()).orElseThrow().setExpiresAt(Instant.now().minusSeconds(5));em.flush();
        var next=payment(id);assertThat(next.getId()).isNotEqualTo(old.getId());
        paymentService.processWebhook(event(old));
        assertThat(refunds.findByPaymentId(old.getId())).isPresent();
        paymentService.processWebhook(event(next));
        assertThat(bookings.findById(id).orElseThrow().getAllocatedPaymentId()).isEqualTo(next.getId());
    }

    long noticeCount(Long bookingId, com.homely.rental.notification.entity.NotificationType type, Long userId) {
        return inbox.findAll().stream().filter(n -> "booking".equals(n.getRefType()) && bookingId.equals(n.getRefId())
                && type == n.getType() && userId.equals(n.getUser().getId())).count();
    }

    @Test void bookingAndPaymentNotificationsAreCreatedOncePerOutcome() throws Exception {
        Long id = approved();
        for (User member : List.of(tenant, host)) {
            assertThat(noticeCount(id, com.homely.rental.notification.entity.NotificationType.BOOKING_CREATED, member.getId())).isEqualTo(1);
            assertThat(noticeCount(id, com.homely.rental.notification.entity.NotificationType.BOOKING_APPROVED, member.getId())).isEqualTo(1);
        }
        var p = payment(id); var webhook = event(p);
        paymentService.processWebhook(webhook); paymentService.processWebhook(webhook);
        paymentService.processWebhook(event(p)); service.confirmDeposit(id);
        for (User member : List.of(tenant, host))
            assertThat(noticeCount(id, com.homely.rental.notification.entity.NotificationType.BOOKING_CONFIRMED, member.getId())).isEqualTo(1);
        assertThat(noticeCount(id, com.homely.rental.notification.entity.NotificationType.PAYMENT_SUCCEEDED, tenant.getId())).isEqualTo(1);
    }

    @Test void failedWebhookNotifiesPayerOnceAndLeavesHoldForRetry() throws Exception {
        Long id = approved(); var p = payment(id); var webhook = event(p); webhook.setStatus("FAILED");
        paymentService.processWebhook(webhook); paymentService.processWebhook(webhook);
        webhook.setEventId(UUID.randomUUID().toString()); paymentService.processWebhook(webhook);
        assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(rooms.findById(roomId).orElseThrow().getAvailability()).isEqualTo(RoomAvailability.HELD);
        assertThat(noticeCount(id, com.homely.rental.notification.entity.NotificationType.PAYMENT_FAILED, tenant.getId())).isEqualTo(1);
        assertThat(payment(id).getId()).isNotEqualTo(p.getId());
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void expirationScanExpiresDueAttemptOnceKeepsHoldAndAllowsRetry() throws Exception {
        Long id = approved(); var p = payment(id);
        new TransactionTemplate(manager).executeWithoutResult(tx -> payments.findById(p.getId()).orElseThrow().setExpiresAt(Instant.now().minusSeconds(60)));
        var job = new com.homely.rental.payment.job.PaymentExpirationJob(payments, paymentService);
        job.expirePendingPayments(); job.expirePendingPayments();
        assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(rooms.findById(roomId).orElseThrow().getAvailability()).isEqualTo(RoomAvailability.HELD);
        assertThat(noticeCount(id, com.homely.rental.notification.entity.NotificationType.PAYMENT_EXPIRED, tenant.getId())).isEqualTo(1);
        var next = payment(id); assertThat(next.getId()).isNotEqualTo(p.getId());
        job.expirePendingPayments();
        assertThat(payments.findById(next.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void staleExpirationCandidateDoesNotOverwriteSuccessfulPayment() throws Exception {
        Long id = approved(); var p = payment(id); paymentService.processWebhook(event(p));
        assertThat(paymentService.expirePayment(p.getId(), Instant.now().plusSeconds(3600))).isFalse();
        assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(noticeCount(id, com.homely.rental.notification.entity.NotificationType.PAYMENT_EXPIRED, tenant.getId())).isZero();
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void expirationRechecksDeadlineAndIgnoresEarlyOrMissingCandidate() throws Exception {
        Long id = approved(); var p = payment(id);
        assertThat(paymentService.expirePayment(p.getId(), Instant.now())).isFalse();
        assertThat(paymentService.expirePayment(Long.MAX_VALUE, Instant.now())).isFalse();
        assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(noticeCount(id, com.homely.rental.notification.entity.NotificationType.PAYMENT_EXPIRED, tenant.getId())).isZero();
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void successfulWebhookAfterJobExpirationRefundsOnceWithoutConfirmingBooking() throws Exception {
        Long id = approved(); var p = payment(id);
        new TransactionTemplate(manager).executeWithoutResult(tx -> payments.findById(p.getId()).orElseThrow().setExpiresAt(Instant.now().minusSeconds(60)));
        assertThat(paymentService.expirePayment(p.getId(), Instant.now())).isTrue();
        var webhook = event(p); paymentService.processWebhook(webhook); paymentService.processWebhook(webhook); paymentService.processWebhook(event(p));
        assertThat(refunds.findByPaymentId(p.getId()).orElseThrow().getAmountVnd()).isEqualByComparingTo(p.getAmountVnd());
        assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(noticeCount(id, com.homely.rental.notification.entity.NotificationType.PAYMENT_REFUNDED, tenant.getId())).isEqualTo(1);
        assertThat(noticeCount(id, com.homely.rental.notification.entity.NotificationType.BOOKING_CONFIRMED, host.getId())).isZero();
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void rollingBackPaymentOutcomeAlsoRollsBackInboxAndReceipt() throws Exception {
        Long id = approved(); var p = payment(id); var webhook = event(p);
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            paymentService.processWebhook(webhook); em.flush(); tx.setRollbackOnly();
        });
        assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(receipts.findByProviderAndEventId("MOCK_SANDBOX", webhook.getEventId())).isEmpty();
        assertThat(noticeCount(id, com.homely.rental.notification.entity.NotificationType.PAYMENT_SUCCEEDED, tenant.getId())).isZero();
        assertThat(noticeCount(id, com.homely.rental.notification.entity.NotificationType.BOOKING_CONFIRMED, host.getId())).isZero();
    }
    @Test void expiredRequestCannotBeApprovedBeforeJobRuns() throws Exception {
        Long id=create().getId();bookings.findById(id).orElseThrow().setRequestExpiresAt(Instant.now().minusSeconds(1));em.flush();login(host);
        assertThatThrownBy(()->service.approveBooking(id)).isInstanceOf(ConflictException.class).hasMessageContaining("expired");
    }
    @Test void feesCannotChangeAfterPublishingAndAreIncludedInSnapshot() throws Exception {
        login(host);
        FeeInput f=new FeeInput();f.setFeeCode("WATER");f.setAmountVnd(new BigDecimal("100000"));
        assertThatThrownBy(()->listingService.setFees(listingId,List.of(f))).isInstanceOf(ConflictException.class);
        listings.findById(listingId).orElseThrow().setStatus(ListingStatus.HIDDEN);em.flush();
        listingService.setFees(listingId,List.of(f));
        listings.findById(listingId).orElseThrow().setStatus(ListingStatus.PUBLISHED);em.flush();login(tenant);
        Long id=create().getId();
        assertThat(bookings.findById(id).orElseThrow().getTermsSnapshot()).contains("WATER","100000");
    }
    @Test void cannotAttachOwnMediaToSomeoneElsesRoom() {
        login(other);
        Media m=new Media();m.setUploader(other);m.setPurpose(MediaPurpose.ROOM_PHOTO);m.setContentType("image/jpeg");
        m.setFileSizeBytes(100);m.setStorageKey(UUID.randomUUID().toString());media.saveAndFlush(m);
        assertThatThrownBy(()->mediaService.attachToResource(List.of(m.getId()),"room",roomId,MediaPurpose.ROOM_PHOTO))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(m.getStatus()).isEqualTo(MediaStatus.READY);
    }
    @Test void draftRoomMediaIsPrivateButOwnerCanRead() {
        listings.findById(listingId).orElseThrow().setStatus(ListingStatus.DRAFT);em.flush();login(other);
        assertThatThrownBy(()->mediaService.getResourceMedia("room",roomId)).isInstanceOf(ResourceNotFoundException.class);
        login(host);assertThat(mediaService.getResourceMedia("room",roomId)).isEmpty();
    }
    @Test void searchAppliesBudgetProvinceAreaAndGeographicDistance() {
        ListingSearchQuery q=new ListingSearchQuery();q.setProvince_code("HN");q.setMax_rent_vnd(new BigDecimal("2999999"));
        assertThat(listingService.searchListings(q).getData()).isEmpty();
        q.setMax_rent_vnd(new BigDecimal("3000000"));q.setMin_area_m2(new BigDecimal("20"));
        q.setLatitude(21.0285);q.setLongitude(105.8542);q.setRadius_km(1.0);q.setSort("nearest");
        assertThat(listingService.searchListings(q).getData()).extracting("id").contains(listingId);
        q.setProvince_code("HCM");assertThat(listingService.searchListings(q).getData()).isEmpty();
    }
    @Test void overdueHandoverOpensOnlyOneCase() throws Exception {
        Long id=approved();paymentService.processWebhook(event(payment(id)));
        bookings.findById(id).orElseThrow().setHandoverDueAt(Instant.now().minusSeconds(5));em.flush();
        service.handleOverdueHandovers();service.handleOverdueHandovers();
        assertThat(cases.findByBookingIdOrderByCreatedAtDesc(id)).hasSize(1);
    }
    @Test void caseRefundCancelsBookingAndReleasesRoom() throws Exception {
        Long id=approved();var p=payment(id);paymentService.processWebhook(event(p));
        var dto=service.openCase(id,new CaseCreateRequest(BookingCaseType.DEPOSIT_DISPUTE,"Room unavailable"));
        var c=cases.findById(dto.id()).orElseThrow();
        settlements.settle(c,BookingCaseDecision.REFUND_TENANT,null);
        assertThat(c.getTenantRefundVnd()).isEqualByComparingTo("1000000");
        assertThat(refunds.findByPaymentId(p.getId())).isPresent();
        assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void concurrentApprovalsProduceOnlyOneHold() throws Exception {
        login(tenant);Long first=create().getId();login(other);Long second=create().getId();
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            List<Future<Boolean>> results=new ArrayList<>();
            for(Long id:List.of(first,second)) results.add(pool.submit(()->{
                login(host);start.await();
                try { service.approveBooking(id);return true; }
                catch(ConflictException e){return false;}
                finally {SecurityContextHolder.clearContext();}
            }));
            start.countDown();
            int successes=0;for(var result:results) if(result.get(20,TimeUnit.SECONDS)) successes++;
            assertThat(successes).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }

    @Test void deletionIsBlockedByOpenBooking() throws Exception {
        create();
        org.mockito.Mockito.when(passwordEncoder.matches("password",tenant.getPassword())).thenReturn(true);
        var request=new com.homely.rental.auth.dto.request.DeletionRequest();request.setCurrentPassword("password");
        assertThatThrownBy(()->deletion.requestDeletion(request)).isInstanceOf(ConflictException.class).hasMessageContaining("active bookings");
    }

    @Test void viewingReservesSlotAndCreatesTransactionalNotifications() throws Exception {
        login(host);
        var slot=new com.homely.rental.interaction.dto.ViewingSlotCreateRequest();slot.setRoomId(roomId);
        slot.setStartAt(Instant.now().plusSeconds(172800));slot.setEndAt(Instant.now().plusSeconds(176400));
        var saved=viewings.createSlot(slot);login(tenant);
        var request=new com.homely.rental.interaction.dto.ViewingCreateRequest();request.setViewingSlotId(saved.getId());
        var v=viewings.createViewing(request);login(other);
        assertThatThrownBy(()->viewings.createViewing(request)).isInstanceOf(ConflictException.class);
        login(host);assertThat(viewings.confirmViewing(v.getId()).getStatus()).isEqualTo("CONFIRMED");
        assertThatThrownBy(()->viewings.completeViewing(v.getId())).isInstanceOf(ConflictException.class);
        login(tenant);viewings.cancelViewing(v.getId(),"Reschedule");
        assertThat(viewings.getSlots(roomId)).extracting("id").contains(saved.getId());
    }

    Long queuePush() {
        return new TransactionTemplate(manager).execute(tx -> {
            var device=new com.homely.rental.auth.entity.DeviceToken();device.setUser(users.findById(tenant.getId()).orElseThrow());
            device.setToken(UUID.randomUUID().toString());device.setInstallationId(UUID.randomUUID().toString());device.setDeviceName("Test Android");
            devices.saveAndFlush(device);
            var n=notifications.createNotification(device.getUser(),com.homely.rental.notification.entity.NotificationType.NEW_MESSAGE,
                    "Tin nhắn mới","Mở ứng dụng để xem","conversation",1L);
            em.flush();
            return deliveries.findAll().stream().filter(d->d.getNotificationId().equals(n.getId())).findFirst().orElseThrow().getId();
        });
    }

    com.homely.rental.auth.entity.AccountDeletionRequest requestDeletion(User user) throws Exception {
        login(user);
        org.mockito.Mockito.when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        var request = new com.homely.rental.auth.dto.request.DeletionRequest();
        request.setCurrentPassword("password");
        return deletion.requestDeletion(request);
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void deletionBlocksFurtherWritesButItsStatusRemainsReadable() throws Exception {
        var request = requestDeletion(tenant);
        assertThat(deletion.currentRequest().status()).isEqualTo("PENDING");
        assertThatThrownBy(this::create).isInstanceOf(com.homely.rental.auth.security.AccountAccessException.class)
                .hasMessageContaining("deletion is pending");
        assertThatThrownBy(() -> requestDeletion(tenant)).isInstanceOf(ConflictException.class);
        deletionProcessor.process(request.getId());
        assertThat(users.findById(tenant.getId()).orElseThrow().getStatus())
                .isEqualTo(com.homely.rental.auth.constant.UserStatus.DELETED);
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void hostDeletionRechecksNewBookingsAndReusesFailedRequestOnRetry() throws Exception {
        var request = requestDeletion(host);
        login(tenant);
        var booking = create();
        deletionProcessor.process(request.getId());
        assertThat(deletionRequests.findById(request.getId()).orElseThrow().getFailureCode()).isEqualTo("DELETION_BLOCKED");
        assertThat(users.findById(host.getId()).orElseThrow().getEmail()).isEqualTo(host.getEmail());
        var cancel = new BookingActionRequest(); cancel.setReason("Cancel before account deletion");
        service.cancelBooking(booking.getId(), cancel);
        var retry = requestDeletion(host);
        assertThat(retry.getId()).isEqualTo(request.getId());
        deletionProcessor.process(retry.getId());
        assertThat(listings.findById(listingId).orElseThrow().getStatus()).isEqualTo(ListingStatus.ARCHIVED);
        assertThat(deletionRequests.findById(retry.getId()).orElseThrow().getStatus())
                .isEqualTo(com.homely.rental.auth.entity.AccountDeletionRequest.DeletionStatus.COMPLETED);
        assertThat(bookings.findById(booking.getId())).isPresent();
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void deletionRevokesSessionsDisablesDevicesAndRemovesPersonalProfile() throws Exception {
        Long deliveryId = queuePush();
        Long sessionId = new TransactionTemplate(manager).execute(tx -> {
            var user = users.findById(tenant.getId()).orElseThrow();
            user.setPhone("0901234567"); user.setAvatarUrl("https://example.test/avatar.png");
            var session = new com.homely.rental.auth.entity.RefreshToken();
            session.setUser(user); session.setTokenHash(UUID.randomUUID().toString());
            session.setInstallationId(UUID.randomUUID().toString()); session.setDeviceName("Test");
            session.setExpiresAt(Instant.now().plusSeconds(86400));
            var item = new WishlistItem(); item.setUser(user); item.setRoom(rooms.findById(roomId).orElseThrow()); wishlist.save(item);
            return sessions.save(session).getId();
        });
        var request = requestDeletion(tenant);
        org.mockito.Mockito.when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString())).thenReturn("new-disabled-hash");
        deletionProcessor.process(request.getId());
        deletionProcessor.process(request.getId()); // A second processing attempt is harmless.
        var deleted = users.findById(tenant.getId()).orElseThrow();
        assertThat(deleted.getEmail()).isEqualTo("deleted-" + tenant.getId() + "@deleted.invalid");
        assertThat(deleted.getPhone()).isNull(); assertThat(deleted.getAvatarUrl()).isNull();
        assertThat(deleted.getPassword()).isEqualTo("new-disabled-hash");
        assertThat(sessions.findById(sessionId).orElseThrow().isRevoked()).isTrue();
        assertThat(devices.findByUserIdAndActiveTrue(tenant.getId())).isEmpty();
        assertThat(wishlist.existsByUserIdAndRoomId(tenant.getId(), roomId)).isFalse();
        worker.tick();
        assertThat(deliveries.findById(deliveryId).orElseThrow().getStatus()).isEqualTo("SKIPPED");
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void failedAnonymizationRollsBackAllChanges() throws Exception {
        Long deliveryId = queuePush();
        var request = requestDeletion(tenant);
        org.mockito.Mockito.when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new IllegalStateException("Test failure"));
        assertThatThrownBy(() -> deletionProcessor.process(request.getId())).isInstanceOf(IllegalStateException.class);
        assertThat(users.findById(tenant.getId()).orElseThrow().getEmail()).isEqualTo(tenant.getEmail());
        var delivery = deliveries.findById(deliveryId).orElseThrow();
        assertThat(devices.findById(delivery.getDeviceId()).orElseThrow().isActive()).isTrue();
        deletionProcessor.markFailed(request.getId());
        assertThat(deletionRequests.findById(request.getId()).orElseThrow().getFailureCode()).isEqualTo("PROCESSING_ERROR");
    }

    @Test void unresolvedReportBlocksDeletion() throws Exception {
        var report = new com.homely.rental.interaction.entity.Report();
        report.setReporter(tenant); report.setTargetType("listing"); report.setTargetId(listingId); report.setReasonCode("OTHER");
        em.persist(report); em.flush();
        assertThatThrownBy(() -> requestDeletion(tenant)).isInstanceOf(ConflictException.class).hasMessageContaining("reports");
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void tenantDeletionPreservesCompletedBookingAndPaymentHistory() throws Exception {
        Long id = approved();
        var payment = payment(id);
        paymentService.processWebhook(event(payment));
        service.tenantHandover(id); login(host); service.hostHandover(id);
        var request = requestDeletion(tenant);
        deletionProcessor.process(request.getId());
        assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(payments.findById(payment.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void failedRefundStillBlocksDeletionAfterBookingCompletion() throws Exception {
        Long id = approved();
        var payment = payment(id);
        paymentService.processWebhook(event(payment));
        service.tenantHandover(id); login(host); service.hostHandover(id);
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            var refund = new Refund(); refund.setPayment(payments.findById(payment.getId()).orElseThrow());
            refund.setAmountVnd(new BigDecimal("1000000")); refund.setReason(RefundReason.CASE_DECISION);
            refund.setStatus(RefundStatus.FAILED); refunds.saveAndFlush(refund);
        });
        assertThatThrownBy(() -> requestDeletion(tenant)).isInstanceOf(ConflictException.class);
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void pushSendsOutsideTransactionAndRetriesTransientFailure() throws Exception {
        Long id=queuePush();
        org.mockito.Mockito.doAnswer(call -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            throw new PushGateway.PushFailure("UNAVAILABLE",true,false);
        }).when(gateway).send(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyMap());
        worker.tick();
        var pending=deliveries.findById(id).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo("PENDING");assertThat(pending.getAttempts()).isEqualTo(1);
        assertThat(pending.getNextAttemptAt()).isAfter(Instant.now());
        new TransactionTemplate(manager).executeWithoutResult(tx->deliveries.findById(id).orElseThrow().setNextAttemptAt(Instant.now().minusSeconds(1)));
        org.mockito.Mockito.doNothing().when(gateway).send(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyMap());
        worker.tick();assertThat(deliveries.findById(id).orElseThrow().getStatus()).isEqualTo("SENT");
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void invalidPushTokenIsDisabledAndPreferencePreventsDelivery() throws Exception {
        Long id=queuePush();
        org.mockito.Mockito.doThrow(new PushGateway.PushFailure("UNREGISTERED",false,true)).when(gateway)
                .send(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyMap());
        worker.tick();var failed=deliveries.findById(id).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo("FAILED");assertThat(devices.findById(failed.getDeviceId()).orElseThrow().isActive()).isFalse();
        Long skipped=queuePush();
        new TransactionTemplate(manager).executeWithoutResult(tx->{
            var pref=new com.homely.rental.auth.entity.NotificationPreference();pref.setUser(users.findById(tenant.getId()).orElseThrow());pref.setChatPush(false);preferences.save(pref);
        });
        worker.tick();assertThat(deliveries.findById(skipped).orElseThrow().getStatus()).isEqualTo("SKIPPED");
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void rollbackDoesNotLeaveInboxOrPushDelivery() {
        Long first=queuePush();long before=deliveries.count();
        new TransactionTemplate(manager).executeWithoutResult(tx->{
            notifications.createNotification(users.findById(tenant.getId()).orElseThrow(),com.homely.rental.notification.entity.NotificationType.NEW_MESSAGE,
                    "Rollback","Rollback","conversation",2L);em.flush();tx.setRollbackOnly();
        });
        assertThat(deliveries.count()).isEqualTo(before);
        new TransactionTemplate(manager).executeWithoutResult(tx->deliveries.findById(first).orElseThrow().setStatus("SKIPPED"));
    }
}
