package com.example.bankcards.controller;

import com.example.bankcards.dto.request.BlockCardRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CardRequestResponse;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.service.CardRequestService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.util.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Управление картами пользователя", description = "Операции для управления картами пользователей")
@SecurityRequirement(name = "BearerAuthentication")
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/cards")
public class UserCardController {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private final CardService cardService;
    private final JwtService jwtService;
    private final CardRequestService cardRequestService;

    @Operation(
            summary = "Получить все карты пользователя",
            description = "Получение списка всех карт текущего пользователя с пагинацией"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список карт успешно получен"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<CardResponse>> getMyCards(
            @Parameter(hidden = true) HttpServletRequest request,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        String authorizationHeader = request.getHeader("Authorization");
        Long userId = jwtService.extractUserId(authorizationHeader.substring(7));

        log.info("Пользователь с ID {} запрашивает список своих карт", userId);
        Page<CardResponse> cards = cardService.getCardsByUserId(userId, pageable);
        return ResponseEntity.ok(cards);
    }

    @Operation(
            summary = "Получить баланс карты",
            description = "Получение текущего баланса конкретной карты пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Баланс успешно получен"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @GetMapping("/{cardId}/balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BalanceResponse> getCardBalance(
            @Parameter(description = "ID карты") @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Пользователь {} запрашивает баланс карты с ID: {}",
                userDetails.getUsername(), cardId);
        BalanceResponse balance = cardService.getCardBalance(cardId, userDetails);
        return ResponseEntity.ok(balance);
    }

    @Operation(
            summary = "Запросить блокировку карты",
            description = "Создание запроса на блокировку карты пользователем"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запрос на блокировку успешно создан"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    @PostMapping("/block")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CardRequestResponse> requestCardBlock(
            @Parameter(description = "Данные для запроса блокировки")
            @Valid @RequestBody BlockCardRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Пользователь {} создает запрос на блокировку карты.}",
                userDetails.getUsername());
        CardRequestResponse response = cardRequestService.requestCardBlock(
                request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}