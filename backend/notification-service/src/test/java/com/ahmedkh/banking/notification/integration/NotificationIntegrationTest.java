package com.ahmedkh.banking.notification.integration;

import com.ahmedkh.banking.notification.event.BankingEvent;
import com.ahmedkh.banking.notification.security.TestJwtFactory;
import com.ahmedkh.banking.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationService service;

    @Test
    void consumerIsIdempotentAndListIsOwnerScoped() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        BankingEvent event = new BankingEvent(eventId, "TransferCompleted", Instant.now(), Map.of(
                "ownerUserId", owner.toString(), "amount", new BigDecimal("10.00"), "currency", "EUR", "status", "COMPLETED"
        ));
        service.consume(event);
        service.consume(event);

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + TestJwtFactory.token(owner, "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + TestJwtFactory.token(other, "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
