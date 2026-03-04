package com.example.bankcards.controller;

import com.example.bankcards.controller.config.TestSecurityConfig;
import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.CardRequestResponse;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.CardRequestStatus;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.exceptions.ResourceNotFoundException;
import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.impl.CardRequestServiceImpl;
import com.example.bankcards.util.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCardController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AdminCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardRequestServiceImpl cardRequestService;

    @MockBean
    private CardService cardService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtService jwtService;

    private CardResponse mockCardResponse;
    private CardCreateRequest mockCreateRequest;
    private CardUpdateRequest mockUpdateRequest;
    private Date futureDate;

    @BeforeEach
    void setUp() {
        // Создаем дату в будущем для expiryDate
        futureDate = Date.from(LocalDateTime.now().plusYears(1)
                .atZone(ZoneId.systemDefault()).toInstant());

        mockCardResponse = CardResponse.builder()
                .maskedNumber("**** **** **** 5678")
                .holderName("John Doe")
                .balance(BigDecimal.valueOf(1000.00))
                .status(CardStatus.ACTIVE)
                .expiryDate(futureDate)
                .userId(1001L)
                .build();

        mockCreateRequest = CardCreateRequest.builder()
                .userId(1001L)
                .cardHolderName("John Doe")
                .expiryDate(futureDate)
                .initialBalance(BigDecimal.valueOf(1000.00))
                .build();

        mockUpdateRequest = new CardUpdateRequest(
                1L,
                null, // date parameter (может быть null если не используется)
                futureDate,
                CardStatus.BLOCKED,
                BigDecimal.valueOf(5000.00)
        );
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
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.userId").value(1001));

        verify(cardService).createCard(any(CardCreateRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void activateCard_ShouldReturnActivatedCard() throws Exception {
        CardResponse activatedCard = CardResponse.builder()
                .maskedNumber("**** **** **** 5678")
                .holderName("John Doe")
                .balance(BigDecimal.valueOf(1000.00))
                .status(CardStatus.ACTIVE)
                .expiryDate(futureDate)
                .userId(1001L)
                .build();

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
                .andExpect(jsonPath("$.content[0].maskedNumber").value("**** **** **** 5678"))
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
                .andExpect(jsonPath("$.holderName").value("John Doe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(cardService).getCardById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCard_ShouldReturnUpdatedCard() throws Exception {
        CardResponse updatedCardResponse = CardResponse.builder()
                .maskedNumber("**** **** **** 5678")
                .holderName("John Doe")
                .balance(BigDecimal.valueOf(5000.00))
                .status(CardStatus.BLOCKED)
                .expiryDate(futureDate)
                .userId(1001L)
                .build();

        when(cardService.updateCard(anyLong(), any(CardUpdateRequest.class)))
                .thenReturn(updatedCardResponse);

        mockMvc.perform(put("/api/v1/admin/cards/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.balance").value(5000.00));

        verify(cardService).updateCard(eq(1L), any(CardUpdateRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blockCard_ShouldReturnBlockedCard() throws Exception {
        CardResponse blockedCardResponse = CardResponse.builder()
                .maskedNumber("**** **** **** 5678")
                .holderName("John Doe")
                .balance(BigDecimal.valueOf(1000.00))
                .status(CardStatus.BLOCKED)
                .expiryDate(futureDate)
                .userId(1001L)
                .build();

        when(cardService.blockCard(anyLong()))
                .thenReturn(blockedCardResponse);

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
                .cardId(1L)
                .cardMaskedNumber("**** **** **** 1234")
                .status(CardRequestStatus.APPROVED)
                .createdAt(LocalDateTime.now())
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
                .cardId(1L)
                .cardMaskedNumber("**** **** **** 1234")
                .status(CardRequestStatus.REJECTED)
                .createdAt(LocalDateTime.now())
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

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        CardCreateRequest invalidRequest = CardCreateRequest.builder()
                .userId(null) // должно быть not null
                .cardHolderName("") // пустое имя
                .expiryDate(null) // null дата
                .initialBalance(BigDecimal.valueOf(-100.00)) // отрицательный баланс
                .build();

        mockMvc.perform(post("/api/v1/admin/cards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(cardService, never()).createCard(any());
    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_WithNonExistentUser_ShouldReturnNotFound() throws Exception {
        when(cardService.createCard(any(CardCreateRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/api/v1/admin/cards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockCreateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCardById_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        when(cardService.getCardById(anyLong()))
                .thenThrow(new ResourceNotFoundException("Card not found"));

        mockMvc.perform(get("/api/v1/admin/cards/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveNonExistentRequest_ShouldReturnNotFound() throws Exception {
        when(cardRequestService.approveBlockRequest(anyLong(), anyString()))
                .thenThrow(new ResourceNotFoundException("Request not found"));

        mockMvc.perform(patch("/api/v1/admin/cards/999/approve")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCard_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        CardUpdateRequest invalidRequest = new CardUpdateRequest(
                1L,
                null,
                null, // null дата
                null, // null статус
                BigDecimal.valueOf(-100.00) // отрицательный баланс
        );

        mockMvc.perform(put("/api/v1/admin/cards/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}