package com.ahmedkh.banking.transfer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TransactionClientConfig {

    @Bean
    public RestClient transactionRestClient(
            RestClient.Builder builder,
            @Value("${app.transaction-service.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${app.transaction-service.read-timeout-ms}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return builder.requestFactory(requestFactory).build();
    }
}
