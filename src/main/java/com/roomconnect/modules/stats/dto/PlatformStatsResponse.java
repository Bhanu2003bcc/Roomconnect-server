package com.roomconnect.modules.stats.dto;

public record PlatformStatsResponse(
        long roomsListed,
        long verifiedOwners,
        long happyTenants,
        int avgDaysToMove
) {}
