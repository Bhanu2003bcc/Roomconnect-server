package com.roomconnect.modules.admin.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    // Helper methods for DTO mapping
    public Map<String, Object> getMetadataMap() {
        if (metadata == null || metadata.isBlank()) return null;
        try {
            return new ObjectMapper().readValue(metadata, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    public void setMetadataMap(Map<String, Object> metaMap) {
        if (metaMap == null) {
            this.metadata = null;
            return;
        }
        try {
            this.metadata = new ObjectMapper().writeValueAsString(metaMap);
        } catch (Exception e) {
            this.metadata = null;
        }
    }
}
