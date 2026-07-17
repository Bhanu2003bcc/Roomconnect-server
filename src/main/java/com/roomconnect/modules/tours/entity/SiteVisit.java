package com.roomconnect.modules.tours.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "site_visits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SiteVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "visitor_id", nullable = false)
    private UUID visitorId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "requested_time", nullable = false)
    private OffsetDateTime requestedTime;

    @Builder.Default
    @Convert(converter = TourStatusConverter.class)
    @Column(nullable = false, length = 20)
    private TourStatus status = TourStatus.REQUESTED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public enum TourStatus {
        REQUESTED("requested"),
        CONFIRMED("confirmed"),
        DECLINED("declined"),
        COMPLETED("completed"),
        CANCELLED("cancelled");

        private final String value;

        TourStatus(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static TourStatus fromValue(String value) {
            for (TourStatus s : TourStatus.values()) {
                if (s.value.equalsIgnoreCase(value)) {
                    return s;
                }
            }
            throw new IllegalArgumentException("Unknown tour status: " + value);
        }
    }

    @Converter
    public static class TourStatusConverter implements AttributeConverter<TourStatus, String> {
        @Override
        public String convertToDatabaseColumn(TourStatus attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public TourStatus convertToEntityAttribute(String dbData) {
            return dbData != null ? TourStatus.fromValue(dbData) : null;
        }
    }
}
