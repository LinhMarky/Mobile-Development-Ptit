package com.homely.rental.catalog.job;

import com.homely.rental.catalog.entity.Listing;
import com.homely.rental.catalog.entity.ListingStatus;
import com.homely.rental.catalog.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled job to expire listings past their expiresAt date (BR-13).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ListingExpirationJob {

    private final ListingRepository listingRepository;

    @Scheduled(fixedRate = 300_000) // Every 5 minutes
    @Transactional
    public void expireListings() {
        List<Listing> expired = listingRepository.findExpiredListings(Instant.now());
        for (Listing listing : expired) {
            listing.setStatus(ListingStatus.EXPIRED);
            listingRepository.save(listing);
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} listings", expired.size());
        }
    }
}
