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
            com.homely.rental.interaction.service.ViewingService.class, com.homely.rental.auth.service.AccountDeletionService.class})
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
    @Autowired com.homely.rental.interaction.service.ViewingService viewings;
    @Autowired com.homely.rental.auth.service.AccountDeletionService deletion;
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
