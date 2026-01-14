package com.app.services;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MinioServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private MinioService minioService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(minioService, "bucket", "test-bucket");
        minioService.setMinioClient(minioClient);
    }

    @Test
    void testUploadFile_Success() throws Exception {
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        when(multipartFile.getSize()).thenReturn(4L);
        when(multipartFile.getContentType()).thenReturn("image/jpeg");

        String fileName = minioService.uploadFile(multipartFile);

        assertNotNull(fileName);
        assertTrue(fileName.contains("test.jpg"));
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    void testGetFileUrl_Success() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://signed-url.com");

        String url = minioService.getFileUrl("test.jpg");

        assertEquals("http://signed-url.com", url);
    }

    @Test
    void testUploadFile_Failure() throws Exception {
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getInputStream()).thenThrow(new RuntimeException("IO Error"));

        assertThrows(RuntimeException.class, () -> minioService.uploadFile(multipartFile));
    }
}
