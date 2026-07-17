package com.roomconnect.modules.media.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "listing_media", indexes = {
        @Index(name = "idx_media_listing", columnList = "listing_id"),
        @Index(name = "idx_media_sort",    columnList = "listing_id, sort_order")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ListingMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    /** "photo" or "video" */
    @Column(name = "type", length = 10)
    private String type;

    /** Full CDN / R2 URL of the original file */
    @Column(name = "cdn_url", nullable = false, columnDefinition = "TEXT")
    private String cdnUrl;

    /** CDN URL of the generated thumbnail */
    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "processing_status", nullable = false, length = 20)
    private String processingStatus = "pending"; // pending | done | failed

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ── convenience aliases used by services ─────────────────────────────────

    /** Alias so existing code using getFileKey() still compiles */
    public String getFileKey()      { return cdnUrl; }
    public void   setFileKey(String v) { this.cdnUrl = v; }

    public String getThumbnailKey()      { return thumbnailUrl; }
    public void   setThumbnailKey(String v) { this.thumbnailUrl = v; }

    public boolean isReady() { return "done".equals(processingStatus); }
}
