package com.ahmedkh.banking.account.dto;

import com.ahmedkh.banking.account.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(@NotNull AccountStatus status) {
}
