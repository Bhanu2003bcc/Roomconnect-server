package com.roomconnect.modules.auth.repository;

import com.roomconnect.modules.auth.entity.Role;
import com.roomconnect.modules.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    /** Counts users with a given role who have completed phone verification. */
    long countByRoleAndPhoneVerifiedTrue(Role role);
}
