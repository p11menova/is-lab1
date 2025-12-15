package com.example.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.InputStream;
import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class MinIOService {

    private static final Logger logger = Logger.getLogger(MinIOService.class.getName());

    @Inject
    private MinioClient minioClient;

    private final String bucketName;

    public MinIOService() {
        this.bucketName = System.getenv("MINIO_BUCKET_NAME");
        if (this.bucketName == null) {
            throw new IllegalStateException("MINIO_BUCKET_NAME environment variable is not set");
        }
    }

    public String uploadFile(InputStream inputStream, String fileName, String contentType) throws Exception {
        try {
            String objectKey = UUID.randomUUID().toString() + "_" + fileName;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, inputStream.available(), -1)
                            .contentType(contentType)
                            .build()
            );

            logger.info("File uploaded to MinIO: " + objectKey);
            return objectKey;
        } catch (MinioException e) {
            logger.severe("Error uploading file to MinIO: " + e.getMessage());
            throw new Exception("Failed to upload file to MinIO: " + e.getMessage(), e);
        }
    }

    public InputStream downloadFile(String objectKey) throws Exception {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (MinioException e) {
            logger.severe("Error downloading file from MinIO: " + e.getMessage());
            throw new Exception("Failed to download file from MinIO: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String objectKey) throws Exception {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            logger.info("File deleted from MinIO: " + objectKey);
        } catch (MinioException e) {
            logger.severe("Error deleting file from MinIO: " + e.getMessage());
            throw new Exception("Failed to delete file from MinIO: " + e.getMessage(), e);
        }
    }
}
