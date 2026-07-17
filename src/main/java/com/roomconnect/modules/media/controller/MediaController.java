package com.roomconnect.modules.media.controller;

import com.roomconnect.modules.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings/{listingId}/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    /**
     * POST /api/listings/{listingId}/media/presign
     * Owner requests a pre-signed PUT URL to upload directly to R2.
     */
    @PostMapping("/presign")
    @PreAuthorize("hasRole('owner')")
    public ResponseEntity<Map<String, Object>> presign(
            @PathVariable UUID listingId,
            @AuthenticationPrincipal UUID ownerId,
            @RequestParam String mimeType,
            @RequestParam long sizeBytes) {
        MediaService.PresignResult result = mediaService.generateUploadUrl(listingId, ownerId, mimeType, sizeBytes);
        return ResponseEntity.ok(Map.of(
                "mediaId",   result.mediaId(),
                "fileKey",   result.fileKey(),
                "uploadUrl", result.uploadUrl()
        ));
    }

    /**
     * POST /api/listings/{listingId}/media/{mediaId}/confirm
     * Called by client after successful PUT to R2.
     */
    @PostMapping("/{mediaId}/confirm")
    @PreAuthorize("hasRole('owner')")
    public ResponseEntity<Map<String, String>> confirm(
            @PathVariable UUID listingId,
            @PathVariable UUID mediaId) {
        mediaService.confirmUpload(mediaId, listingId);
        return ResponseEntity.ok(Map.of("message", "Upload confirmed, thumbnail queued"));
    }

    /**
     * DELETE /api/listings/{listingId}/media/{mediaId}
     */
    @DeleteMapping("/{mediaId}")
    @PreAuthorize("hasRole('owner')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID listingId,
            @PathVariable UUID mediaId) {
        mediaService.deleteMedia(mediaId, listingId);
        return ResponseEntity.noContent().build();
    }
}
