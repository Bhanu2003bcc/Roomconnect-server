package com.roomconnect.modules.users.dto;

import com.roomconnect.modules.auth.entity.Role;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {
    private UUID id;
    private String phone;
    private String email;
    private Role role;
    private String fullName;

    // Owner specific
    private String address;
    private String landmark;

    // Visitor specific
    private String homeAddress;
    private String profession;
    
    private Integer cityId;
}
