package com.roomconnect.modules.alerts.service;

import com.roomconnect.modules.alerts.entity.SavedSearch;
import com.roomconnect.modules.alerts.repository.SavedSearchRepository;
import com.roomconnect.modules.search.dto.SearchRequest;
import com.roomconnect.shared.exception.ForbiddenException;
import com.roomconnect.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final SavedSearchRepository savedSearchRepository;

    @Transactional
    public SavedSearch saveSearch(UUID visitorId, SearchRequest filters) {
        SavedSearch ss = SavedSearch.builder()
                .visitorId(visitorId)
                .build();
        ss.setFilters(filters);
        SavedSearch saved = savedSearchRepository.save(ss);
        log.info("Saved search created for visitor {}", visitorId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SavedSearch> getVisitorSearches(UUID visitorId) {
        return savedSearchRepository.findByVisitorIdOrderByCreatedAtDesc(visitorId);
    }

    @Transactional
    public void deleteSearch(UUID visitorId, UUID searchId) {
        SavedSearch ss = savedSearchRepository.findById(searchId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved search not found: " + searchId));

        if (!ss.getVisitorId().equals(visitorId)) {
            throw new ForbiddenException("You do not own this saved search");
        }

        savedSearchRepository.delete(ss);
        log.info("Deleted saved search {}", searchId);
    }
}
