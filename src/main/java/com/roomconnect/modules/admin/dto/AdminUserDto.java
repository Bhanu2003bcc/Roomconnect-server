package com.roomconnect.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDto {
    private UUID id;
    private String phone;
    private String email;
    private String role;
    private String status;
    private String fullName;
    private OffsetDateTime createdAt;
}
