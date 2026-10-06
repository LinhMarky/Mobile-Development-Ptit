package com.homely.rental.catalog.controller;

import com.homely.rental.catalog.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.IdInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Wishlist controller (CAT11–CAT13).
 * Handles save/unsave/list operations for authenticated users.
 */
@RestController
@RequestMapping(path = "${apiPrefix}/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    // CAT11: Save room
    @PostMapping("/{roomId}")
    @Operation(summary = "Save room to wishlist")
    public ResponseEntity<Map<String, String>> saveRoom(@PathVariable Long roomId) throws IdInvalidException {
        return ResponseEntity.ok(wishlistService.saveRoom(roomId));
    }

    // CAT12: Unsave room
    @DeleteMapping("/{roomId}")
    @Operation(summary = "Remove room from wishlist")
    public ResponseEntity<Map<String, String>> unsaveRoom(@PathVariable Long roomId) throws IdInvalidException {
        return ResponseEntity.ok(wishlistService.unsaveRoom(roomId));
    }

    // CAT13: List saved rooms
    @GetMapping
    @Operation(summary = "List saved rooms")
    public ResponseEntity<PageResponse<Map<String, Object>>> getSavedRooms(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
            throws IdInvalidException {
        return ResponseEntity.ok(wishlistService.getSavedRooms(pageable));
    }

    // Check if room is saved
    @GetMapping("/{roomId}/check")
    @Operation(summary = "Check if room is saved")
    public ResponseEntity<Map<String, Boolean>> checkSaved(@PathVariable Long roomId) throws IdInvalidException {
        return ResponseEntity.ok(Map.of("saved", wishlistService.isSaved(roomId)));
    }
}
