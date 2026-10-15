package com.homely.student14.repository;

import com.homely.student14.dto.request.catalog.SaveListingDTO;
import com.homely.student14.dto.request.catalog.SaveRoomDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CatalogRepository {
    public void expirePublishedListings() {
        this.databaseRepository.update("""
                UPDATE listings SET status = 'EXPIRED', version = version + 1
                WHERE status = 'PUBLISHED' AND expires_at IS NOT NULL AND expires_at <= UTC_TIMESTAMP(3)
                """);
    }
    private final DatabaseRepository databaseRepository;

    public static final String LISTING_SELECT =
            """
            SELECT l.*,
                   r.host_id, r.unit_code, r.name AS room_name, r.room_type,
                   r.area_m2, r.max_occupants, r.address_line, r.province_name,
                   r.ward_name, r.availability, r.amenity_ids,
                   u.full_name AS host_name
            FROM listings l
            JOIN rooms r ON r.id = l.room_id
            JOIN users u ON u.id = r.host_id
            """;

    public List<Map<String, Object>> findPublishedListings() {
        String query =
                LISTING_SELECT
                        + """
                          WHERE l.status = 'PUBLISHED'
                            AND r.availability = 'AVAILABLE'
                            AND (l.expires_at IS NULL OR l.expires_at > UTC_TIMESTAMP(3))
                          ORDER BY l.id DESC
                          """;
        return this.databaseRepository.all(query);
    }

    public Map<String, Object> findListingById(long listingId) {
        String query = LISTING_SELECT + "WHERE l.id = ?";
        return this.databaseRepository.one(query, listingId);
    }

    public Map<String, Object> findListingByIdForUpdate(long listingId) {
        String query = LISTING_SELECT + "WHERE l.id = ? FOR UPDATE";
        return this.databaseRepository.one(query, listingId);
    }

    public List<Map<String, Object>> findListingsByHostId(long hostId) {
        String query = LISTING_SELECT + "WHERE r.host_id = ? ORDER BY l.id DESC";
        return this.databaseRepository.all(query, hostId);
    }

    public List<Map<String, Object>> findWishlistByUserId(long userId) {
        String query =
                LISTING_SELECT
                        + """
                          JOIN wishlist_items w ON w.room_id = r.id
                          WHERE w.user_id = ? AND l.status = 'PUBLISHED'
                            AND (l.expires_at IS NULL OR l.expires_at > UTC_TIMESTAMP(3))
                          ORDER BY w.id DESC
                          """;
        return this.databaseRepository.all(query, userId);
    }

    public List<Map<String, Object>> findRoomsByHostId(long hostId) {
        return this.databaseRepository.all(
                "SELECT * FROM rooms WHERE host_id = ? ORDER BY id DESC", hostId);
    }

    public Map<String, Object> findRoomById(long roomId) {
        return this.databaseRepository.one("SELECT * FROM rooms WHERE id = ?", roomId);
    }

    public Map<String, Object> findRoomByIdForUpdate(long roomId) {
        return this.databaseRepository.one("SELECT * FROM rooms WHERE id = ? FOR UPDATE", roomId);
    }

    public List<Map<String, Object>> findRoomImages(long roomId) {
        String query =
                """
                SELECT id, display_order,
                       CONCAT('/api/v1/media/', id, '/content') AS url
                FROM media
                WHERE resource_type = 'room' AND resource_id = ? AND status = 'ATTACHED'
                ORDER BY display_order, id
                """;
        return this.databaseRepository.all(query, roomId);
    }

    public long createRoom(long hostId, SaveRoomDTO request) {
        String query =
                """
                INSERT INTO rooms (
                    host_id, unit_code, name, description, room_type,
                    area_m2, max_occupants, address_line, amenity_ids
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        return this.databaseRepository.insert(
                query,
                hostId,
                request.getUnitCode(),
                request.getName(),
                request.getDescription(),
                request.getRoomType(),
                request.getAreaM2(),
                request.getMaxOccupants(),
                request.getAddressLine(),
                this.databaseRepository.encode(request.getAmenityIds()));
    }

    public void updateRoom(long roomId, SaveRoomDTO request) {
        String query =
                """
                UPDATE rooms
                SET name = ?, description = ?, room_type = ?, area_m2 = ?,
                    max_occupants = ?, address_line = ?, amenity_ids = ?,
                    terms_version = terms_version + 1, version = version + 1
                WHERE id = ?
                """;
        this.databaseRepository.update(
                query,
                request.getName(),
                request.getDescription(),
                request.getRoomType(),
                request.getAreaM2(),
                request.getMaxOccupants(),
                request.getAddressLine(),
                this.databaseRepository.encode(request.getAmenityIds()),
                roomId);
    }

    public List<Map<String, Object>> findRoomListingsForUpdate(long roomId) {
        return this.databaseRepository.all(
                "SELECT * FROM listings WHERE room_id = ? FOR UPDATE", roomId);
    }

    public long createListing(long roomId, SaveListingDTO request) {
        String query =
                """
                INSERT INTO listings (room_id, title, description, rent_vnd, deposit_vnd, fees)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        return this.databaseRepository.insert(
                query,
                roomId,
                request.getTitle(),
                request.getDescription(),
                request.getRentVnd(),
                request.getDepositVnd(),
                this.databaseRepository.encode(request.getFees()));
    }

    public void updateListing(long listingId, SaveListingDTO request) {
        String query =
                """
                UPDATE listings
                SET title = ?, description = ?, rent_vnd = ?, deposit_vnd = ?,
                    fees = ?, status = 'DRAFT', terms_version = terms_version + 1
                WHERE id = ?
                """;
        this.databaseRepository.update(
                query,
                request.getTitle(),
                request.getDescription(),
                request.getRentVnd(),
                request.getDepositVnd(),
                this.databaseRepository.encode(request.getFees()),
                listingId);
    }

    public void updateListingStatus(long listingId, String status, String reason) {
        String query =
                """
                UPDATE listings
                SET status = ?,
                    moderation_reason = ?,
                    published_at = IF(? = 'PUBLISHED', UTC_TIMESTAMP(3), published_at),
                    expires_at = IF(
                        ? = 'PUBLISHED', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 30 DAY), expires_at
                    ),
                    version = version + 1
                WHERE id = ?
                """;
        this.databaseRepository.update(query, status, reason, status, status, listingId);
    }

    public void notifyListingStatus(long hostId, long listingId, String status, String reason) {
        this.databaseRepository.notify(
                hostId,
                "LISTING_STATUS",
                "Trạng thái tin đăng",
                status + " " + reason,
                "listing",
                listingId);
    }

    public void saveWishlist(long userId, long roomId) {
        this.databaseRepository.update(
                "INSERT IGNORE INTO wishlist_items(user_id, room_id) VALUES (?, ?)",
                userId,
                roomId);
    }

    public void deleteWishlist(long userId, long roomId) {
        this.databaseRepository.update(
                "DELETE FROM wishlist_items WHERE user_id = ? AND room_id = ?", userId, roomId);
    }

    public List<Map<String, Object>> findVisibleReviewsByRoomId(long roomId) {
        String query =
                """
                SELECT r.*, u.full_name AS reviewer_name
                FROM reviews r
                JOIN users u ON u.id = r.reviewer_id
                WHERE r.room_id = ? AND r.visible = TRUE
                ORDER BY r.id DESC
                """;
        return this.databaseRepository.all(query, roomId);
    }

    public Object parseJson(Object value) {
        return this.databaseRepository.decode(value);
    }

    public Object findAmenities() {
        return this.databaseRepository.readJsonResource("/student14-amenities.json");
    }
}
