package com.roomconnect.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.roomconnect.models.OwnerProfile;

import java.util.UUID;

@Repository
public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, UUID> {
}
