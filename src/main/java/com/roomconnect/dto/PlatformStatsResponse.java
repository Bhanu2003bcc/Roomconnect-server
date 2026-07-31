package com.roomconnect.dto;

public record PlatformStatsResponse(
        long roomsListed,
        long verifiedOwners,
        long happyTenants,
        int avgDaysToMove
) {}
