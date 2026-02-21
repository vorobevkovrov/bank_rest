package com.example.bankcards.controller;

import com.example.bankcards.config.JwtAuthenticationFilter;
import com.example.bankcards.dto.request.BlockCardRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CardRequestResponse;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.CardRequestStatus;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.service.CardRequestService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserCardController.class)
@ActiveProfiles("test")
class UserCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CardRequestService cardRequestService;

    // Добавляем MockBean для зависимостей безопасности
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private CardResponse mockCardResponse;
    private BalanceResponse mockBalanceResponse;
    private CardRequestResponse mockRequestResponse;
    private BlockCardRequest blockCardRequest;

    @BeforeEach
    void setUp() {
        mockCardResponse = new CardResponse();
        mockCardResponse.setMaskedNumber("**** **** **** 5678");
        mockCardResponse.setHolderName("John Doe");
        mockCardResponse.setBalance(BigDecimal.valueOf(1000.00));
        mockCardResponse.setStatus(CardStatus.ACTIVE);
        mockCardResponse.setExpiryDate(new Date());
        mockCardResponse.setUserId(1001L);

        mockBalanceResponse = BalanceResponse.builder()
                .cardNumber("**** **** **** 5678")
                .holderName("John Doe")
                .balance(BigDecimal.valueOf(1000.00))
                .build();

        mockRequestResponse = CardRequestResponse.builder()
                .requestId(1L)
                .cardId(1L)
                .cardMaskedNumber("**** **** **** 5678")
                .status(CardRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        blockCardRequest = BlockCardRequest.builder()
                .cardId(1L)
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyCards_ShouldReturnUserCards() throws Exception {
        Page<CardResponse> page = new PageImpl<>(List.of(mockCardResponse));
        when(jwtService.extractUserId(anyString())).thenReturn(1001L);
        when(cardService.getCardsByUserId(anyLong(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/cards/my")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].maskedNumber").value("**** **** **** 5678"))
                .andExpect(jsonPath("$.content[0].userId").value(1001));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCardBalance_ShouldReturnBalance() throws Exception {
        when(cardService.getCardBalance(anyLong(), any()))
                .thenReturn(mockBalanceResponse);

        mockMvc.perform(get("/api/v1/cards/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("**** **** **** 5678"))
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.holderName").value("John Doe"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void requestCardBlock_ShouldReturnRequestResponse() throws Exception {
        when(cardRequestService.requestCardBlock(any(BlockCardRequest.class), anyString()))
                .thenReturn(mockRequestResponse);

        mockMvc.perform(post("/api/v1/cards/block")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockCardRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(1))
                .andExpect(jsonPath("$.cardId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void userEndpoints_ShouldBeForbiddenForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/cards/my")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userEndpoints_ShouldBeUnauthorizedForAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/cards/my"))
                .andExpect(status().isUnauthorized());
    }
}