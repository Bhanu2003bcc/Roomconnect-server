package com.roomconnect.modules.stats.controller;

import com.roomconnect.modules.stats.dto.PlatformStatsResponse;
import com.roomconnect.modules.stats.service.PlatformStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class PlatformStatsController {

    private final PlatformStatsService platformStatsService;

    @GetMapping
    public ResponseEntity<PlatformStatsResponse> getPlatformStats() {
        return ResponseEntity.ok(platformStatsService.getStats());
    }
}
