package com.bankcore360.repository;

import com.bankcore360.entity.UserSessionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRecordRepository extends JpaRepository<UserSessionRecord, Long> {

    List<UserSessionRecord> findByUserIdAndActiveTrueOrderByLastActivityAtDesc(Long userId);

    Optional<UserSessionRecord> findBySessionTokenHash(String sessionTokenHash);

    List<UserSessionRecord> findByUserId(Long userId);

    boolean existsByUserIdAndIpAddress(Long userId, String ipAddress);

    @Modifying
    @Query("UPDATE UserSessionRecord s SET s.active = false WHERE s.user.id = :userId")
    void revokeAllForUser(@Param("userId") Long userId);
}

