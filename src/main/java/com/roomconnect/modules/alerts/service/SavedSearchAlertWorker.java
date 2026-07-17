package com.roomconnect.modules.alerts.service;

import com.roomconnect.modules.alerts.entity.SavedSearch;
import com.roomconnect.modules.alerts.repository.SavedSearchRepository;
import com.roomconnect.modules.auth.entity.User;
import com.roomconnect.modules.auth.repository.UserRepository;
import com.roomconnect.modules.notifications.service.NotificationService;
import com.roomconnect.modules.search.dto.SearchRequest;
import com.roomconnect.modules.search.service.SearchService;
import com.roomconnect.modules.listings.dto.ListingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavedSearchAlertWorker {

    private final SavedSearchRepository savedSearchRepository;
    private final SearchService searchService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /** Background job running every hour to match alerts */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void processAlerts() {
        log.info("Processing saved search alerts...");
        List<SavedSearch> searches = savedSearchRepository.findAll();

        for (SavedSearch ss : searches) {
            try {
                processSingleSearch(ss);
            } catch (Exception e) {
                log.error("Failed to process saved search " + ss.getId(), e);
            }
        }
    }

    private void processSingleSearch(SavedSearch ss) {
        OffsetDateTime since = ss.getLastNotifiedAt() != null ? ss.getLastNotifiedAt() : ss.getCreatedAt();
        SearchRequest req = ss.getFilters();
        if (req == null) return;

        // Force a large page size to catch all new listings in the radius
        req.setPage(0);
        req.setSize(100);

        SearchService.SearchResult results = searchService.search(req);
        List<ListingResponse> matches = results.items().stream()
                .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().isAfter(since))
                .toList();

        if (!matches.isEmpty()) {
            User user = userRepository.findById(ss.getVisitorId()).orElse(null);
            if (user != null && user.getEmail() != null) {
                String subject = "New listings matched your search criteria!";
                StringBuilder body = new StringBuilder("Hi, here are the new listings matching your filters:\n\n");
                for (ListingResponse match : matches) {
                    body.append("- ").append(match.getTitle())
                            .append(" in Noida (Rent: INR ").append(match.getRentAmount()).append(")\n");
                }
                body.append("\nClick here to view them in RoomConnect.");

                notificationService.sendEmail(user.getEmail(), subject, body.toString());
            }

            ss.setLastNotifiedAt(OffsetDateTime.now());
            savedSearchRepository.save(ss);
            log.info("Alert sent for saved search {} to visitor {}", ss.getId(), ss.getVisitorId());
        }
    }
}
