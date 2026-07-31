package com.roomconnect.repositories;

import com.roomconnect.models.User;
import com.roomconnect.models.OtpRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRequestRepository extends JpaRepository<OtpRequest, UUID> {

    Optional<OtpRequest> findFirstByUserAndPurposeAndConsumedAtIsNullAndExpiresAtAfterOrderByIdDesc(
        User user, 
        String purpose, 
        OffsetDateTime standardTime
    );
}
