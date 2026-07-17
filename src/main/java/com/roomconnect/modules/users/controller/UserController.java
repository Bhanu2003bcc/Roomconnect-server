package com.roomconnect.modules.users.controller;

import com.roomconnect.modules.users.dto.UserProfileDto;
import com.roomconnect.modules.users.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMe(@AuthenticationPrincipal UUID currentUserId) {
        UserProfileDto profile = userService.getUserProfile(currentUserId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateMe(@AuthenticationPrincipal UUID currentUserId, @RequestBody UserProfileDto dto) {
        UserProfileDto updatedProfile = userService.updateUserProfile(currentUserId, dto);
        return ResponseEntity.ok(updatedProfile);
    }
}
