package com.homely.rental.media.repository;

import com.homely.rental.media.entity.Media;
import com.homely.rental.media.entity.MediaPurpose;
import com.homely.rental.media.entity.MediaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    List<Media> findByResourceTypeAndResourceIdAndStatusOrderByDisplayOrder(
            String resourceType, Long resourceId, MediaStatus status);

    List<Media> findByIdInAndUploaderId(List<Long> ids, Long uploaderId);

    long countByResourceTypeAndResourceIdAndStatus(String resourceType, Long resourceId, MediaStatus status);
    long countByResourceTypeAndResourceIdAndStatusAndPurpose(String resourceType, Long resourceId, MediaStatus status, MediaPurpose purpose);

    @Query("SELECT m FROM Media m WHERE m.status = 'READY' AND m.createdAt < :cutoff")
    List<Media> findOrphanMedia(Instant cutoff);

    @Modifying
    @Query("UPDATE Media m SET m.status = :status, m.resourceType = :resourceType, " +
           "m.resourceId = :resourceId, m.updatedAt = :now WHERE m.id IN :ids")
    int attachMedia(List<Long> ids, String resourceType, Long resourceId, MediaStatus status, Instant now);

    List<Media> findByUploaderIdAndStatusAndPurpose(Long uploaderId, MediaStatus status, MediaPurpose purpose);
}
