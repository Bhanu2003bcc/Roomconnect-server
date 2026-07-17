package com.roomconnect.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpSendRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;
}
