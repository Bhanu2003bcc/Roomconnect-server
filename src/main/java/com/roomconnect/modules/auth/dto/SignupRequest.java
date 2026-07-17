package com.roomconnect.modules.auth.dto;

import com.roomconnect.modules.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 15, message = "Phone must be between 10 and 15 digits")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Role is required")
    private Role role;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Consent for terms and DPDP compliance is required")
    private Boolean consent;
}
