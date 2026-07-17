package com.roomconnect.modules.chat.repository;

import com.roomconnect.modules.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByListingIdAndVisitorId(UUID listingId, UUID visitorId);
    List<Conversation> findByOwnerIdOrVisitorIdOrderByLastMessageAtDesc(UUID ownerId, UUID visitorId);
}
