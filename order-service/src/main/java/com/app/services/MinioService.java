package com.app.services;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    private MinioClient minioClient;

    // For Testing
    public void setMinioClient(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @PostConstruct
    public void init() {
        if (this.minioClient == null) {
            try {
                minioClient = MinioClient.builder()
                        .endpoint(endpoint)
                        .credentials(accessKey, secretKey)
                        .build();
            } catch (Exception e) {
                System.err.println(">>> Error initializing MinIO: " + e.getMessage());
                return;
            }
        }
        ensureBucketExists();
    }

    public void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                System.out.println(">>> Created MinIO bucket: " + bucket);
            }
        } catch (Exception e) {
            System.err.println(">>> Error ensuring bucket exists: " + e.getMessage());
        }
    }

    public String uploadFile(MultipartFile file) {
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Error uploading file to MinIO: " + e.getMessage());
        }
    }

    public String uploadFromUrl(String fileUrl, String preferredName) {
        try {
            URL url = new URL(fileUrl);
            URLConnection connection = url.openConnection();
            InputStream is = connection.getInputStream();
            byte[] bytes = is.readAllBytes();
            String contentType = connection.getContentType();

            String fileName = System.currentTimeMillis() + "_" + preferredName;
            return uploadBytes(bytes, fileName, contentType);
        } catch (Exception e) {
            throw new RuntimeException("Error uploading from URL to MinIO: " + e.getMessage());
        }
    }

    public String uploadBytes(byte[] bytes, String fileName, String contentType) {
        try {
            InputStream is = new ByteArrayInputStream(bytes);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .stream(is, bytes.length, -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build());
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Error uploading bytes to MinIO: " + e.getMessage());
        }
    }

    public String getFileUrl(String fileName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(fileName)
                            .expiry(2, TimeUnit.HOURS)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Error generating MinIO URL: " + e.getMessage());
        }
    }
}
