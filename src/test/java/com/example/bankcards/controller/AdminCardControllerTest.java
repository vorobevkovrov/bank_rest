package com.example.bankcards.controller;

import com.example.bankcards.config.JwtAuthenticationFilter;
import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.CardRequestResponse;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.CardRequestStatus;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.impl.CardRequestServiceImpl;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCardController.class)
@ActiveProfiles("test")
class AdminCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardRequestServiceImpl cardRequestService;

    @MockBean
    private CardService cardService;

    // Добавляем MockBean для зависимостей безопасности
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private CardResponse mockCardResponse;
    private CardCreateRequest mockCreateRequest;
    private CardUpdateRequest mockUpdateRequest;

    @BeforeEach
    void setUp() {
        mockCardResponse = new CardResponse();
        mockCardResponse.setMaskedNumber("**** **** **** 5678");
        mockCardResponse.setHolderName("John Doe");
        mockCardResponse.setBalance(BigDecimal.valueOf(1000.00));
        mockCardResponse.setStatus(CardStatus.ACTIVE);
        mockCardResponse.setExpiryDate(new Date());
        mockCardResponse.setUserId(1001L);

        mockCreateRequest = CardCreateRequest.builder()
                .userId(1001L)
                .cardHolderName("John Doe")
                .expiryDate(new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L))
                .initialBalance(BigDecimal.valueOf(1000.00))
                .cardNumberLastFour("5678")
                .cardStatus(CardStatus.ACTIVE)
                .build();

        mockUpdateRequest = new CardUpdateRequest();
        mockUpdateRequest.setId(1L);
        mockUpdateRequest.setStatus(CardStatus.BLOCKED);
        mockUpdateRequest.setBalance(BigDecimal.valueOf(5000.00));
        mockUpdateRequest.setExpiryDate(new Date());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_ShouldReturnCreatedCard() throws Exception {
        when(cardService.createCard(any(CardCreateRequest.class)))
                .thenReturn(mockCardResponse);

        mockMvc.perform(post("/api/v1/admin/cards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maskedNumber").value("**** **** **** 5678"))
                .andExpect(jsonPath("$.holderName").value("John Doe"))
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.userId").value(1001));

        verify(cardService).createCard(any(CardCreateRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void activateCard_ShouldReturnActivatedCard() throws Exception {
        CardResponse activatedCard = new CardResponse();
        activatedCard.setMaskedNumber("**** **** **** 5678");
        activatedCard.setStatus(CardStatus.ACTIVE);

        when(cardService.activateCard(anyLong()))
                .thenReturn(activatedCard);

        mockMvc.perform(patch("/api/v1/admin/cards/1/activate")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(cardService).activateCard(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCards_ShouldReturnPaginatedCards() throws Exception {
        Page<CardResponse> page = new PageImpl<>(List.of(mockCardResponse));
        when(cardService.getAllCards(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/cards")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].holderName").value("John Doe"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(cardService).getAllCards(any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCardById_ShouldReturnCard() throws Exception {
        when(cardService.getCardById(anyLong()))
                .thenReturn(mockCardResponse);

        mockMvc.perform(get("/api/v1/admin/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedNumber").value("**** **** **** 5678"))
                .andExpect(jsonPath("$.holderName").value("John Doe"));

        verify(cardService).getCardById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCard_ShouldReturnUpdatedCard() throws Exception {
        mockCardResponse.setStatus(CardStatus.BLOCKED);
        when(cardService.updateCard(anyLong(), any(CardUpdateRequest.class)))
                .thenReturn(mockCardResponse);

        mockMvc.perform(put("/api/v1/admin/cards/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        verify(cardService).updateCard(eq(1L), any(CardUpdateRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blockCard_ShouldReturnBlockedCard() throws Exception {
        mockCardResponse.setStatus(CardStatus.BLOCKED);
        when(cardService.blockCard(anyLong()))
                .thenReturn(mockCardResponse);

        mockMvc.perform(patch("/api/v1/admin/cards/1/block")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        verify(cardService).blockCard(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCard_ShouldReturnNoContent() throws Exception {
        doNothing().when(cardService).deleteCard(anyLong());

        mockMvc.perform(delete("/api/v1/admin/cards/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(cardService).deleteCard(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCardsByUser_ShouldReturnUserCards() throws Exception {
        Page<CardResponse> page = new PageImpl<>(List.of(mockCardResponse));
        when(cardService.getCardsByUserId(anyLong(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/cards/user/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(1001));

        verify(cardService).getCardsByUserId(eq(1001L), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getPendingRequests_ShouldReturnRequestsList() throws Exception {
        CardRequestResponse requestResponse = CardRequestResponse.builder()
                .requestId(1L)
                .cardId(1L)
                .cardMaskedNumber("**** **** **** 1234")
                .status(CardRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(cardRequestService.getAllPendingRequest(anyString()))
                .thenReturn(List.of(requestResponse));

        mockMvc.perform(get("/api/v1/admin/cards/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(cardRequestService).getAllPendingRequest(anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveRequest_ShouldReturnApprovedRequest() throws Exception {
        CardRequestResponse response = CardRequestResponse.builder()
                .requestId(1L)
                .status(CardRequestStatus.APPROVED)
                .build();

        when(cardRequestService.approveBlockRequest(anyLong(), anyString()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/cards/1/approve")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(cardRequestService).approveBlockRequest(eq(1L), anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectRequest_ShouldReturnRejectedRequest() throws Exception {
        CardRequestResponse response = CardRequestResponse.builder()
                .requestId(1L)
                .status(CardRequestStatus.REJECTED)
                .build();

        when(cardRequestService.rejectBlockRequest(anyLong(), anyString()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/cards/1/reject")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        verify(cardRequestService).rejectBlockRequest(eq(1L), anyString());
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminEndpoints_ShouldBeForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cards"))
                .andExpect(status().isForbidden());
    }
}