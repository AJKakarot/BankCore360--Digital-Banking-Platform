package com.bankcore360.repository;

import com.bankcore360.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByTokenFamily(String tokenFamily);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.tokenFamily = :family")
    void revokeAllInFamily(@Param("family") String family);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId")
    void revokeAllForUser(@Param("userId") Long userId);
}
