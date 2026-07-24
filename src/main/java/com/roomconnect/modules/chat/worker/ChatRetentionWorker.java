package com.roomconnect.modules.chat.worker;

import com.roomconnect.modules.chat.repository.ConversationRepository;
import com.roomconnect.modules.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRetentionWorker {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    /**
     * Daily cleanup job at 03:00 AM IST/UTC.
     * Purges chat messages and inactive conversations older than 90 days.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredChatLogs() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(90);
        log.info("Starting 90-day chat retention cleanup for entries older than {}", cutoff);

        int deletedMessages = messageRepository.deleteBySentAtBefore(cutoff);
        int deletedConversations = conversationRepository.deleteByLastMessageAtBeforeOrCreatedAtBefore(cutoff, cutoff);

        log.info("Chat retention cleanup finished. Deleted {} messages and {} conversations older than 90 days.",
                deletedMessages, deletedConversations);
    }
}
