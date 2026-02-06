package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.CardRequestResponse;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.impl.CardRequestServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(
        name = "Администрирование карт",
        description = "API для управления банковскими картами администраторами"
)
@RequestMapping("/api/v1/admin/cards")
public class AdminCardController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int USER_CARDS_PAGE_SIZE = 10;

    private final CardRequestServiceImpl cardRequestService;
    private final CardService cardService;

    @Operation(
            summary = "Создать новую карту",
            description = "Создание новой банковской карты для пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Карта успешно создана",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "409", description = "Карта с такими параметрами уже существует"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @Parameter(
                    description = "Данные для создания карты",
                    required = true,
                    schema = @Schema(implementation = CardCreateRequest.class)
            )
            @Valid @RequestBody CardCreateRequest request,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        log.info("Администратор {} создает карту для пользователя: {}",
                adminDetails.getUsername(), request.getUserId());
        CardResponse response = cardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Активировать карту",
            description = "Активация ранее созданной, но неактивной карты"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно активирована",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Карта не найдена"),
            @ApiResponse(responseCode = "409", description = "Карта уже активирована")
    })
    @PatchMapping("/{cardId}/activate")
    public ResponseEntity<CardResponse> activateCard(
            @Parameter(description = "ID карты", required = true, example = "12345")
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        log.info("Администратор {} активирует карту с ID: {}",
                adminDetails.getUsername(), cardId);
        CardResponse activatedCard = cardService.activateCard(cardId);
        return ResponseEntity.ok(activatedCard);
    }

    @Operation(
            summary = "Получить все карты (с пагинацией)",
            description = "Получение списка всех карт в системе с поддержкой пагинации и сортировки"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список карт успешно получен",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "400", description = "Неверные параметры пагинации")
    })
    @GetMapping
    public ResponseEntity<Page<CardResponse>> getAllCards(
            @Parameter(description = "Параметры пагинации")
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "expiryDate", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("Получение всех карт с пагинацией: страница {}, размер {}",
                pageable.getPageNumber(), pageable.getPageSize());
        Page<CardResponse> cards = cardService.getAllCards(pageable);
        return ResponseEntity.ok(cards);
    }

    @Operation(
            summary = "Получить карту по ID",
            description = "Получение детальной информации о карте по её уникальному идентификатору"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта найдена",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Карта не найдена"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getCardById(
            @Parameter(description = "ID карты", required = true, example = "12345")
            @PathVariable Long cardId
    ) {
        log.info("Получение карты по ID: {}", cardId);
        CardResponse card = cardService.getCardById(cardId);
        return ResponseEntity.ok(card);
    }

    @Operation(
            summary = "Обновить карту",
            description = "Обновление информации о карте: статус, лимиты, данные держателя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно обновлена",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @PutMapping("/{cardId}")
    public ResponseEntity<CardResponse> updateCard(
            @Parameter(description = "ID карты", required = true, example = "12345")
            @PathVariable Long cardId,
            @Parameter(
                    description = "Данные для обновления карты",
                    required = true,
                    schema = @Schema(implementation = CardUpdateRequest.class)
            )
            @Valid @RequestBody CardUpdateRequest request,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        log.info("Администратор {} обновляет карту с ID: {}",
                adminDetails.getUsername(), cardId);
        CardResponse updatedCard = cardService.updateCard(cardId, request);
        return ResponseEntity.ok(updatedCard);
    }

    @Operation(
            summary = "Заблокировать карту",
            description = "Блокировка карты. Заблокированная карта не может использоваться для операций"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно заблокирована",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Карта не найдена"),
            @ApiResponse(responseCode = "409", description = "Карта уже заблокирована")
    })
    @PatchMapping("/{cardId}/block")
    public ResponseEntity<CardResponse> blockCard(
            @Parameter(description = "ID карты", required = true, example = "12345")
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        log.info("Администратор {} блокирует карту с ID: {}",
                adminDetails.getUsername(), cardId);
        CardResponse blockedCard = cardService.blockCard(cardId);
        return ResponseEntity.ok(blockedCard);
    }

    @Operation(
            summary = "Удалить карту",
            description = "Удаление карты из системы. Карта может быть удалена только если её баланс равен нулю"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Карта успешно удалена"),
            @ApiResponse(responseCode = "400", description = "Невозможно удалить карту с ненулевым балансом"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @Parameter(description = "ID карты", required = true, example = "12345")
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        log.info("Администратор {} удаляет карту с ID: {}",
                adminDetails.getUsername(), cardId);
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Получить карты пользователя",
            description = "Получение списка всех карт, принадлежащих конкретному пользователю"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список карт пользователя успешно получен",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<CardResponse>> getCardsByUser(
            @Parameter(description = "ID пользователя", required = true, example = "1001")
            @PathVariable Long userId,
            @Parameter(description = "Параметры пагинации")
            @PageableDefault(size = USER_CARDS_PAGE_SIZE) Pageable pageable
    ) {
        log.info("Получение карт пользователя с ID: {}", userId);
        Page<CardResponse> cards = cardService.getCardsByUserId(userId, pageable);
        return ResponseEntity.ok(cards);
    }

    @Operation(summary = "Получить все ожидающие запросы")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список ожидающих запросов получен")
    })
    @GetMapping("/pending")
    public ResponseEntity<List<CardRequestResponse>> getPendingRequests(
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        log.info("Администратор {} получает ожидающие запросы", adminDetails.getUsername());
        List<CardRequestResponse> cardRequestResponses = cardRequestService
                .getAllPendingRequest(adminDetails.getUsername());
        return ResponseEntity.ok(cardRequestResponses);
    }

    @Operation(summary = "Одобрить запрос на блокировку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запрос успешно одобрен")
    })
    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<CardRequestResponse> approveRequest(
            @Parameter(description = "ID запроса", required = true)
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        log.info("Администратор {} одобряет запрос с ID: {}",
                adminDetails.getUsername(), requestId);
        CardRequestResponse response = cardRequestService.approveBlockRequest(
                requestId, adminDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Отклонить запрос на блокировку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запрос успешно отклонен")
    })
    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<CardRequestResponse> rejectRequest(
            @Parameter(description = "ID запроса", required = true)
            @PathVariable @Positive Long requestId,
            @AuthenticationPrincipal UserDetails adminDetails
    ) {
        log.info("Администратор {} отклоняет запрос с ID: {}",
                adminDetails.getUsername(), requestId);
        CardRequestResponse response = cardRequestService.rejectBlockRequest(
                requestId, adminDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}