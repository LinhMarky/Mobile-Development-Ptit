package com.homely.rental.interaction.service;

import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.security.UserResolver;
import com.homely.rental.catalog.entity.Room;
import com.homely.rental.catalog.repository.RoomRepository;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.ConflictException;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.common.exception.ResourceNotFoundException;
import com.homely.rental.interaction.dto.*;
import com.homely.rental.interaction.entity.*;
import com.homely.rental.interaction.repository.ViewingRepository;
import com.homely.rental.interaction.repository.ViewingSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Viewing service (VIEW01–VIEW08, BR-05).
 */
@Service
@RequiredArgsConstructor
public class ViewingService {

    private final ViewingSlotRepository slotRepository;
    private final ViewingRepository viewingRepository;
    private final RoomRepository roomRepository;
    private final UserResolver userResolver;
    private final jakarta.persistence.EntityManager entityManager;
    private final com.homely.rental.notification.service.NotificationService notifications;
    private final com.homely.rental.catalog.repository.ListingRepository listings;

    // VIEW02: Create slot (Host)
    @Transactional
    public ViewingSlotDTO createSlot(ViewingSlotCreateRequest dto) throws IdInvalidException {
        User host = getVerifiedHost();
        Room room = roomRepository.findByIdAndHostId(dto.getRoomId(), host.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", dto.getRoomId()));

        // BR-05: Slot must be >= 24h in the future
        if (dto.getStartAt().isBefore(Instant.now().plus(24, ChronoUnit.HOURS))) {
            throw new IdInvalidException("Viewing slot must be at least 24 hours in the future");
        }
        if (!dto.getEndAt().isAfter(dto.getStartAt())) {
            throw new IdInvalidException("end_at must be after start_at");
        }

        ViewingSlot slot = new ViewingSlot();
        slot.setRoom(room);
        slot.setHost(host);
        slot.setStartAt(dto.getStartAt());
        slot.setEndAt(dto.getEndAt());
        slot.setStatus(ViewingSlotStatus.OPEN);

        return toSlotDTO(slotRepository.save(slot));
    }

    // VIEW01: Get available slots for a room (Public)
    @Transactional(readOnly = true)
    public List<ViewingSlotDTO> getSlots(Long roomId) {
        requirePublished(roomId);
        return slotRepository.findByRoomIdAndStatusAndStartAtAfterOrderByStartAt(
                        roomId, ViewingSlotStatus.OPEN, Instant.now())
                .stream().map(this::toSlotDTO).toList();
    }

    // VIEW03: Delete slot (Host)
    @Transactional
    public void deleteSlot(Long slotId) throws IdInvalidException {
        User host = getVerifiedHost();
        ViewingSlot slot = lockedSlot(slotId);
        if (!slot.getHost().getId().equals(host.getId())) {
            throw new IdInvalidException("You can only delete your own slots");
        }
        if (slot.getStatus() == ViewingSlotStatus.BOOKED) {
            throw new ConflictException("SLOT_BOOKED", "Cannot delete a booked slot");
        }
        slot.setStatus(ViewingSlotStatus.CANCELLED);
        slotRepository.save(slot);
    }

    // VIEW05: Create viewing (Tenant+Verified)
    @Transactional
    public ViewingDTO createViewing(ViewingCreateRequest dto) throws IdInvalidException {
        User tenant = getVerifiedUser();

        ViewingSlot slot = lockedSlot(dto.getViewingSlotId());
        requirePublished(slot.getRoom().getId());

        if (slot.getStatus() != ViewingSlotStatus.OPEN) {
            throw new ConflictException("SLOT_NOT_AVAILABLE", "This slot is no longer available");
        }

        // BR-01.3: Self-action check
        if (slot.getHost().getId().equals(tenant.getId())) {
            throw new ConflictException("SELF_ACTION_NOT_ALLOWED", "Cannot book viewing for your own room");
        }

        // BR-05: Only 1 active viewing per tenant per room
        boolean hasActive = viewingRepository.existsByTenantIdAndRoomIdAndStatusIn(
                tenant.getId(), slot.getRoom().getId(),
                List.of(ViewingStatus.REQUESTED, ViewingStatus.CONFIRMED));
        if (hasActive) {
            throw new ConflictException("DUPLICATE_VIEWING",
                    "You already have an active viewing request for this room");
        }

        // BR-05: Slot must be >= 24h away
        if (slot.getStartAt().isBefore(Instant.now().plus(24, ChronoUnit.HOURS))) {
            throw new IdInvalidException("Slot is less than 24 hours away, cannot book");
        }

        // Mark slot as booked
        slot.setStatus(ViewingSlotStatus.BOOKED);
        slotRepository.save(slot);

        Viewing viewing = new Viewing();
        viewing.setViewingSlot(slot);
        viewing.setTenant(tenant);
        viewing.setRoom(slot.getRoom());
        viewing.setHost(slot.getHost());
        viewing.setStatus(ViewingStatus.REQUESTED);
        viewing.setNote(dto.getNote());

        viewingRepository.save(viewing);
        notifyViewing(viewing, com.homely.rental.notification.entity.NotificationType.VIEWING_REQUESTED, "Có yêu cầu xem phòng mới");
        return toViewingDTO(viewing);
    }

    // VIEW04: My viewings (Auth)
    @Transactional(readOnly = true)
    public ViewingDTO getViewing(Long id) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        Viewing viewing = viewingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Viewing", id));
        if (!viewing.getTenant().getId().equals(user.getId()) && !viewing.getHost().getId().equals(user.getId()))
            throw new ResourceNotFoundException("Viewing", id);
        return toViewingDTO(viewing);
    }

    @Transactional(readOnly = true)
    public PageResponse<ViewingDTO> getMyViewings(Pageable pageable) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        Page<Viewing> page = viewingRepository.findByTenantIdOrderByCreatedAtDesc(user.getId(), pageable);
        return PageResponse.of(page, page.getContent().stream().map(this::toViewingDTO).toList());
    }

    // VIEW04 host variant: viewings for host
    @Transactional(readOnly = true)
    public PageResponse<ViewingDTO> getHostViewings(Pageable pageable) throws IdInvalidException {
        User host = getVerifiedHost();
        Page<Viewing> page = viewingRepository.findByHostIdOrderByCreatedAtDesc(host.getId(), pageable);
        return PageResponse.of(page, page.getContent().stream().map(this::toViewingDTO).toList());
    }

    // VIEW06: Confirm viewing (Host)
    @Transactional
    public ViewingDTO confirmViewing(Long viewingId) throws IdInvalidException {
        User host = getVerifiedHost();
        Viewing viewing = getViewingForHost(viewingId, host.getId());
        assertStatus(viewing, ViewingStatus.REQUESTED, "confirm");

        viewing.setStatus(ViewingStatus.CONFIRMED);
        notifyViewing(viewing, com.homely.rental.notification.entity.NotificationType.VIEWING_CONFIRMED, "Lịch xem phòng đã được xác nhận");
        return toViewingDTO(viewingRepository.save(viewing));
    }

    // VIEW07: Cancel viewing (Auth — tenant or host)
    @Transactional
    public ViewingDTO cancelViewing(Long viewingId, String reason) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        Viewing viewing = lockedViewing(viewingId);

        if (viewing.getStatus() == ViewingStatus.COMPLETED ||
            viewing.getStatus() == ViewingStatus.CANCELLED_BY_TENANT ||
            viewing.getStatus() == ViewingStatus.CANCELLED_BY_HOST ||
            viewing.getStatus() == ViewingStatus.NO_SHOW) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Cannot cancel viewing in status: " + viewing.getStatus());
        }

        boolean isTenant = viewing.getTenant().getId().equals(user.getId());
        boolean isHost = viewing.getHost().getId().equals(user.getId());
        if (!isTenant && !isHost) {
            throw new IdInvalidException("Not authorized to cancel this viewing");
        }

        // BR-05: Tenant can cancel only before 2h
        if (isTenant && viewing.getViewingSlot().getStartAt()
                .isBefore(Instant.now().plus(2, ChronoUnit.HOURS))) {
            throw new IdInvalidException("Cannot cancel less than 2 hours before the viewing");
        }

        viewing.setStatus(isTenant ? ViewingStatus.CANCELLED_BY_TENANT : ViewingStatus.CANCELLED_BY_HOST);
        viewing.setCancelledReason(reason);
        notifyViewing(viewing, com.homely.rental.notification.entity.NotificationType.VIEWING_CANCELLED, "Lịch xem phòng đã bị hủy");

        // Free up the slot
        ViewingSlot slot = viewing.getViewingSlot();
        slot.setStatus(ViewingSlotStatus.OPEN);
        slotRepository.save(slot);

        return toViewingDTO(viewingRepository.save(viewing));
    }

    // VIEW08: Complete viewing (Host)
    @Transactional
    public ViewingDTO completeViewing(Long viewingId) throws IdInvalidException {
        User host = getVerifiedHost();
        Viewing viewing = getViewingForHost(viewingId, host.getId());
        assertStatus(viewing, ViewingStatus.CONFIRMED, "complete");
        if (viewing.getViewingSlot().getStartAt().isAfter(Instant.now()))
            throw new ConflictException("VIEWING_NOT_STARTED", "Viewing has not started yet");

        viewing.setStatus(ViewingStatus.COMPLETED);
        viewing.setCompletedAt(Instant.now());
        notifyViewing(viewing, com.homely.rental.notification.entity.NotificationType.VIEWING_COMPLETED, "Buổi xem phòng đã hoàn tất");
        return toViewingDTO(viewingRepository.save(viewing));
    }

    // ===== DTO Conversion =====

    private ViewingSlotDTO toSlotDTO(ViewingSlot slot) {
        return ViewingSlotDTO.builder()
                .id(slot.getId())
                .roomId(slot.getRoom().getId())
                .hostId(slot.getHost().getId())
                .startAt(slot.getStartAt())
                .endAt(slot.getEndAt())
                .status(slot.getStatus().name())
                .build();
    }

    private ViewingDTO toViewingDTO(Viewing v) {
        return ViewingDTO.builder()
                .id(v.getId())
                .viewingSlotId(v.getViewingSlot().getId())
                .tenantId(v.getTenant().getId())
                .roomId(v.getRoom().getId())
                .hostId(v.getHost().getId())
                .status(v.getStatus().name())
                .note(v.getNote())
                .cancelledReason(v.getCancelledReason())
                .slotStartAt(v.getViewingSlot().getStartAt())
                .slotEndAt(v.getViewingSlot().getEndAt())
                .completedAt(v.getCompletedAt())
                .createdAt(v.getCreatedAt())
                .build();
    }

    // ===== Helpers =====

    private Viewing getViewingForHost(Long viewingId, Long hostId) throws IdInvalidException {
        Viewing viewing = lockedViewing(viewingId);
        if (!viewing.getHost().getId().equals(hostId)) {
            throw new IdInvalidException("Not authorized for this viewing");
        }
        return viewing;
    }

    private void assertStatus(Viewing viewing, ViewingStatus expected, String action) {
        if (viewing.getStatus() != expected) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Cannot " + action + " viewing in status: " + viewing.getStatus());
        }
    }

    // All viewing mutations for a room share its lock, including different slots.
    private ViewingSlot lockedSlot(Long id) {
        Long roomId = slotRepository.findRoomId(id).orElseThrow(() -> new ResourceNotFoundException("ViewingSlot", id));
        roomRepository.lockById(roomId).orElseThrow();
        ViewingSlot slot = slotRepository.findById(id).orElseThrow();
        entityManager.refresh(slot);
        return slot;
    }

    private Viewing lockedViewing(Long id) {
        Long roomId = viewingRepository.findRoomId(id).orElseThrow(() -> new ResourceNotFoundException("Viewing", id));
        roomRepository.lockById(roomId).orElseThrow();
        Viewing viewing = viewingRepository.findById(id).orElseThrow();
        entityManager.refresh(viewing);
        entityManager.refresh(viewing.getViewingSlot());
        return viewing;
    }

    private void requirePublished(Long roomId) {
        var listing = listings.findByRoomId(roomId).orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        if (listing.getStatus() != com.homely.rental.catalog.entity.ListingStatus.PUBLISHED
                || (listing.getExpiresAt() != null && !listing.getExpiresAt().isAfter(Instant.now())))
            throw new ResourceNotFoundException("Room", roomId);
    }

    private void notifyViewing(Viewing v, com.homely.rental.notification.entity.NotificationType type, String title) {
        for (User recipient : List.of(v.getHost(), v.getTenant()))
            notifications.createNotification(recipient, type, title, "Xem chi tiết lịch hẹn trong ứng dụng.", "viewing", v.getId());
    }


    private User getVerifiedUser() throws IdInvalidException {
        User user = userResolver.requireCurrent();
        if (!user.isEmailVerified()) {
            throw new IdInvalidException("Email must be verified");
        }
        return user;
    }

    private User getVerifiedHost() throws IdInvalidException {
        User user = getVerifiedUser();
        if (!user.isHost()) {
            throw new IdInvalidException("User does not have HOST role");
        }
        return user;
    }
}
