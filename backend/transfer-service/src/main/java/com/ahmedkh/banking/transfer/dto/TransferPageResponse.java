package com.ahmedkh.banking.transfer.dto;

import java.util.List;

public record TransferPageResponse(
        List<TransferResponse> content,
        int page,
        int size,
        long totalElements,
        boolean last
) {
}
