package com.ahmedkh.banking.transfer.service;

import com.ahmedkh.banking.transfer.entity.Transfer;
import com.ahmedkh.banking.transfer.entity.TransferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpTransactionServiceClientTest {

    private static final String BASE_URL = "http://transaction-service";
    private static final String INTERNAL_TOKEN = "internal-token";
    private static final String DESTINATION_REFERENCE = "FR76 **** **** **** 1234";

    private MockRestServiceServer server;
    private HttpTransactionServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory());
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpTransactionServiceClient(builder.build(), BASE_URL, INTERNAL_TOKEN);
    }

    @Test
    void completedTransferPostsTwoEntriesWithInternalToken() {
        Transfer transfer = newTransfer();

        server.expect(requestTo(BASE_URL + "/internal/transactions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Service-Token", INTERNAL_TOKEN))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.externalReference").value("transfer:" + transfer.getId()))
                .andExpect(jsonPath("$.transferId").value(transfer.getId().toString()))
                .andExpect(jsonPath("$.ownerUserId").value(transfer.getOwnerUserId().toString()))
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.entries[0].accountId").value(transfer.getSourceAccountId().toString()))
                .andExpect(jsonPath("$.entries[0].side").value("DEBIT"))
                .andExpect(jsonPath("$.entries[1].destinationReference").value(DESTINATION_REFERENCE))
                .andExpect(jsonPath("$.entries[1].side").value("CREDIT"))
                .andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON));

        client.recordCompletedTransfer(transfer);

        server.verify();
    }

    @Test
    void reversalPostsSingleCreditEntry() {
        Transfer transfer = newTransfer();

        server.expect(requestTo(BASE_URL + "/internal/transactions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.externalReference").value("transfer:" + transfer.getId() + ":reversal"))
                .andExpect(jsonPath("$.type").value("REVERSAL"))
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].side").value("CREDIT"))
                .andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON));

        client.recordReversal(transfer);

        server.verify();
    }

    private Transfer newTransfer() {
        return new Transfer(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                DESTINATION_REFERENCE,
                new BigDecimal("25.00"),
                "EUR",
                "test",
                TransferStatus.COMPLETED,
                "key",
                "fingerprint",
                Instant.now()
        );
    }
}
