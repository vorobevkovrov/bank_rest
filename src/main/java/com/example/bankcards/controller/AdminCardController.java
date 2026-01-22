package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@Slf4j
@RestController
@RequestMapping("api/v1/admin/cards")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Admin Card Management", description = "API для управления картами (только для администраторов)")
@SecurityRequirement(name = "bearerAuth")
public class AdminCardController {

    private final CardService cardService;

    @Operation(summary = "Создать новую карту")
    @PostMapping("/create")
    public ResponseEntity<CardResponse> createCard(
            @Valid @RequestBody CardCreateRequest request
    ) {
        log.info("Admin creating card for user: {}", request.getUserId());
        CardResponse response = cardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Активировать карту")
    @PostMapping("/{cardId}/activate")
    public ResponseEntity<CardResponse> activateCard(
            @Parameter(description = "ID карты") @PathVariable Long cardId
    ) {
        CardResponse activatedCard = cardService.activateCard(cardId);
        return ResponseEntity.status(HttpStatus.OK).body(activatedCard);
    }

    //TODO 403 forbidden
    @Operation(summary = "Получить все карты (с пагинацией)")
    @GetMapping
    public ResponseEntity<Page<CardResponse>> getAllCards(
            @PageableDefault(
                    size = 20,
                    sort = "expiryDate",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Page<CardResponse> cards = cardService.getAllCards(pageable);
        return ResponseEntity.ok(cards);
    }

    //TODO 403 forbidden
    @Operation(summary = "Получить карту по ID")
    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getCardById(
            @Parameter(description = "ID карты") @PathVariable Long cardId
    ) {
        CardResponse card = cardService.getCardById(cardId);
        return ResponseEntity.ok(card);
    }


    @Operation(summary = "Обновить карту (статус, лимиты)")
    @PutMapping("/{cardId}")
    public ResponseEntity<CardResponse> updateCard(
            @Parameter(description = "ID карты") @PathVariable Long cardId,
            @Valid @RequestBody CardUpdateRequest request
    ) {
        CardResponse updatedCard = cardService.updateCard(cardId, request);
        return ResponseEntity.ok(updatedCard);
    }

    @Operation(summary = "Заблокировать карту")
    @PostMapping("/{cardId}/block")
    public ResponseEntity<CardResponse> blockCard(
            @Parameter(description = "ID карты") @PathVariable Long cardId
    ) {
        CardResponse blockedCard = cardService.blockCard(cardId);
        return ResponseEntity.ok(blockedCard);
    }


    @Operation(summary = "Удалить карту (только с нулевым балансом)")
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @Parameter(description = "ID карты") @PathVariable Long cardId
    ) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить карты пользователя")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<CardResponse>> getCardsByUser(
            @Parameter(description = "ID пользователя") @PathVariable Long userId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<CardResponse> cards = cardService.getCardsByUserId(userId, pageable);
        return ResponseEntity.ok(cards);
    }
}

