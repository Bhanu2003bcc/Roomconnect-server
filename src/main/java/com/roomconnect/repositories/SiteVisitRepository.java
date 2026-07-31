package com.roomconnect.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.roomconnect.models.SiteVisit;

import java.util.List;
import java.util.UUID;

@Repository
public interface SiteVisitRepository extends JpaRepository<SiteVisit, UUID> {
    List<SiteVisit> findByVisitorIdOrderByRequestedTimeDesc(UUID visitorId);
    List<SiteVisit> findByOwnerIdOrderByRequestedTimeDesc(UUID ownerId);
}
