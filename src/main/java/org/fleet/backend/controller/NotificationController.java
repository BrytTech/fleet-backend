package org.fleet.backend.controller;

import org.fleet.backend.entity.Notification;
import org.fleet.backend.entity.User;
import org.fleet.backend.service.NotificationService;
import org.fleet.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RIDER', 'ADMIN')")
    public ResponseEntity<List<Notification>> getMyNotifications() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findUserByEmail(email);
        return ResponseEntity.ok(notificationService.getUserNotifications(user.getId()));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RIDER', 'ADMIN')")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok("Marked as read");
    }
}