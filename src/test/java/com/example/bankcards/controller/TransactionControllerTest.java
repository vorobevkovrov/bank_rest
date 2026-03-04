package com.example.bankcards.controller;

import com.example.bankcards.controller.config.TestSecurityConfig;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.TransactionStatus;
import com.example.bankcards.exception.exceptions.CardBlockedException;
import com.example.bankcards.exception.exceptions.InsufficientFundsException;
import com.example.bankcards.exception.exceptions.SameCardTransferException;
import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.util.JwtService;
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
    @MockBean
    private UserDetailsServiceImpl userDetailsService;
    @MockBean
    private JwtService jwtService;

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
    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void transfer_WithInsufficientFunds_ShouldReturnBadRequest() throws Exception {
        when(transactionService.transferBetweenOwnCards(any(TransferRequest.class), any()))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        mockMvc.perform(post("/api/v1/transactions/transfer/own")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void transfer_WithSameCard_ShouldReturnBadRequest() throws Exception {
        TransferRequest sameCardRequest = TransferRequest.builder()
                .fromCardId(1L)
                .toCardId(1L) // одинаковые ID
                .amount(BigDecimal.valueOf(100.00))
                .build();

        when(transactionService.transferBetweenOwnCards(any(TransferRequest.class), any()))
                .thenThrow(new SameCardTransferException("Cannot transfer to the same card"));

        mockMvc.perform(post("/api/v1/transactions/transfer/own")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sameCardRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void transfer_WithBlockedCard_ShouldReturnConflict() throws Exception {
        when(transactionService.transferBetweenOwnCards(any(TransferRequest.class), any()))
                .thenThrow(new CardBlockedException("Card is blocked"));

        mockMvc.perform(post("/api/v1/transactions/transfer/own")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isConflict());
    }
}
