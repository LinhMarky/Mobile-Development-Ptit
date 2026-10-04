package com.homely.rental.interaction.service;

import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.security.UserResolver;
import com.homely.rental.booking.entity.Booking;
import com.homely.rental.booking.entity.BookingStatus;
import com.homely.rental.booking.repository.BookingRepository;
import com.homely.rental.common.exception.ConflictException;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.common.exception.ResourceNotFoundException;
import com.homely.rental.interaction.dto.*;
import com.homely.rental.interaction.entity.Review;
import com.homely.rental.interaction.repository.ReviewRepository;
import com.homely.rental.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Review service (REV01–REV04, BR-10).
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserResolver userResolver;

    // REV01: Create review (Tenant+Verified, after Booking COMPLETED)
    @Transactional
    public ReviewDTO createReview(ReviewCreateRequest dto) throws IdInvalidException {
        User reviewer = getVerifiedUser();

        // BR-10: 1 review per booking
        if (reviewRepository.existsByBookingId(dto.getBookingId())) {
            throw new ConflictException("DUPLICATE_REVIEW", "You have already reviewed this booking");
        }

        // Validate booking exists and is COMPLETED
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", dto.getBookingId()));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IdInvalidException("Can only review COMPLETED bookings (current: " + booking.getStatus() + ")");
        }

        // BR-10: Reviewer must be the tenant of the booking
        if (!booking.getTenant().getId().equals(reviewer.getId())) {
            throw new IdInvalidException("Only the tenant of this booking can write a review");
        }

        Review review = new Review();
        review.setBookingId(dto.getBookingId());
        review.setReviewer(reviewer);
        review.setRoom(booking.getRoom());
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        return toDTO(reviewRepository.save(review));
    }

    // REV02: Get reviews for a room (Public)
    @Transactional(readOnly = true)
    public PageResponse<ReviewDTO> getRoomReviews(Long roomId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByRoomIdAndVisibleTrueOrderByCreatedAtDesc(roomId, pageable);
        return PageResponse.of(page, page.getContent().stream().map(this::toDTO).toList());
    }

    // REV03: Update review (Owner only)
    @Transactional
    public ReviewDTO updateReview(Long reviewId, ReviewUpdateRequest dto) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        if (!review.getReviewer().getId().equals(user.getId())) {
            throw new IdInvalidException("You can only edit your own reviews");
        }

        if (dto.getRating() != null) review.setRating(dto.getRating());
        if (dto.getComment() != null) review.setComment(dto.getComment());

        return toDTO(reviewRepository.save(review));
    }

    // REV04: Delete review (Owner only)
    @Transactional
    public void deleteReview(Long reviewId) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        if (!review.getReviewer().getId().equals(user.getId())) {
            throw new IdInvalidException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }

    // Aggregate: review stats for a room
    @Transactional(readOnly = true)
    public ReviewStatsDTO getRoomReviewStats(Long roomId) {
        Double avg = reviewRepository.getAverageRatingByRoomId(roomId);
        long count = reviewRepository.countVisibleByRoomId(roomId);
        return new ReviewStatsDTO(avg != null ? avg : 0.0, count);
    }

    // ===== DTO =====

    private ReviewDTO toDTO(Review r) {
        return ReviewDTO.builder()
                .id(r.getId())
                .bookingId(r.getBookingId())
                .reviewerId(r.getReviewer().getId())
                .reviewerName(r.getReviewer().getFullName())
                .roomId(r.getRoom() != null ? r.getRoom().getId() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }


    private User getVerifiedUser() throws IdInvalidException {
        User user = userResolver.requireCurrent();
        if (!user.isEmailVerified()) {
            throw new IdInvalidException("Email must be verified to create a review");
        }
        return user;
    }

    // Simple stats record
    public record ReviewStatsDTO(double averageRating, long totalReviews) {}
}
