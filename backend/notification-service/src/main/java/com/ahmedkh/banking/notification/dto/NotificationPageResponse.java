package com.ahmedkh.banking.notification.dto;

import java.util.List;

public record NotificationPageResponse(
        List<NotificationResponse> content,
        int page,
        int size,
        long totalElements,
        boolean last
) {
}
