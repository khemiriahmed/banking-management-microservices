package com.ahmedkh.banking.transfer.integration;

import com.ahmedkh.banking.transfer.dto.AccountView;
import com.ahmedkh.banking.transfer.dto.BeneficiaryView;
import com.ahmedkh.banking.transfer.entity.TransferStatus;
import com.ahmedkh.banking.transfer.exception.TransferBusinessException;
import com.ahmedkh.banking.transfer.repository.TransferRepository;
import com.ahmedkh.banking.transfer.repository.TransferStatusHistoryRepository;
import com.ahmedkh.banking.transfer.security.TestJwtFactory;
import com.ahmedkh.banking.transfer.service.AccountServiceClient;
import com.ahmedkh.banking.transfer.service.TransactionServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferIntegrationTest {

    private static final String DESTINATION_REFERENCE = "FR76 **** **** **** 1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransferRepository repository;

    @Autowired
    private TransferStatusHistoryRepository historyRepository;

    @MockitoBean
    private AccountServiceClient client;

    @MockitoBean
    private TransactionServiceClient transactionClient;

    private UUID owner;
    private UUID sourceAccountId;
    private UUID beneficiaryId;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        repository.deleteAll();
        owner = UUID.randomUUID();
        sourceAccountId = UUID.randomUUID();
        beneficiaryId = UUID.randomUUID();
        when(client.getAccount(any(), anyString())).thenAnswer(invocation ->
                new AccountView(invocation.getArgument(0), "EUR", "ACTIVE"));
        when(client.getBeneficiary(any(), anyString())).thenAnswer(invocation ->
                new BeneficiaryView(invocation.getArgument(0), DESTINATION_REFERENCE, "ACTIVE"));
    }

    @Test
    void createTransferRunsSagaAndPersistsHistory() throws Exception {
        MvcResult result = createRequest("idem-success", "25.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.compensationStatus").value("NOT_REQUIRED"))
                .andExpect(jsonPath("$.destinationReference").value(DESTINATION_REFERENCE))
                .andExpect(jsonPath("$.history.length()").value(4))
                .andReturn();
        JsonNode json = json(result);
        String id = json.get("id").asText();

        assertThat(json.get("ownerUserId").asText()).isEqualTo(owner.toString());
        assertThat(json.get("amount").decimalValue()).isEqualByComparingTo("25.00");
        verify(client).reserve(sourceAccountId, new BigDecimal("25.00"), "EUR");
        verify(client).debit(sourceAccountId, new BigDecimal("25.00"), "EUR");
        verify(client).creditDestination(
                eq(DESTINATION_REFERENCE),
                eq(new BigDecimal("25.00")),
                eq("EUR"),
                eq(id + ":credit")
        );

        mockMvc.perform(get("/transfers/{id}", id).header(HttpHeaders.AUTHORIZATION, clientToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(get("/transfers").header(HttpHeaders.AUTHORIZATION, clientToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/transfers/{id}/history", id).header(HttpHeaders.AUTHORIZATION, clientToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void duplicateKeyReplaysSameTransfer() throws Exception {
        JsonNode first = json(createRequest("idem-double", "25.00")
                .andExpect(status().isCreated())
                .andReturn());
        JsonNode second = json(createRequest("idem-double", "25.00")
                .andExpect(status().isCreated())
                .andReturn());

        assertThat(second.get("id").asText()).isEqualTo(first.get("id").asText());
        assertThat(second.get("amount").asText()).isEqualTo(first.get("amount").asText());
        assertThat(second.get("status").asText()).isEqualTo(first.get("status").asText());
        verify(client, times(1)).reserve(sourceAccountId, new BigDecimal("25.00"), "EUR");
        verify(client, times(1)).debit(sourceAccountId, new BigDecimal("25.00"), "EUR");
    }

    @Test
    void sameKeyWithDifferentPayloadIsConflict() throws Exception {
        createRequest("idem-conflict", "25.00").andExpect(status().isCreated());

        mockMvc.perform(post("/transfers")
                        .header(HttpHeaders.AUTHORIZATION, clientToken())
                        .header("Idempotency-Key", "idem-conflict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("30.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void insufficientBalanceMarksTransferFailedWithoutCompensation() throws Exception {
        doThrow(new TransferBusinessException("Insufficient available balance"))
                .when(client).reserve(any(), any(), any());

        createRequest("idem-insufficient", "25.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.compensationStatus").value("NOT_REQUIRED"))
                .andExpect(jsonPath("$.failureReason").value(
                        org.hamcrest.Matchers.containsString("Insufficient available balance")));

        verify(client, never()).debit(any(), any(), any());
        verify(client, never()).creditDestination(anyString(), any(), anyString(), anyString());
    }

    @Test
    void invalidBeneficiaryMarksTransferFailed() throws Exception {
        when(client.getBeneficiary(eq(beneficiaryId), anyString()))
                .thenReturn(new BeneficiaryView(beneficiaryId, DESTINATION_REFERENCE, "SUSPENDED"));

        createRequest("idem-beneficiary", "25.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value(
                        org.hamcrest.Matchers.containsString("Beneficiary is not active")));

        verify(client, never()).reserve(any(), any(), any());
    }

    @Test
    void serviceUnavailableMarksTransferFailed() throws Exception {
        when(client.getAccount(any(), anyString()))
                .thenThrow(new ResourceAccessException("account-service is down"));

        createRequest("idem-unavailable", "25.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value(
                        org.hamcrest.Matchers.containsString("account-service is down")));

        verify(client, never()).reserve(any(), any(), any());
    }

    @Test
    void creditFailureRefundsSource() throws Exception {
        when(client.creditDestination(anyString(), any(), anyString(), anyString()))
                .thenThrow(new ResourceAccessException("destination unavailable"));

        createRequest("idem-credit-failure", "25.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.compensationStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.failureReason").value(
                        org.hamcrest.Matchers.containsString("source credited back")));

        verify(client).creditSource(sourceAccountId, new BigDecimal("25.00"), "EUR");
        verify(client, never()).release(any(), any(), any());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(post("/transfers")
                        .header("Idempotency-Key", "idem-no-auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("25.00")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedRequestAndMissingIdempotencyKeyAreRejected() throws Exception {
        mockMvc.perform(post("/transfers")
                        .header(HttpHeaders.AUTHORIZATION, clientToken())
                        .header("Idempotency-Key", "idem-invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("0.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations.amount").exists());

        mockMvc.perform(post("/transfers")
                        .header(HttpHeaders.AUTHORIZATION, clientToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("25.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferStatusTransitionsFollowSagaContract() {
        assertThat(TransferStatus.REQUESTED.canTransitionTo(TransferStatus.VALIDATING)).isTrue();
        assertThat(TransferStatus.VALIDATING.canTransitionTo(TransferStatus.PROCESSING)).isTrue();
        assertThat(TransferStatus.PROCESSING.canTransitionTo(TransferStatus.COMPLETED)).isTrue();
        assertThat(TransferStatus.PROCESSING.canTransitionTo(TransferStatus.FAILED)).isTrue();
        assertThat(TransferStatus.COMPLETED.canTransitionTo(TransferStatus.FAILED)).isFalse();
        assertThat(TransferStatus.FAILED.canTransitionTo(TransferStatus.PROCESSING)).isFalse();
    }

    private ResultActions createRequest(String key, String amount) throws Exception {
        return mockMvc.perform(post("/transfers")
                .header(HttpHeaders.AUTHORIZATION, clientToken())
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(amount)));
    }

    private String requestBody(String amount) {
        return """
                {
                  "sourceAccountId":"%s",
                  "beneficiaryId":"%s",
                  "amount":%s,
                  "currency":"EUR",
                  "reason":"invoice-42"
                }
                """.formatted(sourceAccountId, beneficiaryId, amount);
    }

    private String clientToken() {
        return "Bearer " + TestJwtFactory.token(owner, "client@example.com", "CLIENT");
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
