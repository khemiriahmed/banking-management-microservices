package com.ahmedkh.banking.user.integration;

import com.ahmedkh.banking.user.security.TestJwtFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void clientReadsAndUpdatesOnlyOwnProfile() throws Exception {
        UUID authUserId = UUID.randomUUID();
        String token = bearer(TestJwtFactory.token(authUserId, "client@example.com", "CLIENT"));

        MvcResult created = mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUserId").value(authUserId.toString()))
                .andExpect(jsonPath("$.firstName").value("client"))
                .andExpect(jsonPath("$.lastName").value(""))
                .andReturn();

        JsonNode profile = objectMapper.readTree(created.getResponse().getContentAsString());

        mockMvc.perform(put("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ahmed","lastName":"Khemiri","phone":"0600000000"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(profile.get("id").asText()))
                .andExpect(jsonPath("$.authUserId").value(authUserId.toString()))
                .andExpect(jsonPath("$.firstName").value("Ahmed"))
                .andExpect(jsonPath("$.lastName").value("Khemiri"))
                .andExpect(jsonPath("$.phone").value("0600000000"));

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUserId").value(authUserId.toString()))
                .andExpect(jsonPath("$.firstName").value("Ahmed"));
    }

    @Test
    void clientCannotAccessAdminEndpoints() throws Exception {
        String clientToken = bearer(TestJwtFactory.token(UUID.randomUUID(), "client@example.com", "CLIENT"));

        mockMvc.perform(get("/admin/users").header(HttpHeaders.AUTHORIZATION, clientToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/users/{authUserId}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListAndFindProfileByAuthUserId() throws Exception {
        UUID authUserId = UUID.randomUUID();
        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                bearer(TestJwtFactory.token(authUserId, "admin-client@example.com", "CLIENT"))))
                .andExpect(status().isOk());

        String adminToken = bearer(TestJwtFactory.token(UUID.randomUUID(), "admin@example.com", "ADMIN"));

        MvcResult list = mockMvc.perform(get("/admin/users")
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andReturn();

        JsonNode page = objectMapper.readTree(list.getResponse().getContentAsString());
        assertThat(page.get("content").isArray()).isTrue();
        assertThat(page.get("content").toString()).contains(authUserId.toString());

        mockMvc.perform(get("/admin/users/{authUserId}", authUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUserId").value(authUserId.toString()));
    }

    @Test
    void adminGetsNotFoundForUnknownAuthUserId() throws Exception {
        String adminToken = bearer(TestJwtFactory.token(UUID.randomUUID(), "admin@example.com", "ADMIN"));

        mockMvc.perform(get("/admin/users/{authUserId}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void missingInvalidAndExpiredTokensAreRejected() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                bearer(TestJwtFactory.expiredToken(UUID.randomUUID(), "client@example.com", "CLIENT"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationRejectsBlankNames() throws Exception {
        String token = bearer(TestJwtFactory.token(UUID.randomUUID(), "client@example.com", "CLIENT"));

        mockMvc.perform(put("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":" ","lastName":"Khemiri"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations.firstName").exists());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
