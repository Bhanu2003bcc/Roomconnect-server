package com.roomconnect.modules.stats.service;

import com.roomconnect.modules.auth.entity.Role;
import com.roomconnect.modules.auth.repository.UserRepository;
import com.roomconnect.modules.listings.entity.ListingStatus;
import com.roomconnect.modules.listings.repository.ListingRepository;
import com.roomconnect.modules.stats.dto.PlatformStatsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlatformStatsServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PlatformStatsService platformStatsService;

    @Test
    public void testGetStats_shouldReturnRealDatabaseCounts() {
        // Arrange — raw DB counts, no baseline inflation
        when(listingRepository.countByStatus(ListingStatus.AVAILABLE)).thenReturn(42L);
        when(userRepository.countByRoleAndPhoneVerifiedTrue(Role.owner)).thenReturn(15L);
        when(userRepository.countByRoleAndPhoneVerifiedTrue(Role.visitor)).thenReturn(88L);

        // Act
        PlatformStatsResponse response = platformStatsService.getStats();

        // Assert — exactly what the DB returns, no offsets added
        assertEquals(42L, response.roomsListed());
        assertEquals(15L, response.verifiedOwners());
        assertEquals(88L, response.happyTenants());
        assertEquals(3, response.avgDaysToMove());

        // Verify caching: second call should NOT re-query the repositories
        PlatformStatsResponse cachedResponse = platformStatsService.getStats();
        assertEquals(response, cachedResponse);
        verify(listingRepository, times(1)).countByStatus(ListingStatus.AVAILABLE);
        verify(userRepository, times(1)).countByRoleAndPhoneVerifiedTrue(Role.owner);
        verify(userRepository, times(1)).countByRoleAndPhoneVerifiedTrue(Role.visitor);
    }
}
