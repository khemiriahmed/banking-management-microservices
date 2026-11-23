package com.ahmedkh.banking.transfer.service;

import com.ahmedkh.banking.transfer.dto.InternalTransactionEntryRequest;
import com.ahmedkh.banking.transfer.dto.InternalTransactionRequest;
import com.ahmedkh.banking.transfer.entity.Transfer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

@Component
public class HttpTransactionServiceClient implements TransactionServiceClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final RestClient restClient;
    private final String baseUrl;
    private final String internalToken;

    public HttpTransactionServiceClient(
            @Qualifier("transactionRestClient") RestClient restClient,
            @Value("${app.transaction-service.base-url}") String baseUrl,
            @Value("${app.transaction-service.internal-token}") String internalToken
    ) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
        this.internalToken = internalToken;
    }

    @Override
    public void recordCompletedTransfer(Transfer transfer) {
        List<InternalTransactionEntryRequest> entries = List.of(
                new InternalTransactionEntryRequest(
                        transfer.getSourceAccountId(),
                        null,
                        "DEBIT",
                        transfer.getAmount(),
                        transfer.getCurrency()
                ),
                new InternalTransactionEntryRequest(
                        null,
                        transfer.getDestinationReference(),
                        "CREDIT",
                        transfer.getAmount(),
                        transfer.getCurrency()
                )
        );
        post(new InternalTransactionRequest(
                "transfer:" + transfer.getId(),
                transfer.getId(),
                transfer.getOwnerUserId(),
                "TRANSFER",
                transfer.getAmount(),
                transfer.getCurrency(),
                Instant.now(),
                entries
        ));
    }

    @Override
    public void recordReversal(Transfer transfer) {
        List<InternalTransactionEntryRequest> entries = List.of(
                new InternalTransactionEntryRequest(
                        transfer.getSourceAccountId(),
                        null,
                        "CREDIT",
                        transfer.getAmount(),
                        transfer.getCurrency()
                )
        );
        post(new InternalTransactionRequest(
                "transfer:" + transfer.getId() + ":reversal",
                transfer.getId(),
                transfer.getOwnerUserId(),
                "REVERSAL",
                transfer.getAmount(),
                transfer.getCurrency(),
                Instant.now(),
                entries
        ));
    }

    private void post(InternalTransactionRequest request) {
        restClient.post()
                .uri(baseUrl + "/internal/transactions")
                .header(INTERNAL_TOKEN_HEADER, internalToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
