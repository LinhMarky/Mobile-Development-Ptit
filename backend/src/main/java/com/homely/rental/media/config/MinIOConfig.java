package com.homely.rental.media.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MinIOConfig {

    @Value("${homely.minio.endpoint}")
    private String endpoint;

    @Value("${homely.minio.access-key}")
    private String accessKey;

    @Value("${homely.minio.secret-key}")
    private String secretKey;

    @Value("${homely.minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // Auto-create bucket if not exists
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("Could not check/create MinIO bucket '{}': {}", bucket, e.getMessage());
        }

        return client;
    }
}
