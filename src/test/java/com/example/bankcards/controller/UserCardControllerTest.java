package com.example.bankcards.controller;

import com.example.bankcards.controller.config.TestSecurityConfig;
import com.example.bankcards.dto.request.BlockCardRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CardRequestResponse;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.CardRequestStatus;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.exceptions.CardBlockedException;
import com.example.bankcards.exception.exceptions.ResourceNotFoundException;
import com.example.bankcards.service.CardRequestService;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = UserCardController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {com.example.bankcards.config.JwtAuthenticationFilter.class}
        )
)
@EnableMethodSecurity()
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class UserCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @MockBean
    private CardRequestService cardRequestService;
    private CardResponse mockCardResponse;
    private BalanceResponse mockBalanceResponse;
    private CardRequestResponse mockRequestResponse;
    private BlockCardRequest blockCardRequest;

    @BeforeEach
    void setUp() {
        Date futureDate = Date.from(LocalDateTime.now().plusYears(1)
                .atZone(ZoneId.systemDefault()).toInstant());

        mockCardResponse = CardResponse.builder()
                .maskedNumber("**** **** **** 5678")
                .holderName("John Doe")
                .balance(BigDecimal.valueOf(1000.00))
                .status(CardStatus.ACTIVE)
                .expiryDate(futureDate)
                .userId(1001L)
                .build();

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
    @WithMockUser(username = "testuser", roles = "USER")
    void getMyCards_ShouldReturnUserCards() throws Exception {
        Page<CardResponse> page = new PageImpl<>(List.of(mockCardResponse));

        when(cardService.getCardsByUserName(eq("testuser"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/user/cards/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].maskedNumber").value("**** **** **** 5678"))
                .andExpect(jsonPath("$.content[0].holderName").value("John Doe"))
                .andExpect(jsonPath("$.content[0].balance").value(1000.00))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getCardBalance_ShouldReturnBalance() throws Exception {
        when(cardService.getCardBalance(eq(1L), any()))
                .thenReturn(mockBalanceResponse);

        mockMvc.perform(get("/api/v1/user/cards/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("**** **** **** 5678"))
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.holderName").value("John Doe"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void requestCardBlock_ShouldReturnRequestResponse() throws Exception {
        when(cardRequestService.requestCardBlock(any(BlockCardRequest.class), eq("testuser")))
                .thenReturn(mockRequestResponse);

        mockMvc.perform(post("/api/v1/user/cards/block")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockCardRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(1))
                .andExpect(jsonPath("$.cardId").value(1))
                .andExpect(jsonPath("$.cardMaskedNumber").value("**** **** **** 5678"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void userEndpoints_ShouldBeForbiddenForAdmin() throws Exception {
        when(cardService.getCardsByUserName(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/v1/user/cards/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userEndpoints_ShouldBeUnauthorizedForAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/user/cards/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getMyCards_WithPaginationParams_ShouldReturnUserCards() throws Exception {
        Page<CardResponse> page = new PageImpl<>(List.of(mockCardResponse));

        when(cardService.getCardsByUserName(eq("testuser"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/user/cards/my")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].maskedNumber").value("**** **** **** 5678"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getCardBalance_WithNonExistentCard_ShouldReturnNotFound() throws Exception {
        when(cardService.getCardBalance(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Card not found"));

        mockMvc.perform(get("/api/v1/user/cards/999/balance"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getCardBalance_WithOthersCard_ShouldReturnForbidden() throws Exception {
        when(cardService.getCardBalance(eq(1L), any()))
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/user/cards/1/balance"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void requestCardBlock_WithAlreadyBlockedCard_ShouldReturnBadRequest() throws Exception {
        when(cardRequestService.requestCardBlock(any(BlockCardRequest.class), eq("testuser")))
                .thenThrow(new CardBlockedException("Card already blocked"));

        mockMvc.perform(post("/api/v1/user/cards/block")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockCardRequest)))
                .andExpect(status().isConflict());
    }

}