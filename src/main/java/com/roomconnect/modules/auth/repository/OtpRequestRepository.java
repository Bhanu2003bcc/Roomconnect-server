package com.roomconnect.modules.auth.repository;

import com.roomconnect.modules.auth.entity.OtpRequest;
import com.roomconnect.modules.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRequestRepository extends JpaRepository<OtpRequest, UUID> {
    List<OtpRequest> findByUserAndPurposeAndConsumedAtIsNullAndExpiresAtAfter(
            User user, String purpose, OffsetDateTime time);
}
