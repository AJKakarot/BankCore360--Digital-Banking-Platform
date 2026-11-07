package com.bankcore360.service;

import com.bankcore360.dto.NotificationResponse;
import com.bankcore360.entity.Notification;
import com.bankcore360.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notifyUser(String recipientUsername, String title, String message, String relatedReference) {
        notifyUser(recipientUsername, "SYSTEM", title, message, relatedReference);
    }

    @Transactional
    public void notifyUser(String recipientUsername, String notificationType, String title, String message, String relatedReference) {
        notifyUserWithAction(recipientUsername, notificationType, title, message, relatedReference, null, null, null);
    }

    @Transactional
    public void notifyUserWithAction(String recipientUsername, String notificationType, String title,
                                     String message, String relatedReference,
                                     String actionUrl, String actionLabel, String actionPayload) {
        if (recipientUsername == null || recipientUsername.isBlank()) {
            return;
        }
        Notification notification = new Notification(
                recipientUsername.trim(),
                notificationType != null ? notificationType : "SYSTEM",
                title,
                message,
                relatedReference,
                actionUrl,
                actionLabel,
                actionPayload
        );
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        return notificationRepository.countByRecipientUsernameAndReadStatus(username, false);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(String username) {
        return notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public boolean markAsRead(Long notificationId, String username) {
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getRecipientUsername().equalsIgnoreCase(username))
                .map(n -> {
                    n.setReadStatus(true);
                    notificationRepository.save(n);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void markAllAsRead(String username) {
        List<Notification> unread = notificationRepository
                .findByRecipientUsernameAndReadStatusOrderByCreatedAtDesc(username, false);
        for (Notification n : unread) {
            n.setReadStatus(true);
        }
        notificationRepository.saveAll(unread);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getRecipientUsername(),
                n.getNotificationType(),
                n.getTitle(),
                n.getMessage(),
                n.isReadStatus(),
                n.getRelatedReference(),
                n.getActionUrl(),
                n.getActionLabel(),
                n.getActionPayload(),
                n.getCreatedAt()
        );
    }
}
