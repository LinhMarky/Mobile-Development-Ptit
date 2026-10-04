package com.homely.rental.media.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * S3/MinIO storage service for file upload/download/delete operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final MinioClient minioClient;

    @Value("${homely.minio.bucket}")
    private String bucket;

    @Value("${homely.minio.presigned-url-expiry-seconds:3600}")
    private int presignedUrlExpiry;

    /**
     * Upload a file to MinIO.
     *
     * @param key         storage key (UUID-based path)
     * @param file        uploaded file
     * @param contentType MIME type
     */
    public void upload(String key, MultipartFile file, String contentType) {
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(is, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
            log.debug("Uploaded object: {}/{}", bucket, key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload to MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a pre-signed URL for read access.
     */
    public String getPresignedUrl(String key) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .method(Method.GET)
                    .expiry(presignedUrlExpiry, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Delete a file from MinIO.
     */
    public InputStream open(String key) {
        try { return minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build()); }
        catch (Exception e) { throw new IllegalStateException("Media storage is unavailable", e); }
    }

    public void delete(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
            log.debug("Deleted object: {}/{}", bucket, key);
        } catch (Exception e) {
            log.warn("Failed to delete object {}: {}", key, e.getMessage());
        }
    }
}
