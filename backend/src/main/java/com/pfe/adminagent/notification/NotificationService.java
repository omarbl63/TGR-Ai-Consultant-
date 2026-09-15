package com.pfe.adminagent.notification;

import com.pfe.adminagent.common.exception.ResourceNotFoundException;
import com.pfe.adminagent.notification.domain.Notification;
import com.pfe.adminagent.notification.domain.NotificationType;
import com.pfe.adminagent.notification.dto.NotificationDto;
import com.pfe.adminagent.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notify(UUID userId, NotificationType type, String title, String message, UUID requestId) {
        notificationRepository.save(new Notification(userId, type, title, message, requestId));
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> listForUser(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadIsFalse(userId);
    }

    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        Notification n = notificationRepository.findById(notificationId)
                .filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));
        n.markRead();
        notificationRepository.save(n);
    }

    @Transactional
    public void delete(UUID notificationId, UUID userId) {
        Notification n = notificationRepository.findById(notificationId)
                .filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));
        notificationRepository.delete(n);
    }

    @Transactional
    public void deleteAll(UUID userId) {
        notificationRepository.deleteByUserId(userId);
    }
}
