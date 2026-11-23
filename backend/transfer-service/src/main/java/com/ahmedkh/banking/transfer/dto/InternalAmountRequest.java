package com.ahmedkh.banking.transfer.dto;

import java.math.BigDecimal;

public record InternalAmountRequest(BigDecimal amount, String currency) {
}
