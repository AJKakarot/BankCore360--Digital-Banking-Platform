package com.bankcore360.controller;

import com.bankcore360.dto.NotificationResponse;
import com.bankcore360.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-app notifications for workflow updates, status changes, and EMI reminders")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get user notifications", description = "Returns all in-app notifications for the authenticated user")
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "";
        List<NotificationResponse> list = notificationService.getUserNotifications(username);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get count of unread notifications", description = "Returns total count of unread notifications for the authenticated user")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "";
        long count = notificationService.getUnreadCount(username);
        return ResponseEntity.ok(count);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read", description = "Updates notification read status to true")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "";
        boolean updated = notificationService.markAsRead(id, username);
        return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications for the authenticated user as read")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "";
        notificationService.markAllAsRead(username);
        return ResponseEntity.ok().build();
    }
}

