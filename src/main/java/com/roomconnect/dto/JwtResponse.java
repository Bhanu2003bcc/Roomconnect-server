package com.roomconnect.dto;

import com.roomconnect.models.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private UUID userId;
    private Role role;
    private String phone;
}
