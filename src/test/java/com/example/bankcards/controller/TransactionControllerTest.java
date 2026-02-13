package com.example.bankcards.controller;

import com.example.bankcards.controller.config.TestSecurityConfig;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.TransactionStatus;
import com.example.bankcards.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;
    private TransferRequest transferRequest;
    private TransferResponse transferResponse;

    @BeforeEach
    void setUp() {
        transferRequest = TransferRequest.builder()
                .fromCardId(1L)
                .toCardId(2L)
                .amount(BigDecimal.valueOf(100.00))
                .build();

        transferResponse = TransferResponse.builder()
                .transactionId(123456L)
                .fromCardId(1L)
                .toCardId(2L)
                .amount(BigDecimal.valueOf(100.00))
                .status(TransactionStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .message("Transfer completed successfully")
                .build();
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void transferBetweenOwnCards_ShouldReturnTransferResponse() throws Exception {
        when(transactionService.transferBetweenOwnCards(any(TransferRequest.class), any()))
                .thenReturn(transferResponse);

        mockMvc.perform(post("/api/v1/transactions/transfer/own")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transactionId").value(123456))
                .andExpect(jsonPath("$.fromCardId").value(1))
                .andExpect(jsonPath("$.toCardId").value(2))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void transferBetweenOwnCards_ShouldBeForbiddenForAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/transfer/own")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void transferBetweenOwnCards_ShouldBeUnauthorizedForAnonymous() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/transfer/own")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isUnauthorized()); // Ожидаем 401
    }
}
