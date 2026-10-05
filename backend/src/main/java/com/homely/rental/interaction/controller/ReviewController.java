package com.homely.rental.interaction.controller;

import com.homely.rental.common.annotation.ApiMessage;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.interaction.dto.*;
import com.homely.rental.interaction.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Review controller (REV01–REV04).
 */
@RestController
@RequestMapping(path = "${apiPrefix}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // REV01: Create review
    @PostMapping
    @ApiMessage("Create review")
    public ResponseEntity<ReviewDTO> createReview(
            @Valid @RequestBody ReviewCreateRequest request) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(request));
    }

    // REV02: Get reviews for a room
    @GetMapping("/room/{roomId}")
    @ApiMessage("Get room reviews")
    public ResponseEntity<PageResponse<ReviewDTO>> getRoomReviews(
            @PathVariable Long roomId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getRoomReviews(roomId, pageable));
    }

    // REV02 stats: Get review stats for a room
    @GetMapping("/room/{roomId}/stats")
    @ApiMessage("Get room review stats")
    public ResponseEntity<ReviewService.ReviewStatsDTO> getRoomReviewStats(@PathVariable Long roomId) {
        return ResponseEntity.ok(reviewService.getRoomReviewStats(roomId));
    }

    // REV03: Update review
    @PatchMapping("/{id}")
    @ApiMessage("Update review")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewUpdateRequest request) throws IdInvalidException {
        return ResponseEntity.ok(reviewService.updateReview(id, request));
    }

    // REV04: Delete review
    @DeleteMapping("/{id}")
    @ApiMessage("Delete review")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) throws IdInvalidException {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
