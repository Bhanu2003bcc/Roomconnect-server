package com.roomconnect.modules.auth.dto;

import com.roomconnect.modules.auth.entity.Role;
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
