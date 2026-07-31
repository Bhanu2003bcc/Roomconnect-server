package com.roomconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpSendRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;
}
