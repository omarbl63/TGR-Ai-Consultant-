package com.pfe.adminagent.notification.dto;

import com.pfe.adminagent.notification.domain.Notification;
import com.pfe.adminagent.notification.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        NotificationType type,
        String title,
        String message,
        UUID requestId,
        boolean read,
        Instant createdAt
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getRequestId(), n.isRead(), n.getCreatedAt());
    }
}
