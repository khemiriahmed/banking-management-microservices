package com.ahmedkh.banking.notification.controller;

import com.ahmedkh.banking.notification.dto.NotificationPageResponse;
import com.ahmedkh.banking.notification.dto.NotificationResponse;
import com.ahmedkh.banking.notification.security.AuthenticatedUser;
import com.ahmedkh.banking.notification.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public NotificationPageResponse list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(AuthenticatedUser.getAuthUserId(authentication), page, size);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(Authentication authentication, @PathVariable UUID id) {
        return service.markRead(id, AuthenticatedUser.getAuthUserId(authentication));
    }
}
