package com.ahmedkh.banking.audit.integration;

import com.ahmedkh.banking.audit.event.BankingEvent;
import com.ahmedkh.banking.audit.security.TestJwtFactory;
import com.ahmedkh.banking.audit.service.AuditService;
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
class AuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditService service;

    @Test
    void consumerIsIdempotentAndAdminCanList() throws Exception {
        UUID eventId = UUID.randomUUID();
        BankingEvent event = new BankingEvent(eventId, "TransferFailed", Instant.now(), Map.of(
                "ownerUserId", UUID.randomUUID().toString(), "transferId", UUID.randomUUID().toString(),
                "amount", new BigDecimal("10.00"), "currency", "EUR", "status", "FAILED"
        ));
        service.consume(event);
        service.consume(event);

        mockMvc.perform(get("/admin/audit").header("Authorization", "Bearer " + TestJwtFactory.token(UUID.randomUUID(), "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/admin/audit").header("Authorization", "Bearer " + TestJwtFactory.token(UUID.randomUUID(), "CLIENT")))
                .andExpect(status().isForbidden());
    }
}
