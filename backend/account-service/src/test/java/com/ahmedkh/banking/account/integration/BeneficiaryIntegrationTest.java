package com.ahmedkh.banking.account.integration;

import com.ahmedkh.banking.account.repository.BeneficiaryRepository;
import com.ahmedkh.banking.account.security.TestJwtFactory;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BeneficiaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BeneficiaryRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void clientCanCrudOwnBeneficiaryAndReferenceStaysMasked() throws Exception {
        UUID owner = UUID.randomUUID();
        String token = clientToken(owner);

        MvcResult created = mockMvc.perform(post("/beneficiaries")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Alice Dupont","destinationAccountReference":"FR76 **** **** **** 1234"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerUserId").value(owner.toString()))
                .andExpect(jsonPath("$.displayName").value("Alice Dupont"))
                .andExpect(jsonPath("$.destinationAccountReference").value("FR76 **** **** **** 1234"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        String id = idFrom(created);

        mockMvc.perform(get("/beneficiaries").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get("/beneficiaries/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(put("/beneficiaries/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Alice Martin","destinationAccountReference":"FR76 **** **** **** 9876","status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice Martin"))
                .andExpect(jsonPath("$.destinationAccountReference").value("FR76 **** **** **** 9876"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(delete("/beneficiaries/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/beneficiaries/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientCannotReadUpdateOrDeleteAnotherClientsBeneficiary() throws Exception {
        UUID owner = UUID.randomUUID();
        String ownerToken = clientToken(owner);
        String id = idFrom(mockMvc.perform(post("/beneficiaries")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Alice","destinationAccountReference":"FR76 **** **** **** 1234"}
                                """))
                .andExpect(status().isCreated())
                .andReturn());
        String otherToken = clientToken(UUID.randomUUID());

        mockMvc.perform(get("/beneficiaries").header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/beneficiaries/{id}", id).header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/beneficiaries/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Hacked"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/beneficiaries/{id}", id).header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isNotFound());

        assertThat(repository.findById(UUID.fromString(id))).isPresent();
    }

    @Test
    void createRejectsInvalidInputAndDuplicateReferencePerOwner() throws Exception {
        UUID owner = UUID.randomUUID();
        String token = clientToken(owner);

        mockMvc.perform(post("/beneficiaries")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"   ","destinationAccountReference":"FR76 **** **** **** 1234"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/beneficiaries")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Alice","destinationAccountReference":"FR7630006000011234567890189"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/beneficiaries")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Alice","destinationAccountReference":"FR76 **** **** **** 1234"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/beneficiaries")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Alice Dupont","destinationAccountReference":"FR76 **** **** **** 1234"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/beneficiaries")
                        .header(HttpHeaders.AUTHORIZATION, clientToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Bob","destinationAccountReference":"FR76 **** **** **** 1234"}
                                """))
                .andExpect(status().isCreated());
    }

    private String idFrom(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    private String clientToken(UUID authUserId) {
        return "Bearer " + TestJwtFactory.token(authUserId, "client@example.com", "CLIENT");
    }
}
