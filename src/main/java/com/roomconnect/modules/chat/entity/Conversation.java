package com.roomconnect.modules.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"listing_id", "visitor_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "listing_id")
    private UUID listingId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "visitor_id", nullable = false)
    private UUID visitorId;

    @Column(name = "listing_title")
    private String listingTitle;

    @Column(name = "listing_address", columnDefinition = "TEXT")
    private String listingAddress;

    @Column(name = "listing_rent")
    private BigDecimal listingRent;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;
}
