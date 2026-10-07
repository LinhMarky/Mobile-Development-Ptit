-- =============================================
-- V3: Catalog module tables (Phase 2)
-- amenities, rooms, room_amenities, listings,
-- listing_fees, wishlist_items,
-- listing_status_history
-- =============================================

-- Amenities
CREATE TABLE amenities (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL UNIQUE,
    icon     VARCHAR(100),
    category VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default amenities
INSERT INTO amenities (name, icon, category) VALUES
    ('WiFi', 'wifi', 'connectivity'),
    ('Air Conditioning', 'ac_unit', 'comfort'),
    ('Washing Machine', 'local_laundry_service', 'appliance'),
    ('Refrigerator', 'kitchen', 'appliance'),
    ('Water Heater', 'hot_tub', 'comfort'),
    ('Parking', 'local_parking', 'facility'),
    ('Security Camera', 'videocam', 'security'),
    ('Balcony', 'balcony', 'feature'),
    ('Kitchen', 'restaurant', 'feature'),
    ('Private Bathroom', 'bathtub', 'feature'),
    ('Pet Allowed', 'pets', 'policy'),
    ('Furnished', 'chair', 'feature'),
    ('Elevator', 'elevator', 'facility'),
    ('Gym', 'fitness_center', 'facility'),
    ('Swimming Pool', 'pool', 'facility');

-- Rooms
CREATE TABLE rooms (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    host_id         BIGINT NOT NULL,
    unit_code       VARCHAR(30) NOT NULL,
    room_type       VARCHAR(20) NOT NULL,
    area_m2         DECIMAL(6,1) NOT NULL,
    max_occupants   INT NOT NULL DEFAULT 1,
    address_line    VARCHAR(300),
    province_code   VARCHAR(20),
    province_name   VARCHAR(100),
    ward_code       VARCHAR(20),
    ward_name       VARCHAR(100),
    latitude        DOUBLE,
    longitude       DOUBLE,
    availability    VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    terms_version   INT NOT NULL DEFAULT 1,
    version         INT NOT NULL DEFAULT 0,
    created_by      VARCHAR(255),
    created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_by      VARCHAR(255),
    updated_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_rooms_host FOREIGN KEY (host_id) REFERENCES users(id),
    UNIQUE KEY uk_rooms_host_unit (host_id, unit_code),
    INDEX idx_rooms_host (host_id),
    INDEX idx_rooms_availability (availability),
    INDEX idx_rooms_location (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Room-Amenity join table
CREATE TABLE room_amenities (
    room_id    BIGINT NOT NULL,
    amenity_id BIGINT NOT NULL,
    PRIMARY KEY (room_id, amenity_id),
    CONSTRAINT fk_room_amenities_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_room_amenities_amenity FOREIGN KEY (amenity_id) REFERENCES amenities(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Listings
CREATE TABLE listings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id         BIGINT NOT NULL,
    title           VARCHAR(150) NOT NULL DEFAULT '',
    description     VARCHAR(5000) NOT NULL DEFAULT '',
    rent_vnd        DECIMAL(18,0),
    deposit_vnd     DECIMAL(18,0),
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    terms_version   INT NOT NULL DEFAULT 1,
    published_at    DATETIME(3),
    expires_at      DATETIME(3),
    version         INT NOT NULL DEFAULT 0,
    created_by      VARCHAR(255),
    created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_by      VARCHAR(255),
    updated_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_listings_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    INDEX idx_listings_room (room_id),
    INDEX idx_listings_status (status),
    INDEX idx_listings_published (published_at),
    INDEX idx_listings_expires (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Listing fees
CREATE TABLE listing_fees (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id  BIGINT NOT NULL,
    fee_name    VARCHAR(100) NOT NULL,
    amount_vnd  DECIMAL(18,0) NOT NULL,
    unit        VARCHAR(50),
    note        VARCHAR(500),
    sort_order  INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_listing_fees_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
    INDEX idx_listing_fees_listing (listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Wishlist items
CREATE TABLE wishlist_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    listing_id  BIGINT NOT NULL,
    created_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
    UNIQUE KEY uk_wishlist_user_listing (user_id, listing_id),
    INDEX idx_wishlist_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Listing status history (audit trail)
CREATE TABLE listing_status_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id  BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status   VARCHAR(30) NOT NULL,
    actor_id    BIGINT,
    actor_type  VARCHAR(20) NOT NULL,
    reason      VARCHAR(1000),
    trace_id    VARCHAR(64) NOT NULL,
    created_at  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_listing_history_listing FOREIGN KEY (listing_id) REFERENCES listings(id),
    CONSTRAINT fk_listing_history_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    INDEX idx_listing_history (listing_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
