package com.bankcore360.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    List<OutboxEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<OutboxEvent> findByEventId(String eventId);

    long countByStatus(OutboxStatus status);
}

