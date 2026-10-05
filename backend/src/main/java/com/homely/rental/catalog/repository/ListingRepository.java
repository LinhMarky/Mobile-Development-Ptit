package com.homely.rental.catalog.repository;

import com.homely.rental.catalog.entity.Listing;
import com.homely.rental.catalog.entity.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
    @Query("select l.room.id from Listing l where l.id = :id")
    Optional<Long> findRoomId(Long id);
    Optional<Listing> findByIdAndRoomHostId(Long id, Long hostId);
    Page<Listing> findByRoomHostId(Long hostId, Pageable pageable);
    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);
    Optional<Listing> findByRoomId(Long roomId);

    // Legacy published rows without expiresAt remain visible until a separate data migration sets it.
    @Query("SELECT l FROM Listing l WHERE l.status = 'PUBLISHED' AND l.room.availability = 'AVAILABLE' " +
            "AND (l.expiresAt IS NULL OR l.expiresAt > ?1)")
    Page<Listing> findPublishedAvailableListings(Instant now, Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE l.status = 'PUBLISHED' AND l.room.availability = 'AVAILABLE' " +
            "AND (l.expiresAt IS NULL OR l.expiresAt > ?2) " +
            "AND (LOWER(l.title) LIKE LOWER(CONCAT('%', ?1, '%')) OR LOWER(l.description) LIKE LOWER(CONCAT('%', ?1, '%')) " +
            "OR LOWER(l.room.address.line) LIKE LOWER(CONCAT('%', ?1, '%')) " +
            "OR LOWER(l.room.address.provinceName) LIKE LOWER(CONCAT('%', ?1, '%')))")
    Page<Listing> searchPublishedListings(String query, Instant now, Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE l.status = 'PUBLISHED' AND l.expiresAt IS NOT NULL AND l.expiresAt <= ?1")
    java.util.List<Listing> findExpiredListings(Instant now);
}
