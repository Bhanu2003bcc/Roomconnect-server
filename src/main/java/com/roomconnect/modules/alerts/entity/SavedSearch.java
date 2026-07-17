package com.roomconnect.modules.alerts.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomconnect.modules.search.dto.SearchRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "saved_searches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavedSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "visitor_id", nullable = false)
    private UUID visitorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters")
    private String filters;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "last_notified_at")
    private OffsetDateTime lastNotifiedAt;

    // Helper methods for DTO mapping
    public SearchRequest getFilters() {
        if (filters == null || filters.isBlank()) return null;
        try {
            return new ObjectMapper().readValue(filters, SearchRequest.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void setFilters(SearchRequest filtersRequest) {
        if (filtersRequest == null) {
            this.filters = null;
            return;
        }
        try {
            this.filters = new ObjectMapper().writeValueAsString(filtersRequest);
        } catch (Exception e) {
            this.filters = null;
        }
    }
}
