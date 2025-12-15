package com.example.config;

import io.minio.MinioClient;
import io.minio.MakeBucketArgs;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.logging.Logger;

@ApplicationScoped
public class MinIOConfig {

    private static final Logger logger = Logger.getLogger(MinIOConfig.class.getName());

    private MinioClient client;

    @PostConstruct
    public void init() {
        String endpoint = System.getenv("MINIO_ENDPOINT");
        String accessKey = System.getenv("MINIO_ACCESS_KEY");
        String secretKey = System.getenv("MINIO_SECRET_KEY");

        if (endpoint == null || accessKey == null || secretKey == null) {
            throw new IllegalStateException("MinIO configuration is missing. Please set MINIO_ENDPOINT, MINIO_ACCESS_KEY, and MINIO_SECRET_KEY environment variables.");
        }

        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // Инициализируем bucket при старте
        initializeBucket();
    }

    @Produces
    @jakarta.enterprise.context.Dependent
    public MinioClient minioClient() {
        return client;
    }

    private void initializeBucket() {
        String bucketName = System.getenv("MINIO_BUCKET_NAME");
        if (bucketName == null) {
            logger.warning("MINIO_BUCKET_NAME not set, skipping bucket initialization");
            return;
        }

        try {
            boolean found = client.bucketExists(io.minio.BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!found) {
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                logger.info("Created MinIO bucket: " + bucketName);
            } else {
                logger.info("MinIO bucket already exists: " + bucketName);
            }
        } catch (Exception e) {
            logger.severe("Failed to initialize MinIO bucket: " + e.getMessage());
            // Не бросаем исключение, чтобы приложение могло запуститься
        }
    }
}
