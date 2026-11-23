package com.ahmedkh.banking.transfer.service;

import com.ahmedkh.banking.transfer.dto.AccountView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpAccountServiceClientTest {

    private static final String BASE_URL = "http://account-service";
    private static final String AUTHORIZATION = "Bearer token";
    private static final String INTERNAL_TOKEN = "internal-token";

    private MockRestServiceServer server;
    private HttpAccountServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory());
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpAccountServiceClient(builder.build(), BASE_URL, INTERNAL_TOKEN, 2, 0);
    }

    @Test
    void getAccountRetriesServerErrors() {
        UUID accountId = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/accounts/" + accountId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andRespond(withServerError());
        server.expect(requestTo(BASE_URL + "/accounts/" + accountId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andRespond(withSuccess("""
                        {"id":"%s","currency":"EUR","status":"ACTIVE"}
                        """.formatted(accountId), MediaType.APPLICATION_JSON));

        AccountView account = client.getAccount(accountId, AUTHORIZATION);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.currency()).isEqualTo("EUR");
        assertThat(account.status()).isEqualTo("ACTIVE");
        server.verify();
    }

    @Test
    void reserveIsNotRetriedAfterServerError() {
        UUID accountId = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/accounts/" + accountId + "/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Service-Token", INTERNAL_TOKEN))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.reserve(accountId, new BigDecimal("10.00"), "EUR"))
                .isInstanceOf(HttpServerErrorException.class);
        server.verify();
    }
}
