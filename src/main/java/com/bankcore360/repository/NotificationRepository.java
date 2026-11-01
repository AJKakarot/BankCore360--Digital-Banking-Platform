package com.bankcore360.repository;

import com.bankcore360.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername);

    List<Notification> findByRecipientUsernameAndReadStatusOrderByCreatedAtDesc(String recipientUsername, boolean readStatus);

    long countByRecipientUsernameAndReadStatus(String recipientUsername, boolean readStatus);
}

