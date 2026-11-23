package com.ahmedkh.banking.audit.dto;

import java.util.List;

public record AuditPageResponse(
        List<AuditEventResponse> content,
        int page,
        int size,
        long totalElements,
        boolean last
) {
}
