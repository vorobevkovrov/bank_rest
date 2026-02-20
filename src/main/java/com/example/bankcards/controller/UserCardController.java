package com.example.bankcards.controller;

import com.example.bankcards.dto.request.BlockCardRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CardRequestResponse;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.service.CardRequestService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
/**
 * Контроллер для управления банковскими картами обычными пользователями.
 * <p>
 * Предоставляет REST API для операций с картами, доступных пользователям с ролью USER:
 * <ul>
 *   <li>Просмотр списка своих карт</li>
 *   <li>Получение баланса конкретной карты</li>
 *   <li>Создание запросов на блокировку карт</li>
 * </ul>
 * </p>
 * <p>
 * Все эндпоинты требуют аутентификации и авторизации с ролью USER.
 * </p>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @since 1.0
 *
 * @see CardService
 * @see CardRequestService
 * @see JwtService
 */
@SecurityRequirement(name = "BearerAuthentication")
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/user/cards")
public class UserCardController {
    /**
     * Константа для размера страницы по умолчанию при пагинации.
     */
    private static final int DEFAULT_PAGE_SIZE = 20;
    /**
     * Сервис для выполнения операций с банковскими картами.
     * <p>
     * Предоставляет методы для получения карт пользователя, проверки баланса
     * и других операций, связанных с картами.
     * </p>
     */
    private final CardService cardService;
    /**
     * Сервис для управления запросами на блокировку карт.
     * <p>
     * Обрабатывает создание запросов на блокировку карт пользователями
     * и отслеживание их статуса.
     * </p>
     */
    private final CardRequestService cardRequestService;

    @Operation(
            summary = "Получить все карты пользователя",
            description = """
                    Возвращает список всех карт текущего пользователя с поддержкой пагинации.
                                    
                    **Особенности:**
                    * Возвращаются только карты, принадлежащие текущему пользователю
                    * Поддерживается сортировка по различным полям
                    * По умолчанию сортировка по ID в порядке убывания
                                    
                    **Доступно:**
                    * Номер карты (маскированный)
                    * Тип карты
                    * Статус (активна/заблокирована)
                    * Срок действия
                    * Баланс
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Список карт успешно получен",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Page.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Пользователь не авторизован (отсутствует или невалидный JWT токен)"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Доступ запрещен (недостаточно прав)"
                    )
            }
    )
    /**
     * Получает список всех карт текущего аутентифицированного пользователя.
     * <p>
     * Возвращает страницу с картами пользователя с поддержкой пагинации и сортировки.
     * По умолчанию сортировка выполняется по ID в порядке убывания.
     * </p>
     *
     * @param userDetails объект с данными аутентифицированного пользователя,
     *                   содержит username для поиска карт
     * @param pageable параметры пагинации (номер страницы, размер, сортировка)
     * @return ResponseEntity с объектом Page, содержащим список карт пользователя
     *
     * @throws com.example.bankcards.exception.UserNotFoundException если пользователь не найден
     *
     * @apiNote Эндпоинт доступен только пользователям с ролью USER.
     *          Требуется валидный JWT токен в заголовке Authorization.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<CardResponse>> getMyCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("Пользователь '{}' запрашивает список своих карт. Параметры пагинации: страница {}, размер {}",
                userDetails.getUsername(), pageable.getPageNumber(), pageable.getPageSize());
        Page<CardResponse> cards = cardService.getCardsByUserName(userDetails.getUsername(), pageable);
        log.debug("Найдено {} карт для пользователя '{}'", cards.getTotalElements(), userDetails.getUsername());
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

    /**
     * Получает текущий баланс указанной карты.
     * <p>
     * Проверяет, что карта принадлежит текущему пользователю, прежде чем
     * вернуть информацию о балансе.
     * </p>
     *
     * @param cardId уникальный идентификатор карты, баланс которой запрашивается
     * @param userDetails объект с данными аутентифицированного пользователя,
     *                   используется для проверки владения картой
     * @return ResponseEntity с объектом, содержащим ID карты и текущий баланс
     *
     * @throws com.example.bankcards.exception.CardNotFoundException если карта с указанным ID не найдена
     * @throws org.springframework.security.access.AccessDeniedException если карта принадлежит другому пользователю
     *
     * @apiNote Эндпоинт доступен только пользователям с ролью USER.
     *          Требуется валидный JWT токен в заголовке Authorization.
     */
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
            description = """
                    Создает запрос на блокировку карты, который будет рассмотрен администратором.
                                    
                    **Процесс:**
                    1. Пользователь указывает карту и причину блокировки
                    2. Система создает запрос со статусом PENDING
                    3. Администратор получает уведомление о новом запросе
                    4. После рассмотрения запрос либо одобряется (карта блокируется),
                       либо отклоняется (карта остается активной)
                                    
                    **Важно:**
                    * Нельзя создать запрос на уже заблокированную карту
                    * Причина блокировки обязательна и должна содержать не менее 10 символов
                    * Статус запроса можно отслеживать через соответствующий эндпоинт
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Запрос на блокировку успешно создан",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CardRequestResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Неверные параметры запроса (не указана причина, карта уже заблокирована)"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Доступ запрещен (попытка заблокировать чужую карту)"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Карта не найдена"
                    )
            }
    )
    /**
     * Создает запрос на блокировку карты.
     * <p>
     * Пользователь может запросить блокировку своей карты. Запрос отправляется
     * администраторам на рассмотрение. После одобрения администратором карта
     * будет заблокирована.
     * </p>
     * <p>
     * Процесс блокировки:
     * <ol>
     *   <li>Пользователь создает запрос с указанием причины блокировки</li>
     *   <li>Запрос сохраняется со статусом PENDING</li>
     *   <li>Администратор рассматривает запрос и принимает решение</li>
     *   <li>При одобрении карта блокируется, при отказе - запрос отклоняется</li>
     * </ol>
     * </p>
     *
     * @param request объект запроса, содержащий ID карты и причину блокировки
     * @param userDetails объект с данными аутентифицированного пользователя,
     *                   содержит username для идентификации создателя запроса
     * @return ResponseEntity с объектом, содержащим детали созданного запроса:
     *         ID запроса, ID карты, статус, дата создания
     *
     * @throws com.example.bankcards.exception.CardNotFoundException если карта не найдена
     * @throws org.springframework.security.access.AccessDeniedException если карта принадлежит другому пользователю
     * @throws com.example.bankcards.exception.InvalidCardStateException если карта уже заблокирована
     * @throws jakarta.validation.ValidationException если данные запроса не проходят валидацию
     *
     * @apiNote Эндпоинт доступен только пользователям с ролью USER.
     *          Требуется валидный JWT токен в заголовке Authorization.
     */
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
        log.info("Запрос на блокировку создан: ID={}, карта={}",
                response.getRequestId(), response.getCardId());
        return ResponseEntity.ok(response);
    }
}