package com.homely.rental.media.service;

import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.security.UserResolver;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.common.exception.ResourceNotFoundException;
import com.homely.rental.media.dto.MediaDTO;
import com.homely.rental.media.entity.Media;
import com.homely.rental.media.entity.MediaPurpose;
import com.homely.rental.media.entity.MediaStatus;
import com.homely.rental.media.repository.MediaRepository;
import com.homely.rental.media.validation.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Media service (§2.3, BR-09).
 * Handles file upload, validation, attachment, and DTO conversion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaRepository mediaRepository;
    private final StorageService storageService;
    private final FileValidator fileValidator;
    private final UserResolver userResolver;
    private final com.homely.rental.catalog.repository.RoomRepository rooms;
    private final com.homely.rental.catalog.repository.ListingRepository listings;

    private static final int MAX_ROOM_PHOTOS = 15;
    private static final int MAX_ROOM_VIDEOS = 1;

    /**
     * Upload a file (MEDIA01).
     * Validates content, stores in MinIO, saves metadata as READY.
     */
    @Transactional
    public MediaDTO upload(MultipartFile file, MediaPurpose purpose) throws IdInvalidException {
        User uploader = userResolver.requireCurrent();

        boolean isVideo = purpose == MediaPurpose.ROOM_VIDEO;
        fileValidator.validate(file, isVideo);

        // Generate unique storage key
        String ext = extractExtension(file.getOriginalFilename());
        String storageKey = purpose.name().toLowerCase() + "/" + UUID.randomUUID() + ext;

        // Upload to MinIO
        storageService.upload(storageKey, file, file.getContentType());

        // Save metadata
        Media media = new Media();
        media.setUploader(uploader);
        media.setPurpose(purpose);
        media.setContentType(file.getContentType());
        media.setFileSizeBytes(file.getSize());
        media.setStorageKey(storageKey);
        media.setOriginalFilename(file.getOriginalFilename());
        media.setStatus(MediaStatus.READY);

        Media saved = mediaRepository.save(media);
        log.info("Media uploaded: id={}, key={}, size={}", saved.getId(), storageKey, file.getSize());

        return toDTO(saved);
    }

    /**
     * Attach READY media to a resource (room, listing, etc).
     * Validates ownership, count limits, and transitions to ATTACHED.
     */
    @Transactional
    public List<MediaDTO> attachToResource(List<Long> mediaIds, String resourceType,
                                           Long resourceId, MediaPurpose expectedPurpose)
            throws IdInvalidException {
        User user = userResolver.requireCurrent();

        if (!"room".equals(resourceType) || (expectedPurpose != MediaPurpose.ROOM_PHOTO && expectedPurpose != MediaPurpose.ROOM_VIDEO))
            throw new IllegalArgumentException("Only room photos and videos can be attached here");
        var room = rooms.lockById(resourceId).orElseThrow(() -> new ResourceNotFoundException("Room", resourceId));
        if (!user.isHost() || !user.isEmailVerified() || !room.getHost().getId().equals(user.getId()))
            throw new ResourceNotFoundException("Room", resourceId);
        if (room.getAvailability() == com.homely.rental.catalog.entity.RoomAvailability.HELD)
            throw new com.homely.rental.common.exception.ConflictException("ROOM_HELD", "Cannot change photos during a booking hold");
        if (mediaIds.stream().distinct().count() != mediaIds.size()) throw new IllegalArgumentException("Duplicate media IDs");

        // Load and validate ownership
        List<Media> mediaList = mediaRepository.findByIdInAndUploaderId(mediaIds, user.getId());
        if (mediaList.size() != mediaIds.size()) {
            throw new IdInvalidException("One or more media IDs not found or not owned by you");
        }

        // Validate all are READY and correct purpose
        for (Media m : mediaList) {
            if (m.getStatus() != MediaStatus.READY) {
                throw new IdInvalidException("Media " + m.getId() + " is already attached or deleted");
            }
            if (m.getPurpose() != expectedPurpose) {
                throw new IdInvalidException("Media " + m.getId() + " has wrong purpose. Expected: " + expectedPurpose);
            }
        }

        // Check count limits
        long existingCount = mediaRepository.countByResourceTypeAndResourceIdAndStatusAndPurpose(
                resourceType, resourceId, MediaStatus.ATTACHED, expectedPurpose);
        boolean isVideo = expectedPurpose == MediaPurpose.ROOM_VIDEO;
        int maxAllowed = isVideo ? MAX_ROOM_VIDEOS : MAX_ROOM_PHOTOS;

        if (existingCount + mediaList.size() > maxAllowed) {
            throw new IdInvalidException("Exceeds max media limit (" + maxAllowed +
                    ") for " + resourceType + ". Currently: " + existingCount);
        }

        // Attach
        int order = (int) existingCount;
        for (Media m : mediaList) {
            m.setStatus(MediaStatus.ATTACHED);
            m.setResourceType(resourceType);
            m.setResourceId(resourceId);
            m.setDisplayOrder(order++);
        }
        List<Media> saved = mediaRepository.saveAll(mediaList);

        return saved.stream().map(this::toDTO).toList();
    }

    /**
     * Get all attached media for a resource.
     */
    @Transactional(readOnly = true)
    public List<MediaDTO> getResourceMedia(String resourceType, Long resourceId) {
        var room = rooms.findById(resourceId).orElseThrow(() -> new ResourceNotFoundException("Room", resourceId));
        boolean owner = userResolver.currentIfAuthenticated()
                .map(user -> room.getHost().getId().equals(user.getId())).orElse(false);
        var listing = listings.findByRoomId(resourceId).orElse(null);
        if (!owner && (listing == null || listing.getStatus() != com.homely.rental.catalog.entity.ListingStatus.PUBLISHED
                || (listing.getExpiresAt() != null && !listing.getExpiresAt().isAfter(java.time.Instant.now()))))
            throw new ResourceNotFoundException("Room", resourceId);
        List<Media> mediaList = mediaRepository
                .findByResourceTypeAndResourceIdAndStatusOrderByDisplayOrder(
                        resourceType, resourceId, MediaStatus.ATTACHED);
        return mediaList.stream().map(this::toDTO).toList();
    }

    /**
     * Delete a media record (soft-delete + remove from storage).
     */
    @Transactional
    public void deleteMedia(Long mediaId) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", mediaId));

        if (!media.getUploader().getId().equals(user.getId())) {
            throw new IdInvalidException("You can only delete your own media");
        }
        if (media.getStatus() == MediaStatus.ATTACHED && "room".equals(media.getResourceType())) {
            var room = rooms.lockById(media.getResourceId()).orElseThrow();
            if (!room.getHost().getId().equals(user.getId())) throw new ResourceNotFoundException("Media", mediaId);
            if (room.getAvailability() == com.homely.rental.catalog.entity.RoomAvailability.HELD)
                throw new com.homely.rental.common.exception.ConflictException("ROOM_HELD", "Cannot remove photos during a booking hold");
        }

        // Delete from storage
        storageService.delete(media.getStorageKey());

        // Soft-delete in DB
        media.setStatus(MediaStatus.DELETED);
        mediaRepository.save(media);
    }

    /**
     * Get user's READY (unattached) media for a given purpose.
     */
    @Transactional(readOnly = true)
    public List<MediaDTO> getMyReadyMedia(MediaPurpose purpose) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        List<Media> media = mediaRepository.findByUploaderIdAndStatusAndPurpose(
                user.getId(), MediaStatus.READY, purpose);
        return media.stream().map(this::toDTO).toList();
    }

    // ===== DTO Conversion =====

    public record Content(String contentType, long size, java.io.InputStream stream) {}

    @Transactional(readOnly = true)
    public Content content(Long id) {
        Media media = mediaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Media", id));
        if (media.getStatus() == MediaStatus.DELETED) throw new ResourceNotFoundException("Media", id);
        boolean owner = userResolver.currentIfAuthenticated()
                .map(user -> media.getUploader().getId().equals(user.getId())).orElse(false);
        if (!owner) {
            if (media.getStatus() != MediaStatus.ATTACHED || !"room".equals(media.getResourceType()))
                throw new ResourceNotFoundException("Media", id);
            getResourceMedia("room", media.getResourceId());
        }
        return new Content(media.getContentType(), media.getFileSizeBytes(), storageService.open(media.getStorageKey()));
    }

    public MediaDTO toDTO(Media media) {
        return MediaDTO.builder()
                .id(media.getId())
                .contentType(media.getContentType())
                .fileSizeBytes(media.getFileSizeBytes())
                .originalFilename(media.getOriginalFilename())
                .url("/api/v1/media/" + media.getId() + "/content")
                .width(media.getWidth())
                .height(media.getHeight())
                .durationSecs(media.getDurationSecs())
                .purpose(media.getPurpose().name())
                .status(media.getStatus().name())
                .displayOrder(media.getDisplayOrder())
                .createdAt(media.getCreatedAt())
                .build();
    }

    // ===== Helpers =====


    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
