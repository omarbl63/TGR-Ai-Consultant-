package com.pfe.adminagent.notification.repository;

import com.pfe.adminagent.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadIsFalse(UUID userId);

    @Transactional
    void deleteByUserId(UUID userId);
}
