package com.roomconnect.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.roomconnect.models.Message;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByConversationId(UUID conversationId, Pageable pageable);
    List<Message> findByConversationIdOrderBySentAtAsc(UUID conversationId);
    int deleteBySentAtBefore(OffsetDateTime cutoff);
}
