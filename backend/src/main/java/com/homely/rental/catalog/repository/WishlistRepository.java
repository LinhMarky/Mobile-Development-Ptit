package com.homely.rental.catalog.repository;

import com.homely.rental.catalog.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    Optional<WishlistItem> findByUserIdAndRoomId(Long userId, Long roomId);
    boolean existsByUserIdAndRoomId(Long userId, Long roomId);
    Page<WishlistItem> findByUserId(Long userId, Pageable pageable);
    void deleteByUserIdAndRoomId(Long userId, Long roomId);
}
