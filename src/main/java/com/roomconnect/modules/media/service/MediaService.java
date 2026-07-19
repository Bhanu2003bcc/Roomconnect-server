package com.roomconnect.modules.media.service;

import com.roomconnect.modules.media.entity.ListingMedia;
import com.roomconnect.modules.media.repository.ListingMediaRepository;
import com.roomconnect.shared.exception.ForbiddenException;
import com.roomconnect.shared.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final ListingMediaRepository mediaRepository;
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucket;

    @Value("${cloudflare.r2.public-url:}")
    private String publicUrl;

    @Value("${cloudflare.r2.endpoint:}")
    private String endpoint;

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOriginsRaw;

    @Value("${cloudflare.r2.initialize-on-startup:false}")
    private boolean initializeOnStartup;

    @PostConstruct
    public void init() {
        if (!initializeOnStartup) {
            log.info("S3/R2 bucket initialization on startup is disabled.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                // Wait briefly to allow application port binding to succeed first
                Thread.sleep(1000);
                log.info("Checking if S3/R2 bucket '{}' exists...", bucket);
                try {
                    s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
                    log.info("S3/R2 bucket '{}' exists.", bucket);
                } catch (NoSuchBucketException e) {
                    log.info("S3/R2 bucket '{}' does not exist. Creating it...", bucket);
                    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                    log.info("S3/R2 bucket '{}' created successfully.", bucket);
                }

                // Set CORS configuration
                try {
                    log.info("Configuring CORS policy for S3/R2 bucket '{}'...", bucket);
                    List<String> allowedOrigins = java.util.Arrays.stream(allowedOriginsRaw.split(","))
                            .map(String::trim)
                            .toList();

                    CORSRule rule = CORSRule.builder()
                            .allowedOrigins(allowedOrigins)
                            .allowedMethods(List.of("GET", "PUT", "POST", "DELETE", "HEAD", "OPTIONS"))
                            .allowedHeaders(List.of("content-type", "authorization", "x-requested-with", "accept", "origin"))
                            .maxAgeSeconds(3600)
                            .build();

                    s3Client.putBucketCors(PutBucketCorsRequest.builder()
                            .bucket(bucket)
                            .corsConfiguration(CORSConfiguration.builder()
                                    .corsRules(List.of(rule))
                                    .build())
                            .build());
                    log.info("CORS policy configured successfully for bucket '{}'.", bucket);
                } catch (Exception corsEx) {
                    log.warn("Could not configure CORS for S3/R2 bucket '{}': {}", bucket, corsEx.getMessage());
                }

                // Set Public Read bucket policy
                try {
                    log.info("Configuring public read bucket policy for S3/R2 bucket '{}'...", bucket);
                    String policyJson = """
                    {
                      "Version": "2012-10-17",
                      "Statement": [
                        {
                          "Sid": "PublicRead",
                          "Effect": "Allow",
                          "Principal": "*",
                          "Action": ["s3:GetObject"],
                          "Resource": ["arn:aws:s3:::%s/*"]
                        }
                      ]
                    }
                    """.formatted(bucket);
                    s3Client.putBucketPolicy(software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest.builder()
                            .bucket(bucket)
                            .policy(policyJson)
                            .build());
                    log.info("Public read bucket policy configured successfully for bucket '{}'.", bucket);
                } catch (Exception policyEx) {
                    log.warn("Could not set public read bucket policy for S3/R2 bucket '{}': {}", bucket, policyEx.getMessage());
                }

            } catch (Exception e) {
                log.warn("Could not verify/create/configure CORS for S3/R2 bucket '{}': {}", bucket, e.getMessage());
            }
        });
    }

    private static final int MAX_IMAGES_PER_LISTING = 10;
    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(15);

    /**
     * Generate a pre-signed PUT URL for a new image upload.
     * Creates a PENDING record; client calls confirmUpload() after PUT.
     */
    @Transactional
    public PresignResult generateUploadUrl(UUID listingId, UUID requesterId,
                                           String mimeType, long sizeBytes) {
        int count = mediaRepository.countByListingId(listingId);
        if (count >= MAX_IMAGES_PER_LISTING) {
            throw new ForbiddenException("Maximum " + MAX_IMAGES_PER_LISTING + " images per listing");
        }

        String fileKey = "listings/" + listingId + "/" + UUID.randomUUID() + resolveExtension(mimeType);

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .contentType(mimeType)
                .contentLength(sizeBytes)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(PRESIGN_DURATION)
                        .putObjectRequest(putReq)
                        .build());

        ListingMedia media = ListingMedia.builder()
                .listingId(listingId)
                .cdnUrl(fileKey)          // stored as key; resolved to URL on read
                .type("photo")
                .sortOrder(count)
                .processingStatus("pending")
                .build();
        mediaRepository.save(media);

        return new PresignResult(media.getId(), fileKey, presigned.url().toString());
    }

    /**
     * Called by the client after a successful PUT to R2.
     * Marks the media as done and sets the thumbnail key.
     */
    @Transactional
    public void confirmUpload(UUID mediaId, UUID listingId) {
        ListingMedia media = mediaRepository.findById(mediaId)
                .filter(m -> m.getListingId().equals(listingId))
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + mediaId));

        media.setProcessingStatus("done");
        media.setThumbnailUrl(generateThumbnailKey(media.getCdnUrl()));
        mediaRepository.save(media);
        log.info("Media {} confirmed for listing {}", mediaId, listingId);
    }

    @Transactional
    public void deleteMedia(UUID mediaId, UUID listingId) {
        ListingMedia media = mediaRepository.findById(mediaId)
                .filter(m -> m.getListingId().equals(listingId))
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + mediaId));

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket).key(media.getCdnUrl()).build());
        if (media.getThumbnailUrl() != null) {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket).key(media.getThumbnailUrl()).build());
        }
        mediaRepository.delete(media);
    }

    @Transactional(readOnly = true)
    public List<ListingMedia> getListingMedia(UUID listingId) {
        return mediaRepository.findByListingIdOrderBySortOrderAsc(listingId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String resolveExtension(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default           -> ".jpg";
        };
    }

    private String generateThumbnailKey(String key) {
        int dot = key.lastIndexOf('.');
        if (dot < 0) return key + "_thumb";
        return key.substring(0, dot) + "_thumb" + key.substring(dot);
    }

    public String getPublicUrl(String fileKey) {
        if (fileKey == null) return null;
        if (publicUrl != null && !publicUrl.isBlank()) {
            String base = publicUrl.endsWith("/") ? publicUrl : publicUrl + "/";
            return base + fileKey;
        }
        if (endpoint != null && !endpoint.isBlank()) {
            String base = endpoint.endsWith("/") ? endpoint : endpoint + "/";
            return base + bucket + "/" + fileKey;
        }
        return fileKey;
    }

    public record PresignResult(UUID mediaId, String fileKey, String uploadUrl) {}
}
