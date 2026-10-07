package com.homely.rental.chat.service;

import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.repository.UserRepository;
import com.homely.rental.catalog.entity.Listing;
import com.homely.rental.catalog.entity.ListingStatus;
import com.homely.rental.catalog.entity.Room;
import com.homely.rental.catalog.repository.ListingRepository;
import com.homely.rental.chat.dto.*;
import com.homely.rental.chat.entity.*;
import com.homely.rental.chat.repository.*;
import com.homely.rental.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final UserRepository users;
    private final com.homely.rental.auth.security.AccountAccessService accounts;
    private final ChatRoomRepository rooms;
    private final ListingRepository listings;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ReadMarkerRepository readMarkers;
    private final ApplicationEventPublisher events;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ConversationDTO createConversation(Long roomId, String email) {
        User tenant = requireActiveUser(email);
        requireVerified(tenant);
        requirePositiveId(roomId, "room_id");
        // The room is the stable lock even before a conversation exists.
        Room room = rooms.findLockedById(roomId)
                .orElseThrow(() -> new ChatException(404, "RESOURCE_NOT_FOUND", "Room not found"));
        if (Objects.equals(room.getHost().getId(), tenant.getId())) {
            throw new ChatException(409, "SELF_ACTION_NOT_ALLOWED", "You cannot open a conversation with yourself");
        }
        Conversation existing = conversations.findByRoomIdAndTenantId(roomId, tenant.getId()).orElse(null);
        if (existing != null) return toConversation(existing, tenant.getId());

        Listing listing = listings.findByRoomId(roomId).orElse(null);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        if (listing == null || listing.getStatus() != ListingStatus.PUBLISHED
                || listing.getExpiresAt() == null || !listing.getExpiresAt().isAfter(now)) {
            throw new ChatException(409, "LISTING_NOT_AVAILABLE", "The room does not have an active published listing");
        }
        Conversation conversation = new Conversation();
        conversation.setRoom(room);
        conversation.setTenant(tenant);
        conversation.setHost(room.getHost());
        conversation.setRoomTitle(listing.getTitle());
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversations.saveAndFlush(conversation);
        return toConversation(conversation, tenant.getId());
    }

    public PageResponse<ConversationDTO> listConversations(String email, int page, int size) {
        User user = requireActiveUser(email);
        if (page < 0 || size < 1 || size > 50) throw invalid("page must be >= 0 and size must be between 1 and 50");
        var result = conversations.findForMember(user.getId(), PageRequest.of(page, size));
        return PageResponse.of(result, result.getContent().stream()
                .map(conversation -> toConversation(conversation, user.getId())).toList());
    }

    public ConversationDTO getConversation(Long id, String email) {
        User user = requireActiveUser(email);
        return toConversation(findMemberConversation(id, user, false), user.getId());
    }

    public MessagePageDTO getMessages(Long id, String email, int limit, Long before, Long after) {
        requireMember(id, email);
        if (limit < 1 || limit > 50) throw invalid("limit must be between 1 and 50");
        if (before != null && after != null) throw invalid("Use only one message cursor");
        if (before != null && before < 1 || after != null && after < 0) throw invalid("Invalid message cursor");
        // Fetch one extra row to determine availability in the requested direction.
        var window = PageRequest.of(0, limit + 1);
        List<Message> found = after != null
                ? messages.findByConversationIdAndSequenceGreaterThanOrderBySequenceAsc(id, after, window)
                : before != null
                ? messages.findByConversationIdAndSequenceLessThanOrderBySequenceDesc(id, before, window)
                : messages.findByConversationIdOrderBySequenceDesc(id, window);
        boolean hasMore = found.size() > limit;
        List<Message> selected = new ArrayList<>(found.subList(0, Math.min(limit, found.size())));
        if (after == null) Collections.reverse(selected);
        Long nextCursor = after == null && hasMore && !selected.isEmpty() ? selected.get(0).getSequence() : null;
        return MessagePageDTO.builder().data(selected.stream().map(this::toMessage).toList())
                .nextCursor(nextCursor).hasMore(hasMore).build();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MessageDTO sendMessage(MessageSendFrame frame, String email) {
        User sender = requireActiveUser(email);
        requireVerified(sender);
        if (frame == null) throw invalid("Message body is required");
        String content = frame.getContent();
        if (content == null || content.isBlank() || content.length() > 5000) {
            throw invalid("content must contain between 1 and 5000 characters and cannot be blank");
        }
        String clientId = frame.getClientMessageId();
        if (clientId == null || !UUID_PATTERN.matcher(clientId).matches()) throw invalid("client_message_id must be a UUID");
        clientId = clientId.toLowerCase(Locale.ROOT);
        if (frame.getContentType() == null) throw invalid("content_type is required");
        Conversation conversation = findMemberConversation(frame.getConversationId(), sender, true);
        Message existing = messages.findByConversationIdAndClientMessageId(conversation.getId(), clientId).orElse(null);
        if (existing != null) {
            if (!Objects.equals(existing.getSender().getId(), sender.getId())
                    || !existing.getContent().equals(content) || existing.getContentType() != frame.getContentType()) {
                throw new ChatException(409, "CLIENT_MESSAGE_ID_CONFLICT", "client_message_id was already used for a different message");
            }
            return toMessage(existing);
        }
        if (frame.getContentType() != MessageContentType.TEXT) throw invalid("Only TEXT messages are supported");
        // V11 uses DATETIME(3); ACK, event and later history must share the same timestamp.
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        message.setClientMessageId(clientId);
        message.setContentType(MessageContentType.TEXT);
        message.setSequence(Math.addExact(conversation.getSequence(), 1));
        message.setCreatedAt(now);
        conversation.setSequence(message.getSequence());
        conversation.setLastMessageAt(now);
        int previewEnd = Math.min(content.length(), 200);
        // Keep the length bound without leaving half of a supplementary character.
        if (previewEnd < content.length() && Character.isHighSurrogate(content.charAt(previewEnd - 1))) {
            previewEnd--;
        }
        conversation.setLastMessagePreview(content.substring(0, previewEnd));
        conversation.setUpdatedAt(now);
        messages.saveAndFlush(message);
        MessageDTO result = toMessage(message);
        // Consumers use AFTER_COMMIT; rollback must never deliver an uncommitted message.
        events.publishEvent(new ChatMessageCreatedEvent(result,
                conversation.getTenant().getEmail(), conversation.getHost().getEmail()));
        return result;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReadMarkerDTO markRead(Long id, String email, Long sequence) {
        User user = requireActiveUser(email);
        Conversation conversation = findMemberConversation(id, user, true);
        if (sequence == null || sequence < 0 || sequence > conversation.getSequence()) {
            throw invalid("last_read_sequence must be between 0 and the conversation's latest sequence");
        }
        ReadMarkerId markerId = new ReadMarkerId(id, user.getId());
        ReadMarker marker = readMarkers.findById(markerId).orElse(null);
        if (marker == null) {
            marker = new ReadMarker();
            marker.setId(markerId);
            marker.setLastReadSequence(sequence);
            readMarkers.save(marker);
        } else if (sequence > marker.getLastReadSequence()) {
            marker.setLastReadSequence(sequence);
            marker.setUpdatedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        }
        return ReadMarkerDTO.builder().conversationId(id).userId(user.getId())
                .lastReadSequence(marker.getLastReadSequence()).updatedAt(marker.getUpdatedAt()).build();
    }

    public Conversation requireMember(Long id, String email) {
        return findMemberConversation(id, requireActiveUser(email), false);
    }

    public User requireActiveUser(String email) {
        try {
            return accounts.requireActive(email);
        } catch (com.homely.rental.auth.security.AccountAccessException ex) {
            throw new ChatException(ex.getStatus(), ex.getCode(), ex.getMessage());
        }
    }

    private Conversation findMemberConversation(Long id, User user, boolean lock) {
        requirePositiveId(id, "conversation_id");
        Conversation conversation = (lock ? conversations.findLockedById(id) : conversations.findById(id))
                .orElseThrow(() -> new ChatException(404, "RESOURCE_NOT_FOUND", "Conversation not found"));
        if (!Objects.equals(conversation.getTenant().getId(), user.getId())
                && !Objects.equals(conversation.getHost().getId(), user.getId())) {
            throw new ChatException(404, "RESOURCE_NOT_FOUND", "Conversation not found");
        }
        return conversation;
    }

    private void requireVerified(User user) {
        if (!user.isEmailVerified()) throw new ChatException(403, "EMAIL_NOT_VERIFIED", "Verify your email before using this action");
    }

    private void requirePositiveId(Long id, String field) {
        if (id == null || id < 1) throw invalid(field + " must be positive");
    }

    private ChatException invalid(String detail) {
        return new ChatException(400, "VALIDATION_FAILED", detail);
    }

    private ConversationDTO toConversation(Conversation conversation, Long userId) {
        long marker = readMarkers.findById(new ReadMarkerId(conversation.getId(), userId))
                .map(ReadMarker::getLastReadSequence).orElse(0L);
        long unread = messages.countByConversationIdAndSenderIdNotAndSequenceGreaterThan(conversation.getId(), userId, marker);
        return ConversationDTO.builder().id(conversation.getId()).roomId(conversation.getRoom().getId())
                .tenantId(conversation.getTenant().getId()).hostId(conversation.getHost().getId())
                .roomTitle(conversation.getRoomTitle()).lastMessageAt(conversation.getLastMessageAt())
                .lastMessagePreview(conversation.getLastMessagePreview()).sequence(conversation.getSequence())
                .lastReadSequence(marker).unreadCount(unread).createdAt(conversation.getCreatedAt()).build();
    }

    private MessageDTO toMessage(Message message) {
        return MessageDTO.builder().id(message.getId()).conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId()).content(message.getContent()).clientMessageId(message.getClientMessageId())
                .sequence(message.getSequence()).contentType(message.getContentType()).createdAt(message.getCreatedAt()).build();
    }
}
