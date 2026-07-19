package com.roomconnect.modules.media.service;

import com.roomconnect.modules.media.repository.ListingMediaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MediaServiceTest {

    @Mock
    private ListingMediaRepository mediaRepository;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private MediaService mediaService;

    @Test
    public void testInit_whenInitializeOnStartupIsFalse_shouldNotInteractWithS3() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(mediaService, "initializeOnStartup", false);
        ReflectionTestUtils.setField(mediaService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(mediaService, "allowedOriginsRaw", "http://localhost:4200");

        // Act
        mediaService.init();

        // Wait brief moment to verify no background interaction starts
        Thread.sleep(1500);

        // Assert
        verifyNoInteractions(s3Client);
    }

    @Test
    public void testInit_whenInitializeOnStartupIsTrue_shouldInteractWithS3() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(mediaService, "initializeOnStartup", true);
        ReflectionTestUtils.setField(mediaService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(mediaService, "allowedOriginsRaw", "http://localhost:4200");

        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenReturn(HeadBucketResponse.builder().build());

        // Act
        mediaService.init();

        // Wait brief moment since init has a Thread.sleep(1000) inside runAsync
        Thread.sleep(1500);

        // Assert
        verify(s3Client, atLeastOnce()).headBucket(any(HeadBucketRequest.class));
    }
}
