package com.bankcore360.event.payload;

import java.time.LocalDateTime;

public record NotificationEventPayload(
        String recipientUsername,
        String notificationType,
        String title,
        String message,
        String relatedReference,
        LocalDateTime createdAt
) {}

