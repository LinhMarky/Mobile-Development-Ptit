-- =============================================
-- V5: Fix catalog schema per contract (Phase 2 fixes)
-- B9: Add UNIQUE(room_id) on listings
-- B10: Update listing_fees columns to fee_code, fee_mode, unit_name
-- B11: Change wishlist_items FK to room_id instead of listing_id
-- =============================================

-- B9: Listings should have 1:1 relationship with Room
ALTER TABLE listings ADD CONSTRAINT uk_listings_room UNIQUE (room_id);

-- B10: Update listing_fees schema to match contract
ALTER TABLE listing_fees
    CHANGE COLUMN fee_name fee_code VARCHAR(50) NOT NULL,
    ADD COLUMN fee_mode VARCHAR(20) NOT NULL DEFAULT 'FIXED' AFTER fee_code,
    CHANGE COLUMN unit unit_name VARCHAR(50);

-- B11: Change wishlist_items FK from listing_id to room_id
-- First drop the existing FK and index/unique constraint
ALTER TABLE wishlist_items DROP FOREIGN KEY fk_wishlist_listing;
ALTER TABLE wishlist_items DROP INDEX uk_wishlist_user_listing;

-- Rename column and recreate constraints
ALTER TABLE wishlist_items CHANGE COLUMN listing_id room_id BIGINT NOT NULL;
ALTER TABLE wishlist_items ADD CONSTRAINT fk_wishlist_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE;
ALTER TABLE wishlist_items ADD CONSTRAINT uk_wishlist_user_room UNIQUE (user_id, room_id);
