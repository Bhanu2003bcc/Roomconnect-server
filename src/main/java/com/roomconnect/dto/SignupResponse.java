package com.roomconnect.dto;

import com.roomconnect.models.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class SignupResponse {
    private UUID userId;
    private String phone;
    private Role role;
    private String message;
}
