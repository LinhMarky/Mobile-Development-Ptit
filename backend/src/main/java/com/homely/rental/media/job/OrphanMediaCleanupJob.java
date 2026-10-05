package com.homely.rental.media.job;

import com.homely.rental.media.entity.Media;
import com.homely.rental.media.repository.MediaRepository;
import com.homely.rental.media.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Cleanup job for orphan media files (BR-13).
 * Deletes READY media older than 24 hours that were never attached to a resource.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrphanMediaCleanupJob {

    private final MediaRepository mediaRepository;
    private final StorageService storageService;

    /**
     * Run daily at 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOrphanMedia() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        List<Media> orphans = mediaRepository.findOrphanMedia(cutoff);

        if (orphans.isEmpty()) {
            log.debug("No orphan media to clean up");
            return;
        }

        log.info("Cleaning up {} orphan media files older than {}", orphans.size(), cutoff);

        int deleted = 0;
        for (Media media : orphans) {
            try {
                storageService.delete(media.getStorageKey());
                mediaRepository.delete(media);
                deleted++;
            } catch (Exception e) {
                log.error("Failed to clean up media id={}: {}", media.getId(), e.getMessage());
            }
        }

        log.info("Orphan media cleanup complete: {}/{} deleted", deleted, orphans.size());
    }
}
