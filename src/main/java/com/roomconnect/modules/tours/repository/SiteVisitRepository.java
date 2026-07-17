package com.roomconnect.modules.tours.repository;

import com.roomconnect.modules.tours.entity.SiteVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SiteVisitRepository extends JpaRepository<SiteVisit, UUID> {
    List<SiteVisit> findByVisitorIdOrderByRequestedTimeDesc(UUID visitorId);
    List<SiteVisit> findByOwnerIdOrderByRequestedTimeDesc(UUID ownerId);
}
