package com.roomconnect.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.roomconnect.models.VisitorProfile;

import java.util.UUID;

@Repository
public interface VisitorProfileRepository extends JpaRepository<VisitorProfile, UUID> {
}
