package com.roomconnect.modules.favorites.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "favorites",
        indexes = @Index(name = "idx_fav_visitor", columnList = "visitor_id"))
@IdClass(Favorite.FavoriteId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Favorite {

    @Id
    @Column(name = "visitor_id", nullable = false)
    private UUID visitorId;

    @Id
    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /** Composite PK class */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    @EqualsAndHashCode
    public static class FavoriteId implements Serializable {
        private UUID visitorId;
        private UUID listingId;
    }
}
