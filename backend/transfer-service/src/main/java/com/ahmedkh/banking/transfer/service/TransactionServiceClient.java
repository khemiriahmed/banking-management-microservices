package com.ahmedkh.banking.transfer.service;

import com.ahmedkh.banking.transfer.entity.Transfer;

public interface TransactionServiceClient {

    void recordCompletedTransfer(Transfer transfer);

    void recordReversal(Transfer transfer);
}
