package com.homely.rental.chat.service;

import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.repository.UserRepository;
import com.homely.rental.catalog.entity.*;
import com.homely.rental.catalog.repository.*;
import com.homely.rental.chat.dto.*;
import com.homely.rental.chat.entity.Conversation;
import com.homely.rental.chat.entity.MessageContentType;
import com.homely.rental.chat.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.jpa.show-sql=false"}, showSql = false)
@ContextConfiguration(classes = ChatServiceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ChatServiceTest {
    @Configuration
    @EntityScan(basePackageClasses = {User.class, Room.class, Conversation.class})
    @EnableJpaRepositories(basePackageClasses = {UserRepository.class, RoomRepository.class, ConversationRepository.class})
    @Import({ChatService.class, CommittedEvents.class})
    static class Config { }

    static class CommittedEvents {
        final List<ChatMessageCreatedEvent> events = new CopyOnWriteArrayList<>();
        @TransactionalEventListener public void record(ChatMessageCreatedEvent event) { events.add(event); }
    }

    @Autowired ChatService service;
    @Autowired UserRepository users;
    @Autowired RoomRepository rooms;
    @Autowired ListingRepository listings;
    @Autowired ConversationRepository conversations;
    @Autowired MessageRepository messages;
    @Autowired ReadMarkerRepository markers;
    @Autowired PlatformTransactionManager manager;
    @Autowired CommittedEvents committed;
    private Long roomId;
    private Long tenantId;

    @BeforeEach void seed() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            markers.deleteAll(); messages.deleteAll(); conversations.deleteAll();
            listings.deleteAll(); rooms.deleteAll(); users.deleteAll();
            users.flush();
            User host = users.save(user("host@example.test"));
            User tenant = users.save(user("tenant@example.test"));
            users.save(user("other@example.test"));
            tenantId = tenant.getId();
            Room room = new Room(); room.setHost(host); room.setUnitCode("CHAT");
            room.setRoomType(RoomType.SINGLE_ROOM); room.setAreaM2(BigDecimal.valueOf(20));
            roomId = rooms.save(room).getId();
            Listing listing = new Listing(); listing.setRoom(room); listing.setTitle("Original room title");
            listing.setStatus(ListingStatus.PUBLISHED); listing.setExpiresAt(Instant.now().plusSeconds(3600));
            listings.save(listing);
        });
        committed.events.clear();
    }

    @Test void concurrentOpenCreatesOnlyOneConversationAndPreservesTitle() throws Exception {
        var results = race(() -> service.createConversation(roomId, "tenant@example.test"));
        assertThat(results.get(0).getId()).isEqualTo(results.get(1).getId());
        assertThat(conversations.count()).isEqualTo(1);
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            Listing listing = listings.findByRoomId(roomId).orElseThrow();
            listing.setStatus(ListingStatus.ARCHIVED); listing.setTitle("Changed title");
        });
        assertThat(service.createConversation(roomId, "tenant@example.test").getRoomTitle()).isEqualTo("Original room title");
        assertThatThrownBy(() -> service.createConversation(roomId, "other@example.test")).isInstanceOf(ChatException.class);
    }

    @Test void duplicateConcurrentSendReturnsSamePersistedMessageAndOneEvent() throws Exception {
        Long id = open();
        MessageSendFrame frame = frame(id, "hello");
        frame.setClientMessageId("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA");
        var results = race(() -> service.sendMessage(frame, "tenant@example.test"));
        assertThat(results.get(0).getId()).isEqualTo(results.get(1).getId());
        assertThat(results.get(0).getClientMessageId()).isEqualTo("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        assertThat(results.get(0)).usingRecursiveComparison().isEqualTo(results.get(1));
        assertThat(service.getMessages(id, "tenant@example.test", 20, null, null).getData().get(0))
                .usingRecursiveComparison().isEqualTo(results.get(0));
        assertThat(messages.count()).isEqualTo(1);
        assertThat(conversations.findById(id).orElseThrow().getSequence()).isEqualTo(1);
        assertThat(committed.events).hasSize(1);
        assertThat(committed.events.get(0).message()).usingRecursiveComparison().isEqualTo(results.get(0));
        frame.setContent("changed");
        assertThatThrownBy(() -> service.sendMessage(frame, "tenant@example.test"))
                .isInstanceOfSatisfying(ChatException.class, error -> assertThat(error.getStatus()).isEqualTo(409));
    }

    @Test void distinctConcurrentMessagesReceiveUniqueOrderedSequences() throws Exception {
        Long id = open();
        var results = race(() -> service.sendMessage(frame(id, "message"), "tenant@example.test"));
        assertThat(results).extracting(MessageDTO::getSequence).containsExactlyInAnyOrder(1L, 2L);
        assertThat(committed.events).hasSize(2);
    }

    @Test void previewPreservesEmojiAtTruncationBoundary() {
        Long id = open();
        String prefix = "a".repeat(199);
        service.sendMessage(frame(id, prefix + "\uD83D\uDE00trailing text"), "tenant@example.test");
        assertThat(service.getConversation(id, "host@example.test").getLastMessagePreview()).isEqualTo(prefix);
    }

    @Test void rollbackDoesNotPersistOrPublishAfterCommit() {
        Long id = open();
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            service.sendMessage(frame(id, "rolled back"), "tenant@example.test");
            tx.setRollbackOnly();
        });
        assertThat(messages.count()).isZero();
        assertThat(conversations.findById(id).orElseThrow().getSequence()).isZero();
        assertThat(committed.events).isEmpty();
    }

    @Test void historyCursorsAreExclusiveOrderedAndBounded() {
        Long id = open();
        for (int i = 0; i < 5; i++) service.sendMessage(frame(id, "message " + i), "tenant@example.test");
        MessagePageDTO latest = service.getMessages(id, "host@example.test", 2, null, null);
        assertThat(latest.getData()).extracting(MessageDTO::getSequence).containsExactly(4L, 5L);
        assertThat(latest.getNextCursor()).isEqualTo(4L); assertThat(latest.isHasMore()).isTrue();
        assertThat(service.getMessages(id, "host@example.test", 2, 4L, null).getData())
                .extracting(MessageDTO::getSequence).containsExactly(2L, 3L);
        MessagePageDTO catchup = service.getMessages(id, "host@example.test", 2, null, 1L);
        assertThat(catchup.getData()).extracting(MessageDTO::getSequence).containsExactly(2L, 3L);
        assertThat(catchup.getNextCursor()).isNull(); assertThat(catchup.isHasMore()).isTrue();
        assertThatThrownBy(() -> service.getMessages(id, "tenant@example.test", 51, null, null)).isInstanceOf(ChatException.class);
        assertThatThrownBy(() -> service.getMessages(id, "tenant@example.test", 2, 4L, 1L)).isInstanceOf(ChatException.class);
    }

    @Test void readMarkersNeverRegressOrExceedLatestAndUnreadExcludesOwnMessages() throws Exception {
        Long id = open();
        service.sendMessage(frame(id, "one"), "tenant@example.test");
        service.sendMessage(frame(id, "two"), "tenant@example.test");
        service.sendMessage(frame(id, "host reply"), "host@example.test");
        assertThat(service.getConversation(id, "host@example.test").getUnreadCount()).isEqualTo(2);
        assertThat(service.getConversation(id, "tenant@example.test").getUnreadCount()).isEqualTo(1);
        race(() -> service.markRead(id, "host@example.test", 2L));
        assertThat(service.markRead(id, "host@example.test", 1L).getLastReadSequence()).isEqualTo(2);
        assertThat(service.getConversation(id, "host@example.test").getUnreadCount()).isZero();
        assertThatThrownBy(() -> service.markRead(id, "host@example.test", 4L)).isInstanceOf(ChatException.class);
    }

    @Test void membershipVerificationAndAccountStatusAreEnforced() {
        Long id = open();
        assertThatThrownBy(() -> service.getMessages(id, "other@example.test", 20, null, null))
                .isInstanceOfSatisfying(ChatException.class, error -> assertThat(error.getStatus()).isEqualTo(404));
        assertThatThrownBy(() -> service.sendMessage(frame(id, "attack"), "other@example.test")).isInstanceOf(ChatException.class);
        assertThatThrownBy(() -> service.markRead(id, "other@example.test", 0L)).isInstanceOf(ChatException.class);
        assertThatThrownBy(() -> service.createConversation(roomId, "host@example.test")).isInstanceOf(ChatException.class);
        new TransactionTemplate(manager).executeWithoutResult(tx -> users.findById(tenantId).orElseThrow().setEmailVerified(false));
        assertThat(service.getConversation(id, "tenant@example.test").getId()).isEqualTo(id);
        assertThatThrownBy(() -> service.sendMessage(frame(id, "unverified"), "tenant@example.test"))
                .isInstanceOfSatisfying(ChatException.class, error -> assertThat(error.getCode()).isEqualTo("EMAIL_NOT_VERIFIED"));
        new TransactionTemplate(manager).executeWithoutResult(tx -> users.findById(tenantId).orElseThrow().setSuspended(true));
        assertThatThrownBy(() -> service.getConversation(id, "tenant@example.test"))
                .isInstanceOfSatisfying(ChatException.class, error -> assertThat(error.getCode()).isEqualTo("ACCOUNT_INACTIVE"));
        new TransactionTemplate(manager).executeWithoutResult(tx -> users.findById(tenantId).orElseThrow().setSuspended(false));
        new TransactionTemplate(manager).executeWithoutResult(tx -> users.findById(tenantId).orElseThrow().setStatus(UserStatus.SUSPENDED));
        assertThatThrownBy(() -> service.listConversations("tenant@example.test", 0, 20)).isInstanceOf(ChatException.class);
    }

    @Test void rejectsUnsupportedTypesBlankAndOversizedMessages() {
        Long id = open();
        MessageSendFrame frame = frame(id, "image"); frame.setContentType(MessageContentType.IMAGE);
        assertThatThrownBy(() -> service.sendMessage(frame, "tenant@example.test")).isInstanceOf(ChatException.class);
        assertThatThrownBy(() -> service.sendMessage(frame(id, " "), "tenant@example.test")).isInstanceOf(ChatException.class);
        assertThatThrownBy(() -> service.sendMessage(frame(id, "x".repeat(5001)), "tenant@example.test")).isInstanceOf(ChatException.class);
        assertThat(messages.count()).isZero();
    }

    private Long open() { return service.createConversation(roomId, "tenant@example.test").getId(); }
    private User user(String email) {
        User user = new User(); user.setEmail(email); user.setFullName("Test User");
        user.setPassword("test-hash"); user.setEmailVerified(true); return user;
    }
    private MessageSendFrame frame(Long id, String content) {
        MessageSendFrame frame = new MessageSendFrame(); frame.setConversationId(id); frame.setContent(content);
        frame.setClientMessageId(UUID.randomUUID().toString()); return frame;
    }
    private <T> List<T> race(Callable<T> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<T> waiting = () -> { start.await(); return task.call(); };
            Future<T> first = executor.submit(waiting); Future<T> second = executor.submit(waiting);
            start.countDown();
            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); }
    }
}
