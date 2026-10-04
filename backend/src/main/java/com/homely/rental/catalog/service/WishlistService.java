package com.homely.rental.catalog.service;

import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.security.UserResolver;
import com.homely.rental.catalog.entity.Room;
import com.homely.rental.catalog.entity.WishlistItem;
import com.homely.rental.catalog.repository.RoomRepository;
import com.homely.rental.catalog.repository.WishlistRepository;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Wishlist service (CAT11–CAT13).
 * Handles save/unsave rooms and listing saved rooms.
 */
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final RoomRepository roomRepository;
    private final UserResolver userResolver;

    // CAT11: Save room to wishlist
    @Transactional
    public Map<String, String> saveRoom(Long roomId) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        if (wishlistRepository.existsByUserIdAndRoomId(user.getId(), roomId)) {
            return Map.of("status", "already_saved");
        }

        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setRoom(room);
        wishlistRepository.save(item);

        return Map.of("status", "saved");
    }

    // CAT12: Remove room from wishlist
    @Transactional
    public Map<String, String> unsaveRoom(Long roomId) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        wishlistRepository.deleteByUserIdAndRoomId(user.getId(), roomId);
        return Map.of("status", "removed");
    }

    // CAT13: List saved rooms
    public PageResponse<Map<String, Object>> getSavedRooms(Pageable pageable) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        Page<WishlistItem> page = wishlistRepository.findByUserId(user.getId(), pageable);
        var items = page.getContent().stream().map(wi -> Map.<String, Object>of(
                "id", wi.getId(),
                "room_id", wi.getRoom().getId(),
                "unit_code", wi.getRoom().getUnitCode(),
                "address", wi.getRoom().getAddress() != null ? wi.getRoom().getAddress().getLine() : "",
                "saved_at", wi.getCreatedAt().toString()
        )).toList();
        return PageResponse.of(page, items);
    }

    // Check if room is saved
    public boolean isSaved(Long roomId) throws IdInvalidException {
        User user = userResolver.requireCurrent();
        return wishlistRepository.existsByUserIdAndRoomId(user.getId(), roomId);
    }

}
