package com.ahmedkh.banking.transfer.service;

import com.ahmedkh.banking.transfer.dto.AccountView;
import com.ahmedkh.banking.transfer.dto.BeneficiaryView;
import com.ahmedkh.banking.transfer.dto.InternalAccountResponse;
import com.ahmedkh.banking.transfer.dto.InternalAmountRequest;
import com.ahmedkh.banking.transfer.dto.InternalExternalCreditRequest;
import com.ahmedkh.banking.transfer.dto.InternalExternalCreditResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class HttpAccountServiceClient implements AccountServiceClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final RestClient restClient;
    private final String baseUrl;
    private final String internalToken;
    private final int retryAttempts;
    private final long retryDelayMs;

    public HttpAccountServiceClient(
            RestClient restClient,
            @Value("${app.account-service.base-url}") String baseUrl,
            @Value("${app.account-service.internal-token}") String internalToken,
            @Value("${app.account-service.retry-attempts}") int retryAttempts,
            @Value("${app.account-service.retry-delay-ms}") long retryDelayMs
    ) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
        this.internalToken = internalToken;
        this.retryAttempts = Math.max(1, retryAttempts);
        this.retryDelayMs = Math.max(0, retryDelayMs);
    }

    @Override
    public AccountView getAccount(UUID accountId, String authorization) {
        return getWithRetry("/accounts/{id}", accountId, authorization, AccountView.class);
    }

    @Override
    public BeneficiaryView getBeneficiary(UUID beneficiaryId, String authorization) {
        return getWithRetry("/beneficiaries/{id}", beneficiaryId, authorization, BeneficiaryView.class);
    }

    @Override
    public InternalAccountResponse reserve(UUID accountId, BigDecimal amount, String currency) {
        return restClient.post()
                .uri(baseUrl + "/internal/accounts/{id}/reserve", accountId)
                .header(INTERNAL_TOKEN_HEADER, internalToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new InternalAmountRequest(amount, currency))
                .retrieve()
                .body(InternalAccountResponse.class);
    }

    @Override
    public void debit(UUID accountId, BigDecimal amount, String currency) {
        internalPost("/internal/accounts/{id}/debit", accountId, amount, currency);
    }

    @Override
    public void creditSource(UUID accountId, BigDecimal amount, String currency) {
        internalPost("/internal/accounts/{id}/credit", accountId, amount, currency);
    }

    @Override
    public void release(UUID accountId, BigDecimal amount, String currency) {
        internalPost("/internal/accounts/{id}/release", accountId, amount, currency);
    }

    @Override
    public InternalExternalCreditResponse creditDestination(
            String destinationReference,
            BigDecimal amount,
            String currency,
            String operationId
    ) {
        return restClient.post()
                .uri(baseUrl + "/internal/accounts/external-credit")
                .header(INTERNAL_TOKEN_HEADER, internalToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new InternalExternalCreditRequest(destinationReference, amount, currency, operationId))
                .retrieve()
                .body(InternalExternalCreditResponse.class);
    }

    private void internalPost(String path, UUID accountId, BigDecimal amount, String currency) {
        restClient.post()
                .uri(baseUrl + path, accountId)
                .header(INTERNAL_TOKEN_HEADER, internalToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new InternalAmountRequest(amount, currency))
                .retrieve()
                .toBodilessEntity();
    }

    private <T> T getWithRetry(String path, UUID id, String authorization, Class<T> responseType) {
        RestClientException lastFailure = null;
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                T response = restClient.get()
                        .uri(baseUrl + path, id)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(responseType);
                if (response == null) {
                    throw new IllegalStateException("Empty response from account-service");
                }
                return response;
            } catch (HttpServerErrorException | ResourceAccessException exception) {
                lastFailure = exception;
                if (attempt < retryAttempts) {
                    pause();
                }
            }
        }
        throw lastFailure;
    }

    private void pause() {
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying account-service", exception);
        }
    }
}
