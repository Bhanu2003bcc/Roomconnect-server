package com.roomconnect.modules.stats.service;

import com.roomconnect.modules.listings.entity.ListingStatus;
import com.roomconnect.modules.listings.repository.ListingRepository;
import com.roomconnect.modules.stats.dto.PlatformStatsResponse;
import com.roomconnect.modules.users.repository.OwnerProfileRepository;
import com.roomconnect.modules.users.repository.VisitorProfileRepository;
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
    private OwnerProfileRepository ownerProfileRepository;

    @Mock
    private VisitorProfileRepository visitorProfileRepository;

    @InjectMocks
    private PlatformStatsService platformStatsService;

    @Test
    public void testGetStats_shouldReturnRealDatabaseCounts() {
        // Arrange
        when(listingRepository.countByStatus(ListingStatus.AVAILABLE)).thenReturn(42L);
        when(ownerProfileRepository.count()).thenReturn(15L);
        when(visitorProfileRepository.count()).thenReturn(88L);

        // Act
        PlatformStatsResponse response = platformStatsService.getStats();

        // Assert
        assertEquals(42L, response.roomsListed());
        assertEquals(15L, response.verifiedOwners());
        assertEquals(88L, response.happyTenants());
        assertEquals(3, response.avgDaysToMove());

        // Verify caching: second call returns cached value without re-querying repositories
        PlatformStatsResponse cachedResponse = platformStatsService.getStats();
        assertEquals(response, cachedResponse);
        verify(listingRepository, times(1)).countByStatus(ListingStatus.AVAILABLE);
    }
}
