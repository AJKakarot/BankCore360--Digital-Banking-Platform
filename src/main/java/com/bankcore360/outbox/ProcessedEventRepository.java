package com.bankcore360.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventIdAndConsumerGroup(String eventId, String consumerGroup);

    Optional<ProcessedEvent> findByEventIdAndConsumerGroup(String eventId, String consumerGroup);

    List<ProcessedEvent> findAllByOrderByProcessedAtDesc(Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM ProcessedEvent p WHERE p.processedAt < :cutoff")
    int deleteEventsOlderThan(@org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff);
}

