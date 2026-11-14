package com.bankcore360.service;

import com.bankcore360.outbox.ProcessedEventRepository;
import com.bankcore360.repository.UserSessionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogRetentionServiceTest {

    @Mock
    private UserSessionRecordRepository userSessionRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private AuditLogRetentionService retentionService;

    @BeforeEach
    void setUp() {
        retentionService = new AuditLogRetentionService(userSessionRepository, processedEventRepository);
        ReflectionTestUtils.setField(retentionService, "sessionRetentionDays", 180);
        ReflectionTestUtils.setField(retentionService, "eventRetentionDays", 90);
    }

    @Test
    @DisplayName("Should purge inactive sessions and processed events beyond retention cutoff")
    void shouldPurgeExpiredOperationalData() {
        when(userSessionRepository.deleteInactiveSessionsOlderThan(any(LocalDateTime.class)))
                .thenReturn(15);
        when(processedEventRepository.deleteEventsOlderThan(any(LocalDateTime.class)))
                .thenReturn(42);

        AuditLogRetentionService.RetentionPurgeResult result = retentionService.purgeExpiredOperationalData();

        assertThat(result.prunedSessions()).isEqualTo(15);
        assertThat(result.prunedEvents()).isEqualTo(42);
        assertThat(result.sessionCutoff()).isBefore(LocalDateTime.now().minusDays(179));
        assertThat(result.eventCutoff()).isBefore(LocalDateTime.now().minusDays(89));

        verify(userSessionRepository, times(1)).deleteInactiveSessionsOlderThan(any(LocalDateTime.class));
        verify(processedEventRepository, times(1)).deleteEventsOlderThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should handle absent processed event repository gracefully")
    void shouldHandleNullProcessedEventRepository() {
        AuditLogRetentionService standaloneService = new AuditLogRetentionService(userSessionRepository, null);
        ReflectionTestUtils.setField(standaloneService, "sessionRetentionDays", 180);
        ReflectionTestUtils.setField(standaloneService, "eventRetentionDays", 90);

        when(userSessionRepository.deleteInactiveSessionsOlderThan(any(LocalDateTime.class)))
                .thenReturn(7);

        AuditLogRetentionService.RetentionPurgeResult result = standaloneService.purgeExpiredOperationalData();

        assertThat(result.prunedSessions()).isEqualTo(7);
        assertThat(result.prunedEvents()).isEqualTo(0);
        verify(userSessionRepository, times(1)).deleteInactiveSessionsOlderThan(any(LocalDateTime.class));
    }
}
