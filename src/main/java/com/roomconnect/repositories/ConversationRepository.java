package com.roomconnect.repositories;

import com.roomconnect.models.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByListingIdAndVisitorId(UUID listingId, UUID visitorId);
    List<Conversation> findByOwnerIdOrVisitorIdOrderByLastMessageAtDesc(UUID ownerId, UUID visitorId);
    int deleteByLastMessageAtBeforeOrCreatedAtBefore(OffsetDateTime lastMessageCutoff, OffsetDateTime createdCutoff);
}
