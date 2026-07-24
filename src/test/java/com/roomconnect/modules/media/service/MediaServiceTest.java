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

import java.util.UUID;
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

    @Test
    public void testGenerateUploadUrl_whenBucketIsMissing_shouldThrowAppException() {
        // Arrange
        ReflectionTestUtils.setField(mediaService, "bucket", "");

        // Act & Assert
        com.roomconnect.shared.exception.AppException ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.roomconnect.shared.exception.AppException.class, () ->
                mediaService.generateUploadUrl(UUID.randomUUID(), UUID.randomUUID(), "image/png", 1024L)
        );
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("bucket configuration"));
    }

    @Test
    public void testGetThumbnailKey_whenLegacyThumbSuffixExists_shouldFallbackToCdnUrl() {
        // Arrange
        com.roomconnect.modules.media.entity.ListingMedia media = com.roomconnect.modules.media.entity.ListingMedia.builder()
                .cdnUrl("listings/123/image1.jpg")
                .thumbnailUrl("listings/123/image1_thumb.jpg")
                .build();

        // Act & Assert - getThumbnailKey() must sanitize and return cdnUrl without _thumb.jpg
        org.junit.jupiter.api.Assertions.assertEquals("listings/123/image1.jpg", media.getThumbnailKey());
    }

    @Test
    public void testGetPublicUrl_withPublicDevDomain_shouldFormDirectUrl() {
        // Arrange
        ReflectionTestUtils.setField(mediaService, "publicUrl", "https://pub-xxxx.r2.dev");

        // Act
        String result = mediaService.getPublicUrl("listings/123/image1.jpg");

        // Assert
        org.junit.jupiter.api.Assertions.assertEquals("https://pub-xxxx.r2.dev/listings/123/image1.jpg", result);
    }

    @Test
    public void testGetPublicUrl_withR2ApiEndpoint_shouldGeneratePresignedGetUrl() {
        // Arrange
        ReflectionTestUtils.setField(mediaService, "publicUrl", "");
        ReflectionTestUtils.setField(mediaService, "endpoint", "https://accountid.r2.cloudflarestorage.com");
        ReflectionTestUtils.setField(mediaService, "bucket", "rc-bucket");

        software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest presignedMock =
                mock(software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest.class);
        try {
            when(presignedMock.url()).thenReturn(new java.net.URI("https://accountid.r2.cloudflarestorage.com/rc-bucket/listings/123/image1.jpg?X-Amz-Signature=sig123").toURL());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(s3Presigner.presignGetObject(any(software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class)))
                .thenReturn(presignedMock);

        // Act
        String result = mediaService.getPublicUrl("listings/123/image1.jpg");

        // Assert
        org.junit.jupiter.api.Assertions.assertTrue(result.contains("X-Amz-Signature"));
    }
}
