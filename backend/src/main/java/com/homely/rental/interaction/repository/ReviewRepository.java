package com.homely.rental.interaction.repository;

import com.homely.rental.interaction.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByRoomIdAndVisibleTrueOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    Optional<Review> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.room.id = :roomId AND r.visible = true")
    Double getAverageRatingByRoomId(Long roomId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.room.id = :roomId AND r.visible = true")
    long countVisibleByRoomId(Long roomId);
}
