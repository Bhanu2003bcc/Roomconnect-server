package com.roomconnect.modules.users.repository;

import com.roomconnect.modules.users.entity.OwnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, UUID> {
}
