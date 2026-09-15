package com.pfe.adminagent.notification.web;

import com.pfe.adminagent.notification.NotificationService;
import com.pfe.adminagent.notification.dto.NotificationDto;
import com.pfe.adminagent.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "In-app notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List the current user's notifications")
    public List<NotificationDto> list(@AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.listForUser(principal.getId());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count unread notifications")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return Map.of("count", notificationService.unreadCount(principal.getId()));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markRead(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Delete all of the current user's notifications")
    public ResponseEntity<Void> deleteAll(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.deleteAll(principal.getId());
        return ResponseEntity.noContent().build();
    }
}
