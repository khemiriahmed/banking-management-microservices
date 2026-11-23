package com.ahmedkh.banking.transfer.entity;

import java.util.Set;

public enum TransferStatus {

    REQUESTED,
    VALIDATING,
    PROCESSING,
    COMPLETED,
    FAILED;

    private static final Set<TransferStatus> TERMINAL = Set.of(COMPLETED, FAILED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean canTransitionTo(TransferStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case REQUESTED -> target == VALIDATING || target == FAILED;
            case VALIDATING -> target == PROCESSING || target == FAILED;
            case PROCESSING -> target == COMPLETED || target == FAILED;
            case COMPLETED, FAILED -> false;
        };
    }
}
