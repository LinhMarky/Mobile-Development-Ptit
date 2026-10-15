package com.homely.student14.repository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class MediaRepository {
    private final DatabaseRepository databaseRepository;

    public Map<String, Object> findAttachedImageById(long imageId) {
        String query =
                """
                SELECT m.*, r.host_id, l.status AS listing_status, l.expires_at
                FROM media m
                JOIN rooms r ON m.resource_type = 'room' AND m.resource_id = r.id
                LEFT JOIN listings l ON l.room_id = r.id
                WHERE m.id = ? AND m.status = 'ATTACHED'
                """;
        return this.databaseRepository.one(query, imageId);
    }

    public void markAttachedRoomImagesDeleted(long roomId) {
        String query =
                """
                UPDATE media
                SET status = 'DELETED'
                WHERE resource_type = 'room' AND resource_id = ? AND status = 'ATTACHED'
                """;
        this.databaseRepository.update(query, roomId);
    }

    public void markRoomImagesDeleted(long roomId) {
        String query =
                """
                UPDATE media
                SET status = 'DELETED'
                WHERE resource_type = 'room' AND resource_id = ?
                """;
        this.databaseRepository.update(query, roomId);
    }

    public long createRoomImage(
            long uploaderId,
            long roomId,
            long fileSizeBytes,
            String storageKey,
            int width,
            int height) {
        String query =
                """
                INSERT INTO media (
                    uploader_id, purpose, resource_type, resource_id,
                    content_type, file_size_bytes, storage_key,
                    original_filename, width, height, status
                )
                VALUES (
                    ?, 'ROOM_IMAGE', 'room', ?, 'image/jpeg',
                    ?, ?, 'room.jpg', ?, ?, 'ATTACHED'
                )
                """;
        return this.databaseRepository.insert(
                query, uploaderId, roomId, fileSizeBytes, storageKey, width, height);
    }
}
