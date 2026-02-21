package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления финансовыми транзакциями между картами.
 * <p>
 * Предоставляет REST API для выполнения переводов средств между картами
 * с проверкой прав доступа и валидацией входных данных. Все операции
 * требуют аутентификации пользователя.
 * </p>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @see TransactionService
 * @see TransferRequest
 * @see TransferResponse
 * @since 1.0
 */
@Tag(
        name = "Управление транзакциями",
        description = "API для работы с финансовыми транзакциями между картами"
)
@SecurityRequirement(name = "BearerAuthentication")
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    /**
     * Сервис для обработки логики транзакций.
     * <p>
     * Содержит бизнес-логику выполнения переводов, проверки баланса,
     * валидации прав доступа и сохранения истории операций.
     * </p>
     */
    private final TransactionService transactionService;

    @Operation(
            summary = "Перевод между своими картами",
            description = """
                    Выполняет перевод средств между картами, принадлежащими одному пользователю.
                                    
                    **Требования:**
                    * Обе карты должны принадлежать текущему пользователю
                    * Карта-источник должна быть активна и иметь достаточный баланс
                    * Карта-назначение должна быть активна
                    * Сумма перевода должна быть положительной
                                    
                    **Ограничения:**
                    * Нельзя переводить на ту же карту
                    * Нельзя переводить с заблокированной карты
                    * Минимальная сумма перевода: 0.01
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Перевод успешно выполнен",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TransferResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Неверные параметры запроса (недостаточно средств, неверная сумма и т.д.)",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Нет доступа к картам (карты принадлежат другому пользователю)",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Одна из карт не найдена",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Карта заблокирована или неактивна",
                            content = @Content
                    )
            }
    )
    /**
     * Выполняет перевод средств между картами, принадлежащими одному пользователю.
     * <p>
     * Метод проверяет:
     * <ul>
     *   <li>Принадлежность обеих карт аутентифицированному пользователю</li>
     *   <li>Достаточность средств на карте-источнике</li>
     *   <li>Активность обеих карт</li>
     *   <li>Валидность суммы перевода (положительное число)</li>
     * </ul>
     * </p>
     *
     * @param request объект запроса, содержащий ID карт-источника и назначения,
     *               а также сумму перевода. Не может быть null.
     * @param userDetails объект с данными аутентифицированного пользователя,
     *                   используется для проверки прав доступа к картам
     * @return ResponseEntity с объектом ответа, содержащим детали выполненной транзакции:
     *         ID транзакции, статус, дату и время, остатки на картах
     *
     * @throws com.example.bankcards.exception.CardNotFoundException если одна из карт не найдена
     * @throws com.example.bankcards.exception.InsufficientFundsException если недостаточно средств
     * @throws com.example.bankcards.exception.InvalidCardStateException если карта заблокирована или неактивна
     * @throws org.springframework.security.access.AccessDeniedException если пользователь не владеет картами
     * @throws jakarta.validation.ValidationException если данные запроса не проходят валидацию
     *
     * @apiNote Эндпоинт доступен только пользователям с ролью USER.
     *          Требуется валидный JWT токен в заголовке Authorization.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/transfer/own")
    public ResponseEntity<TransferResponse> transferBetweenOwnCards(
            @Parameter(description = "Данные для перевода", required = true)
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("User {} initiating transfer of {} from card {} to card {}",
                userDetails.getUsername(), request.getAmount(),
                request.getFromCardId(), request.getToCardId());
        TransferResponse response = transactionService.transferBetweenOwnCards(request, userDetails);
        log.debug("Transfer completed successfully. Transaction ID: {}", response.getTransactionId());
        return ResponseEntity.ok(response);
    }
}