package com.ahmedkh.banking.transaction.integration;

import com.ahmedkh.banking.transaction.dto.CreateTransactionEntryRequest;
import com.ahmedkh.banking.transaction.dto.CreateTransactionRequest;
import com.ahmedkh.banking.transaction.dto.EntrySideDto;
import com.ahmedkh.banking.transaction.dto.TransactionTypeDto;
import com.ahmedkh.banking.transaction.entity.EntrySide;
import com.ahmedkh.banking.transaction.entity.TransactionEntry;
import com.ahmedkh.banking.transaction.entity.TransactionRecord;
import com.ahmedkh.banking.transaction.entity.TransactionStatus;
import com.ahmedkh.banking.transaction.entity.TransactionType;
import com.ahmedkh.banking.transaction.repository.TransactionRecordRepository;
import com.ahmedkh.banking.transaction.security.TestJwtFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRecordRepository repository;

    private UUID owner;
    private UUID otherOwner;
    private UUID accountId;
    private String token;
    private String otherToken;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        owner = UUID.randomUUID();
        otherOwner = UUID.randomUUID();
        accountId = UUID.randomUUID();
        token = TestJwtFactory.token(owner, "owner@example.com", "CLIENT");
        otherToken = TestJwtFactory.token(otherOwner, "other@example.com", "CLIENT");
    }

    @Test
    void internalEndpointCreatesIdempotentTransactionWithEntries() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "transfer-1",
                UUID.randomUUID(),
                owner,
                TransactionTypeDto.TRANSFER,
                new BigDecimal("25.00"),
                "EUR",
                Instant.now(),
                List.of(
                        new CreateTransactionEntryRequest(
                                accountId, null, EntrySideDto.DEBIT, new BigDecimal("25.00"), "EUR"
                        ),
                        new CreateTransactionEntryRequest(
                                null, "FR76 **** **** **** 1234", EntrySideDto.CREDIT,
                                new BigDecimal("25.00"), "EUR"
                        )
                )
        );
        String body = objectMapper.writeValueAsString(request);

        String firstId = mockMvc.perform(post("/internal/transactions")
                        .header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alreadyProcessed").value(false))
                .andExpect(jsonPath("$.transaction.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.transaction.entries.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/internal/transactions")
                        .header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyProcessed").value(true));

        assertThat(repository.findAll()).hasSize(1);
        assertThat(objectMapper.readTree(firstId).path("transaction").path("externalReference").asText())
                .isEqualTo("transfer-1");
        mockMvc.perform(post("/internal/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clientListsFiltersAndPaginatesOnlyOwnTransactions() throws Exception {
        TransactionRecord recent = seed("transfer-recent", owner, accountId, new BigDecimal("25.00"),
                Instant.parse("2026-11-23T10:00:00Z"));
        seed("transfer-old", owner, accountId, new BigDecimal("10.00"),
                Instant.parse("2026-11-20T10:00:00Z"));
        seed("transfer-other-account", owner, UUID.randomUUID(), new BigDecimal("5.00"),
                Instant.parse("2026-11-23T11:00:00Z"));
        seed("transfer-other-owner", otherOwner, accountId, new BigDecimal("7.00"),
                Instant.parse("2026-11-23T12:00:00Z"));

        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3));

        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .param("accountId", accountId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .param("from", DateTimeFormatter.ISO_INSTANT.format(Instant.parse("2026-11-22T00:00:00Z")))
                        .param("to", DateTimeFormatter.ISO_INSTANT.format(Instant.parse("2026-11-24T00:00:00Z")))
                        .param("sortBy", "amount")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].externalReference").value("transfer-recent"));

        mockMvc.perform(get("/transactions/" + recent.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalReference").value("transfer-recent"));
    }

    @Test
    void anotherClientCannotReadTransactionAndInvalidFiltersAreRejected() throws Exception {
        TransactionRecord record = seed("transfer-private", owner, accountId, new BigDecimal("25.00"),
                Instant.now());

        mockMvc.perform(get("/transactions/" + record.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/transactions/" + record.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "51"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .param("sortBy", "ownerUserId"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-11-23T10:00:00Z")
                        .param("to", "2026-11-20T10:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    private TransactionRecord seed(
            String externalReference,
            UUID ownerUserId,
            UUID entryAccountId,
            BigDecimal amount,
            Instant occurredAt
    ) {
        TransactionRecord record = new TransactionRecord(
                UUID.randomUUID(),
                externalReference,
                UUID.randomUUID(),
                ownerUserId,
                TransactionType.TRANSFER,
                TransactionStatus.CONFIRMED,
                amount,
                "EUR",
                occurredAt,
                Instant.now()
        );
        record.addEntry(new TransactionEntry(
                UUID.randomUUID(), record, entryAccountId, null, EntrySide.DEBIT, amount, "EUR"
        ));
        record.addEntry(new TransactionEntry(
                UUID.randomUUID(), record, null, "FR76 **** **** **** 1234",
                EntrySide.CREDIT, amount, "EUR"
        ));
        return repository.saveAndFlush(record);
    }
}
