package com.bankcore360.repository;

import com.bankcore360.entity.OutboxEvent;
import com.bankcore360.entity.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEventsForPublishing(@Param("status") OutboxEventStatus status,
                                                    @Param("maxRetries") int maxRetries,
                                                    Pageable pageable);
}

