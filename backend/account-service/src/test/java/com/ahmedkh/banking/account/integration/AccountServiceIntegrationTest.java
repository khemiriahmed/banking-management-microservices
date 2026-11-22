package com.ahmedkh.banking.account.integration;

import com.ahmedkh.banking.account.repository.BankAccountRepository;
import com.ahmedkh.banking.account.security.InternalTokenFilter;
import com.ahmedkh.banking.account.security.TestJwtFactory;
import com.ahmedkh.banking.account.service.AccountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountServiceIntegrationTest {

    private static final String INTERNAL_TOKEN = "test-internal-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BankAccountRepository repository;

    @Autowired
    private AccountService service;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void clientSeesOnlyOwnAccountsAndCannotReadAnotherClientsAccount() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID otherOwner = UUID.randomUUID();
        String accountId = createAccount(owner, "EUR", "100.00");
        String otherAccountId = createAccount(otherOwner, "EUR", "50.00");
        String token = clientToken(owner);

        mockMvc.perform(get("/accounts").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(accountId))
                .andExpect(jsonPath("$[0].ownerUserId").value(owner.toString()))
                .andExpect(jsonPath("$[0].reservedBalance").doesNotExist());

        mockMvc.perform(get("/accounts/{id}", accountId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/accounts/{id}", otherAccountId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientCannotUseAdminEndpoints() throws Exception {
        String token = clientToken(UUID.randomUUID());
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(post("/admin/accounts")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerUserId":"%s","accountType":"CHECKING","currency":"EUR","initialBalance":10}
                                """.formatted(unknownId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/admin/accounts/{id}/status", unknownId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SUSPENDED"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesAccountAndChangesItsStatus() throws Exception {
        String accountId = createAccount(UUID.randomUUID(), "EUR", "0.00");

        mockMvc.perform(patch("/admin/accounts/{id}/status", accountId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SUSPENDED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void suspendedAccountAndInvalidInternalTokenAreRejected() throws Exception {
        String accountId = createAccount(UUID.randomUUID(), "EUR", "100.00");
        patchStatus(accountId, "SUSPENDED");

        internalPost("reserve", accountId, INTERNAL_TOKEN, "10.00", "EUR")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Account status does not allow this operation"));

        internalPost("reserve", accountId, "wrong-token", "10.00", "EUR")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalOperationsRespectBigDecimalAndReservationRules() throws Exception {
        String accountId = createAccount(UUID.randomUUID(), "EUR", "100.00");

        internalPost("reserve", accountId, INTERNAL_TOKEN, "25.50", "EUR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedBalance").value(25.50))
                .andExpect(jsonPath("$.availableBalance").value(74.50));

        internalPost("debit", accountId, INTERNAL_TOKEN, "25.50", "EUR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(74.50))
                .andExpect(jsonPath("$.reservedBalance").value(0));

        internalPost("credit", accountId, INTERNAL_TOKEN, "10.25", "EUR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(84.75));

        internalPost("reserve", accountId, INTERNAL_TOKEN, "5.00", "EUR")
                .andExpect(status().isOk());
        internalPost("release", accountId, INTERNAL_TOKEN, "5.00", "EUR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedBalance").value(0));

        internalPost("reserve", accountId, INTERNAL_TOKEN, "-1.00", "EUR")
                .andExpect(status().isBadRequest());
        internalPost("reserve", accountId, INTERNAL_TOKEN, "1.234", "EUR")
                .andExpect(status().isBadRequest());
        internalPost("reserve", accountId, INTERNAL_TOKEN, "1.00", "USD")
                .andExpect(status().isConflict());
    }

    @Test
    @Timeout(20)
    void concurrentReservationsCannotOverspendTheAccount() throws Exception {
        String accountId = createAccount(UUID.randomUUID(), "EUR", "100.00");
        UUID id = UUID.fromString(accountId);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> reserve = () -> {
            start.await();
            try {
                service.reserve(id, new BigDecimal("60.00"), "EUR");
                return true;
            } catch (RuntimeException exception) {
                return false;
            }
        };

        try {
            Future<Boolean> first = executor.submit(reserve);
            Future<Boolean> second = executor.submit(reserve);
            start.countDown();
            boolean firstResult = first.get(10, TimeUnit.SECONDS);
            boolean secondResult = second.get(10, TimeUnit.SECONDS);
            long successes = (firstResult ? 1 : 0) + (secondResult ? 1 : 0);

            assertThat(successes).isEqualTo(1);
            var account = repository.findById(id).orElseThrow();
            assertThat(account.getReservedBalance()).isEqualByComparingTo("60.00");
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("40.00");
        } finally {
            executor.shutdownNow();
        }
    }

    private String createAccount(UUID ownerUserId, String currency, String initialBalance) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/accounts")
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerUserId":"%s","accountType":"CHECKING","currency":"%s","initialBalance":%s}
                                """.formatted(ownerUserId, currency, initialBalance)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    private void patchStatus(String accountId, String status) throws Exception {
        mockMvc.perform(patch("/admin/accounts/{id}/status", accountId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"%s"}
                                """.formatted(status)))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions internalPost(
            String operation,
            String accountId,
            String token,
            String amount,
            String currency
    ) throws Exception {
        return mockMvc.perform(post("/internal/accounts/{id}/{operation}", accountId, operation)
                .header(InternalTokenFilter.TOKEN_HEADER, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":%s,"currency":"%s"}
                        """.formatted(amount, currency)));
    }

    private String clientToken(UUID authUserId) {
        return bearer(TestJwtFactory.token(authUserId, "client@example.com", "CLIENT"));
    }

    private String adminToken() {
        return bearer(TestJwtFactory.token(UUID.randomUUID(), "admin@example.com", "ADMIN"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
